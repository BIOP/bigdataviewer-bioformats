package ch.epfl.biop.bdv.bioformats.bioformatssource;

public class BioFormatsBdvOpenerFix {

    static public void fixNikonND2(BioFormatsBdvOpener opener) {
        opener.positionIsImageCenter = true;
        opener.flipPositionX();
        opener.flipPositionY();
    }

}
