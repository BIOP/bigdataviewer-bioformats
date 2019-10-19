package ch.epfl.biop.bdv.bioformats.imageloader;

public class FileSerieChannel {
    public final int iFile;
    public final int iSerie;
    public final int iChannel;
    public FileSerieChannel(int iF, int iS, int iC) {
        iFile=iF;
        iSerie=iS;
        iChannel=iC;
    }
}