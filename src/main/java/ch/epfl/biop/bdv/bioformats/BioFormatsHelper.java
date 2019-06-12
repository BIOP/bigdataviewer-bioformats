package ch.epfl.biop.bdv.bioformats;

import loci.formats.IFormatReader;
import loci.formats.meta.IMetadata;
import ome.xml.model.enums.PixelType;

import java.util.function.Consumer;
import java.util.logging.Logger;

/**
 * Inspired from https://github.com/qupath/qupath-bioformats-extension/blob/master/src/main/java/qupath/lib/images/servers/BioFormatsImageServer.java
 */

public class BioFormatsHelper {


    private static final Logger LOGGER = Logger.getLogger( BioFormatsHelper.class.getName() );

    /**
     * Manager to help keep multithreading under control.
     */

    public int nDimensions;

    final boolean is8bits;

    final boolean is16bits;

    final boolean is24bitsRGB;

    public BioFormatsHelper(IFormatReader reader, int image_index) throws Exception {

        final IMetadata omeMeta = (IMetadata) reader.getMetadataStore();
        reader.setSeries(image_index);
        is24bitsRGB = (reader.isRGB());
        is8bits = (omeMeta.getPixelsType(image_index) == PixelType.UINT8)&&(!is24bitsRGB);
        is16bits = (omeMeta.getPixelsType(image_index) == PixelType.UINT16)&&(!is24bitsRGB);

        Consumer<String> log = (s) -> {};// LOGGER.info(s);

        log.accept("getchannelsamples="+omeMeta.getChannelSamplesPerPixel(image_index, 0));
        log.accept("getchannelcount="+omeMeta.getChannelCount(image_index));
        log.accept("reader.getImageCount()="+reader.getImageCount());
        log.accept("reader.isRGB()="+reader.isRGB());
        log.accept("reader.getOptimalTileHeight()="+reader.getOptimalTileHeight());
        log.accept("reader.getOptimalTileWidth()="+reader.getOptimalTileWidth());
        log.accept("reader.isInterleaved()="+reader.isInterleaved());
    }

    public String toString() {
        String str = "Img \n";
        str+="nDimensions = "+nDimensions;

        return str;
    }
}
