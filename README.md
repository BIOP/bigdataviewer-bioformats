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

## Usage:
* Enable https://biop.epfl.ch/Fiji-Update-Bdv/

# List of commands

## [ConvertFilesToBdvXmlDatasetCommand](https://github.com/BIOP/bigdataviewer_scijava/tree/master/src/main/java/ch/epfl/biop/bdv/bioformats/command/ConvertFilesToBdvXmlDatasetCommand.java) [BDV_SciJava>SpimDataset>Convert Files to SpimDataset [BioFormats Bdv Bridge]]
Convert files to xml Spimdataset, using bioformats reader
Supports multiresolution bioformats api.
### Input
* [File[]] **files**:Files to include in the dataset
### Output
* [AbstractSpimData] **spimData**:


## [OpenFilesWithBigdataviewerBioformatsBridgeCommand](https://github.com/BIOP/bigdataviewer_scijava/tree/master/src/main/java/ch/epfl/biop/bdv/bioformats/command/OpenFilesWithBigdataviewerBioformatsBridgeCommand.java) [BDV_SciJava>SpimDataset>Open>SpimDataset [BioFormats Bdv Bridge]]
Opens and show in a bdv window files by using bioformats reader
Support bioformmats multiresolution api. Attempts to set colors basedon bioformats metadata. Do not attempt auto contrast.
### Input
* [File[]] **files**:
* [double] **maxDisplay**:
* [double] **minDisplay**:
* [boolean] **setColor**:
* [boolean] **setGrouping**:
### Output
* [BdvHandle] **bdv_h**:
* [AbstractSpimData] **spimData**:


## [ExportBdvSourcesToOmeTiff](https://github.com/BIOP/bigdataviewer_scijava/tree/master/src/main/java/ch/epfl/biop/bdv/bioformats/export/ometiff/ExportBdvSourcesToOmeTiff.java) [BDV_SciJava>Bdv>Export Sources>As OMETIFF (very limited) (SciJava)]
### Input
* [boolean] **computePyramid**:compute pyramid
* [File] **outputFile**:output file, ome tiff format
* [int] **resolutions**:number of resolutions
* [int] **scale**:pyramid scale factor (XY only)
* [List] **srcs**:Bdv Sources to save
* [int] **tileSizeX**:
* [int] **tileSizeY**:
* [int] **timePoint**:time point


## [OpenSample](https://github.com/BIOP/bigdataviewer_scijava/tree/master/src/main/java/ch/epfl/biop/bdv/bioformats/samples/OpenSample.java) [BDV_SciJava>SpimDataset>Open>Open and show sample dataset]
Open sample datasets
Downloads and cache datasets on first open attempt.
### Input
* [String] **datasetName**:Choose a sample dataset
### Output
* [BdvHandle] **bdvh_out**:
* [AbstractSpimData] **spimData**:


