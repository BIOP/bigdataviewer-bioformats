[![](https://travis-ci.com/BIOP/bigdataviewer-bioformats.svg?branch=master)](https://travis-ci.com/BIOP/bigdataviewer-bioformats)

# Bigdataviewer Bioformats Bridge 

This repository enables the opening of files into BigDataViewer by using the BioFormats API.
Emphasis has been put on correct support of metadata for locating multiple series of acquisition, like in slide scanner file.

Multiresolition file formats supported by BioFormats are translated into a BigDataViewer multiresolution source, enabling support for 'big' files.

File format tested:
* JPEG
* PNG
* VSI
* ND2

VSI example video : https://www.youtube.com/watch?v=tuiaEXmFVyE

In theory all files supported by the Bioformats API can be opened using this package.

## Limitations:
* Signed pixels values are not supported (yet)

## Usage in fiji:
* Bdv-bioformats is shipped with the [Bigdataviewer-Playground](https://imagej.github.io/plugins/bdv/playground) update site.

# List of commands

## [BasicOpenFilesWithBigdataviewerBioformatsBridgeCommand](https://github.com/BIOP/bigdataviewer-bioformats/tree/master/src/main/java/ch/epfl/biop/bdv/bioformats/command/BasicOpenFilesWithBigdataviewerBioformatsBridgeCommand.java) [Plugins>BigDataViewer>Playground>BDVDataset>Open [BioFormats Bdv Bridge (Basic)]]
Support bioformats multiresolution api. Attempts to set colors based on bioformats metadata. Do not attempt auto contrast.
### Input
* [File[]] **files**:
* [boolean] **splitRGBChannels**:Split RGB channels
* [String] **unit**:Physical units of the dataset
### Output
* [AbstractSpimData] **spimData**:


## [OpenFilesWithBigdataviewerBioformatsBridgeCommand](https://github.com/BIOP/bigdataviewer-bioformats/tree/master/src/main/java/ch/epfl/biop/bdv/bioformats/command/OpenFilesWithBigdataviewerBioformatsBridgeCommand.java) [Plugins>BigDataViewer>Playground>BDVDataset>Open [BioFormats Bdv Bridge]]
Support bioformats multiresolution api. Attempts to set colors based on bioformats metadata. Do not attempt auto contrast.
### Input
* [File[]] **files**:
### Output
* [AbstractSpimData] **spimData**:


## [StandaloneOpenFileWithBigdataviewerBioformatsBridgeCommand](https://github.com/BIOP/bigdataviewer-bioformats/tree/master/src/main/java/ch/epfl/biop/bdv/bioformats/command/StandaloneOpenFileWithBigdataviewerBioformatsBridgeCommand.java) [Plugins>BigDataViewer>Bio-Formats>Open File with Bio-Formats]
Support bioformats multiresolution api. Attempts to set colors based on bioformats metadata. Do not attempt auto contrast.
### Input
* [File] **file**:
* [boolean] **splitRGBChannels**:Split RGB channels if you have 16 bits RGB images
* [String] **unit**:Physical units of the dataset


## [OpenSample](https://github.com/BIOP/bigdataviewer-bioformats/tree/master/src/main/java/ch/epfl/biop/bdv/bioformats/samples/OpenSample.java) [Plugins>BigDataViewer>Playground>BDVDataset>Open sample dataset]
Open sample datasets
Downloads and cache datasets on first open attempt.
### Input
* [String] **datasetName**:Choose a sample dataset
### Output
* [AbstractSpimData] **spimData**:


