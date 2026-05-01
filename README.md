For Windows, please use the .exe release. https://github.com/bndu/HitachiTEMTIFF_to_OME-TIFF/releases/tag/exe

For other platforms, there is a .jar release. https://github.com/bndu/HitachiTEMTIFF_to_OME-TIFF/releases/tag/jar


HitachiTEMTIFF_to_OME-TIFF – Build Instructions

Overview
This document explains how to build the Hitachi TEM TIFF to OME-TIFF converter from source using Maven.

Requirements

1. Java Development Kit (JDK)
- Version: 17 or newer
- Verify installation:
  java -version

Download:
https://adoptium.net/

2. Apache Maven
- Verify installation:
  mvn -version

Download:
https://maven.apache.org/

Project Structure

The project follows a standard Maven layout:

src/
  main/
    java/
      TiffToOmeTiffConverter.java

pom.xml

Build Instructions

1. Open a terminal or command prompt

2. Navigate to the project root directory (where pom.xml is located):

   cd path_to_project

3. Run the Maven build:

   mvn clean package

4. After a successful build, the compiled JAR will be located at:

   target/hitachi_to_OME-TIFF_converter-1.0-shaded.jar

Running the Built Application

Run the application using:

   java -jar target/hitachi_to_OME-TIFF_converter-1.0-shaded.jar

Notes

- The build uses the Maven Shade Plugin to create a standalone (fat) JAR including all dependencies.
- The resulting JAR can be distributed directly without requiring external libraries.
- Warnings about overlapping resources during the build are normal and can be ignored.

Troubleshooting

If Maven fails:

- Ensure Java 17+ is being used
- Ensure internet access is available for dependency download
- Try forcing dependency updates:

  mvn clean package -U

If Java is not recognised:

- Check your PATH environment variable
- Reinstall Java if necessary

 Disclaimer
This software is an independent project and is not affiliated with, endorsed by, or supported by Hitachi High-Technologies or any of its affiliates.
The use of the name "Hitachi" refers solely to the format of the input data and does not imply any association with the company.

License

This project depends on Bio-Formats (GPL v2) https://github.com/ome/bioformats. Any distribution must comply with GPL v2 or a compatible license.

For details:
https://www.gnu.org/licenses/old-licenses/gpl-2.0.html

