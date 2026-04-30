# HitachiTEMTIFF_to_OME-TIFF
Java file converter for Hitachi TEM TIFF to OME-TIFF

Overview
This tool converts Hitachi TEM image files (.tif) and their associated metadata files (.txt) into OME-TIFF format. The output is compatible with Fiji, ImageJ, Bio-Formats, and other OME-aware software.

Requirements
- Java 17 or newer

How to Run
1. Place the .jar file in the same folder as your .tif and .txt files
2. Double-click the .jar file
   or run: java -jar hitachi_to_OME-TIFF_converter.jar

Metadata Mapping
- PixelSize → Pixels.PhysicalSizeX/Y
- StagePositionX/Y/Z → Plane.PositionX/Y/Z
- Magnification → Objective.NominalMagnification
- Date + Time → Image.AcquisitionDate

Other metadata is stored as structured annotations.

Licensing
This software uses Bio-Formats (GPL v2), so distribution must comply with GPL v2.
