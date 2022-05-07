[![](https://github.com/BIOP/bigdataviewer-bioformats/actions/workflows/build-main.yml/badge.svg)](https://github.com/BIOP/bigdataviewer-bioformats/actions/workflows/build-main.yml)

# BigDataViewer Bio-Formats Bridge 

This repository enables the opening of files into [BigDataViewer](https://github.com/bigdataviewer) by using the [Bio-Formats API](https://github.com/ome/bioformats).
Emphasis has been put on correct support of metadata for locating multiple series of acquisition, like in slide scanner file.

Multiresolition file formats supported by BioFormats are translated into a BigDataViewer multiresolution source, enabling support for 'big' files.

Note: signed pixels values are not supported.

## Usage in ImageJ/Fiji
BigDataViewer-BioFormats is shipped with the [Bigdataviewer-Playground](https://imagej.github.io/plugins/bdv/playground) update site.

# Fiji Plugins List

## [Open [BioFormats Bdv Bridge]](https://github.com/BIOP/bigdataviewer-bioformats/tree/master/src/main/java/ch/epfl/biop/bdv/bioformats/command/OpenFilesWithBigdataviewerBioformatsBridgeCommand.java)
Support bioformats multiresolution api. Attempts to set colors based on bioformats metadata. Do not attempt auto contrast.
### Input
* [String] **datasetname**:Name of this dataset
* [File[]] **files**:Dataset files
### Output
* [AbstractSpimData] **spimdata**

## [Open [BioFormats Bdv Bridge (Basic)]](https://github.com/BIOP/bigdataviewer-bioformats/tree/master/src/main/java/ch/epfl/biop/bdv/bioformats/command/BasicOpenFilesWithBigdataviewerBioformatsBridgeCommand.java)
Support bioformats multiresolution API. Attempts to set colors based on bioformats metadata. Do not attempt auto contrast.
### Input
* [String] **datasetname**:Name of this dataset
* [File[]] **files**:Dataset files
* [boolean] **splitrgbchannels**:Split RGB channels
* [String] **unit**:Physical units of the dataset
### Output
* [AbstractSpimData] **spimdata**

## [Export Sources To OME Tiff (build pyramid)](https://github.com/BIOP/bigdataviewer-bioformats/tree/master/src/main/java/ch/epfl/biop/bdv/bioformats/command/ExportToOMETiffBuildPyramidCommand.java)
Saves Bdv sources as a multi-channel OME-Tiff file, with multi-resolution levels recomputed from the highest resolution level
### Input
* [int] **downscaling**:Scaling factor between resolution levels
* [File] **file**:Output file
* [Boolean] **lzw_compression**:Use LZW compression
* [int] **max_tiles_queue**:Number of tiles computed in advance
* [int] **n_resolution_levels**:Number of resolution levels
* [int] **n_threads**:Number of threads (0 = serial)
* [SourceAndConverter[]] **sacs**:Sources to export
* [int] **tile_size_x**:Tile Size X (negative: no tiling)
* [int] **tile_size_y**:Tile Size Y (negative: no tiling)
* [String] **unit**:Physical unit

## [Export Sources To OME Tiff](https://github.com/BIOP/bigdataviewer-bioformats/tree/master/src/main/java/ch/epfl/biop/bdv/bioformats/command/ExportToOMETiffCommand.java)
Saves Bdv sources as a multi-channel OME-Tiff file, keeping original multiresolution levels if the sources are initially multiresolution.
### Input
* [File] **file**:Output file
* [Boolean] **lzw_compression**:Use LZW compression
* [int] **max_tiles_queue**:Number of tiles computed in advance
* [int] **n_threads**:Number of threads (0 = serial)
* [SourceAndConverter[]] **sacs**:Sources to export
* [int] **tile_size_x**:Tile Size X (negative: no tiling)
* [int] **tile_size_y**:Tile Size Y (negative: no tiling)
* [String] **unit**:Physical unit

## [Open sample dataset](https://github.com/BIOP/bigdataviewer-bioformats/tree/master/src/main/java/ch/epfl/biop/bdv/bioformats/command/OpenSampleCommand.java)
Open sample datasets
Downloads and cache datasets on first open attempt.
### Input
* [String] **datasetName**:Choose a sample dataset
### Output
* [AbstractSpimData] **spimData**

## [Open File with Bio-Formats](https://github.com/BIOP/bigdataviewer-bioformats/tree/master/src/main/java/ch/epfl/biop/bdv/bioformats/command/StandaloneOpenFileWithBigdataviewerBioformatsBridgeCommand.java)
Support bioformats multiresolution api. Attempts to set colors based on bioformats metadata. Do not attempt auto contrast.
### Input
* [File] **file**:File to open
* [boolean] **splitrgbchannels**:Split RGB channels if you have 16 bits RGB images
* [String] **unit**:Physical units of the dataset