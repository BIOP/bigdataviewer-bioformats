package ch.epfl.biop.bdv.bioformats.export;


import mpicbg.spim.data.XmlHelpers;
import mpicbg.spim.data.generic.sequence.AbstractSequenceDescription;
import mpicbg.spim.data.generic.sequence.ImgLoaderIo;
import mpicbg.spim.data.generic.sequence.XmlIoBasicImgLoader;
import org.jdom2.Element;

import java.io.File;

import static mpicbg.spim.data.XmlKeys.IMGLOADER_FORMAT_ATTRIBUTE_NAME;
// https://github.com/fiji/SPIM_Registration/tree/ea9302d92bf975107b48509dcd5ac62992f6ffdc/src/main/java/spim/fiji/spimdata/imgloaders
// https://www.codota.com/web/assistant/code/rs/5c745ecd49efcb0001f8ed68#L44


@ImgLoaderIo( format = "spimreconstruction.biop_bioformatsimageloader", type = BioFormatsImageLoader.class )
public class XmlIoBioFormatsImgLoader implements XmlIoBasicImgLoader< BioFormatsImageLoader > {

    public static final String DIRECTORY_TAG = "imagedirectory";
    public static final String MASTER_FILE_TAG = "masterfile";

    @Override
    public Element toXml(BioFormatsImageLoader imgLoader, File basePath) {
        final Element elem = new Element( "ImageLoader" );
        elem.setAttribute( IMGLOADER_FORMAT_ATTRIBUTE_NAME, this.getClass().getAnnotation( ImgLoaderIo.class ).format() );
        elem.addContent( XmlHelpers.pathElement( DIRECTORY_TAG, imgLoader.file.getParentFile(), basePath ) );
        elem.addContent( XmlHelpers.textElement( MASTER_FILE_TAG, imgLoader.file.getName() ) );
        return elem;
    }

    @Override
    public BioFormatsImageLoader fromXml(Element elem, File basePath, AbstractSequenceDescription<?, ?, ?> sequenceDescription) {
        try
        {
            final File path = XmlHelpers.loadPath( elem, DIRECTORY_TAG, basePath );
            final String masterFile = XmlHelpers.getText( elem, MASTER_FILE_TAG );

            File f = new File( path, masterFile );

            return new BioFormatsImageLoader( f, sequenceDescription);
        }
        catch ( final Exception e )
        {
            throw new RuntimeException( e );
        }
    }
}
