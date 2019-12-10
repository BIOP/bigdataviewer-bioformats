package ch.epfl.biop.bdv.bioformats.bioformatssource;

public class BioFormatsBdvOpenerFix {

    static public BioFormatsBdvOpener fixNikonND2(BioFormatsBdvOpener opener) {
        return opener.centerPositionConvention().flipPositionX().flipPositionY();
    }

}
