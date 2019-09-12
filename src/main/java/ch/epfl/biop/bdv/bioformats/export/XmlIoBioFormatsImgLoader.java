package ch.epfl.biop.bdv.bioformats.export;

import mpicbg.spim.data.XmlHelpers;
import mpicbg.spim.data.generic.sequence.AbstractSequenceDescription;
import mpicbg.spim.data.generic.sequence.ImgLoaderIo;
import mpicbg.spim.data.generic.sequence.XmlIoBasicImgLoader;
import org.jdom2.Element;

import java.io.File;
import java.util.ArrayList;

import static mpicbg.spim.data.XmlKeys.IMGLOADER_FORMAT_ATTRIBUTE_NAME;

@ImgLoaderIo( format = "spimreconstruction.biop_bioformatsimageloader", type = BioFormatsImageLoader.class )
public class XmlIoBioFormatsImgLoader implements XmlIoBasicImgLoader< BioFormatsImageLoader > {

    public static final String DIRECTORY_TAG = "imagedirectory";
    public static final String FILE_NUMBER_TAG = "files_number";
    public static final String FILE_TAG = "filename";

    @Override
    public Element toXml(BioFormatsImageLoader imgLoader, File basePath) {
        final Element elem = new Element( "ImageLoader" );
        elem.setAttribute( IMGLOADER_FORMAT_ATTRIBUTE_NAME, this.getClass().getAnnotation( ImgLoaderIo.class ).format() );
        elem.addContent( XmlHelpers.pathElement( DIRECTORY_TAG, imgLoader.files.get(0).getParentFile(), basePath ) );
        elem.addContent(XmlHelpers.intElement( FILE_NUMBER_TAG, imgLoader.files.size()));
        for (int i=0;i<imgLoader.files.size();i++) {
            elem.addContent(XmlHelpers.textElement(FILE_TAG+"_"+i, imgLoader.files.get(i).getName()));
        }
        return elem;
    }

    @Override
    public BioFormatsImageLoader fromXml(Element elem, File basePath, AbstractSequenceDescription<?, ?, ?> sequenceDescription) {
        try
        {
            final File path = XmlHelpers.loadPath( elem, DIRECTORY_TAG, basePath );
            final int number_of_files = XmlHelpers.getInt( elem, FILE_NUMBER_TAG );
            ArrayList<File> files = new ArrayList<>();
            for (int i=0;i<number_of_files;i++) {

                final String masterFile = XmlHelpers.getText( elem, FILE_TAG+"_"+i );
                File f = new File( path, masterFile );
                files.add(f);
            }

            return new BioFormatsImageLoader( files, sequenceDescription);
        }
        catch ( final Exception e )
        {
            throw new RuntimeException( e );
        }
    }
}
