import loci.common.services.ServiceFactory;
import loci.formats.ImageReader;
import loci.formats.out.OMETiffWriter;
import loci.formats.services.OMEXMLService;
import loci.formats.meta.IMetadata;
import loci.formats.MetadataTools;

import ome.units.quantity.Length;
import ome.units.UNITS;
import ome.xml.model.primitives.Timestamp;
import ome.xml.model.MapPair;

import javax.swing.*;
import java.io.File;
import java.nio.file.*;
import java.util.*;

public class TiffToOmeTiffConverter {

    /**
     * Entry point: launches the GUI workflow on the Swing thread.
     */
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            try {
                runApp();
            } catch (Exception e) {
                JOptionPane.showMessageDialog(null, e.getMessage());
            }
        });
    }

    /**
     * Main application logic:
     * - Locate TIFF files in the same directory as the executable
     * - Identify valid TIFF + TXT metadata pairs
     * - Prompt the user for confirmation
     * - Convert selected files to OME-TIFF
     */
    private static void runApp() throws Exception {

        Path dir = new File(
                TiffToOmeTiffConverter.class
                        .getProtectionDomain()
                        .getCodeSource()
                        .getLocation()
                        .toURI()
        ).getParentFile().toPath();

        List<File> valid = new ArrayList<>();

        try (DirectoryStream<Path> stream = Files.newDirectoryStream(dir, "*.tif")) {
            for (Path p : stream) {

                String name = p.getFileName().toString();

                if (name.toLowerCase().endsWith(".ome.tif")) continue;

                File txt = new File(p.toString().replace(".tif", ".txt"));
                File ome = new File(p.toString().replace(".tif", ".ome.tif"));

                if (txt.exists() && !ome.exists()) {
                    valid.add(p.toFile());
                }
            }
        }

        if (valid.isEmpty()) {
            JOptionPane.showMessageDialog(null, "No valid TIFF + TXT pairs found.");
            return;
        }

        StringBuilder sb = new StringBuilder();
        for (File f : valid) sb.append(f.getName()).append("\n");

        int choice = JOptionPane.showConfirmDialog(
                null,
                "Convert the following files?\n\n" + sb,
                "OME-TIFF Converter",
                JOptionPane.YES_NO_OPTION
        );

        if (choice != JOptionPane.YES_OPTION) return;

        for (File tif : valid) {
            Map<String,String> meta = parse(new File(tif.getAbsolutePath().replace(".tif", ".txt")));
            convert(tif, meta);
        }

        JOptionPane.showMessageDialog(null, "Done.");
    }

    /**
     * Parses the metadata sidecar file into key-value pairs.
     */
    private static Map<String,String> parse(File f) throws Exception {
        Map<String,String> map = new HashMap<>();

        for (String line : Files.readAllLines(f.toPath())) {
            if (!line.contains("=")) continue;
            String[] parts = line.split("=", 2);
            map.put(parts[0].trim(), parts[1].trim());
        }
        return map;
    }

    /**
     * Performs the TIFF → OME-TIFF conversion:
     * - Reads pixel data using Bio-Formats
     * - Constructs OME metadata
     * - Writes a new OME-TIFF file
     */
    private static void convert(File tif, Map<String,String> meta) throws Exception {

        ImageReader reader = new ImageReader();
        reader.setId(tif.getAbsolutePath());

        int width = reader.getSizeX();
        int height = reader.getSizeY();
        int pixelType = reader.getPixelType();

        byte[] plane = reader.openBytes(0);

        ServiceFactory factory = new ServiceFactory();
        OMEXMLService service = factory.getInstance(OMEXMLService.class);
        IMetadata ome = service.createOMEXMLMetadata();

        /**
         * Populate core metadata structure:
         * - Dimension order
         * - Pixel type
         * - Image dimensions
         */
        MetadataTools.populateMetadata(
                ome,
                0,
                null,
                false,
                "XYCZT",
                loci.formats.FormatTools.getPixelTypeString(pixelType),
                width,
                height,
                1,
                1,
                1,
                1
        );

        /**
         * Explicitly define byte order to match original TIFF data.
         */
        ome.setPixelsBigEndian(false, 0);

        /**
         * Convert pixel size from nanometers to micrometers,
         * as expected by OME and imaging tools like Fiji.
         */
        double px_nm = Double.parseDouble(meta.getOrDefault("PixelSize", "1.0"));
        double px_um = px_nm / 1000.0;

        ome.setPixelsPhysicalSizeX(new Length(px_um, UNITS.MICROMETER), 0);
        ome.setPixelsPhysicalSizeY(new Length(px_um, UNITS.MICROMETER), 0);

        /**
         * Convert acquisition date to ISO 8601 format (YYYY-MM-DDTHH:MM:SS).
         */
        if (meta.containsKey("Date") && meta.containsKey("Time")) {
            String[] d = meta.get("Date").split("/");
            String iso = d[2] + "-" + d[1] + "-" + d[0] + "T" + meta.get("Time");
            ome.setImageAcquisitionDate(new Timestamp(iso), 0);
        }

        /**
         * Map stage coordinates into the OME Plane model.
         */
        if (meta.containsKey("StagePositionX"))
            ome.setPlanePositionX(new Length(Double.parseDouble(meta.get("StagePositionX")), UNITS.MICROMETER), 0, 0);

        if (meta.containsKey("StagePositionY"))
            ome.setPlanePositionY(new Length(Double.parseDouble(meta.get("StagePositionY")), UNITS.MICROMETER), 0, 0);

        if (meta.containsKey("StagePositionZ"))
            ome.setPlanePositionZ(new Length(Double.parseDouble(meta.get("StagePositionZ")), UNITS.MICROMETER), 0, 0);

        /**
         * Define instrument and optical configuration.
         * Magnification is stored as Objective metadata.
         */
        ome.setInstrumentID("Instrument:0", 0);
        ome.setObjectiveID("Objective:0", 0, 0);

        if (meta.containsKey("Magnification")) {
            double mag = Double.parseDouble(meta.get("Magnification"));
            ome.setObjectiveNominalMagnification(mag, 0, 0);
        }

        /**
         * Store non-standard metadata fields as structured annotations.
         * Fields already mapped to standard OME properties are excluded.
         */
        Set<String> skip = new HashSet<>(Arrays.asList(
                "PixelSize", "Date", "Time",
                "StagePositionX", "StagePositionY", "StagePositionZ",
                "Magnification"
        ));

        int annotationIndex = 0;

        for (Map.Entry<String,String> entry : meta.entrySet()) {

            if (skip.contains(entry.getKey())) continue;

            List<MapPair> pairs = new ArrayList<>();
            pairs.add(new MapPair(entry.getKey(), entry.getValue()));

            ome.setMapAnnotationID("Annotation:" + annotationIndex, annotationIndex);
            ome.setMapAnnotationNamespace("TEMMetadata", annotationIndex);
            ome.setMapAnnotationValue(pairs, annotationIndex);
            ome.setImageAnnotationRef("Annotation:" + annotationIndex, 0, annotationIndex);

            annotationIndex++;
        }

        /**
         * Write the OME-TIFF file sequentially to avoid rewrite issues.
         */
        OMETiffWriter writer = new OMETiffWriter();
        writer.setWriteSequentially(true);
        writer.setMetadataRetrieve(ome);
        writer.setId(tif.getAbsolutePath().replace(".tif", ".ome.tif"));

        writer.saveBytes(0, plane);

        writer.close();
        reader.close();
    }
}
