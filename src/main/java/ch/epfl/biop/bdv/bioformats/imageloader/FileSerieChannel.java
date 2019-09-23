package ch.epfl.biop.bdv.bioformats.imageloader;

public class FileSerieChannel {
    public int iFile;
    public int iSerie;
    public int iChannel;
    public FileSerieChannel(int iF, int iS, int iC) {
        iFile=iF;
        iSerie=iS;
        iChannel=iC;
    }
}