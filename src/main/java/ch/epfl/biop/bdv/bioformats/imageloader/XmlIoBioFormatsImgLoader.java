package ch.epfl.biop.bdv.bioformats.imageloader;

import ch.epfl.biop.bdv.bioformats.bioformatssource.BioFormatsBdvOpener;
import com.google.gson.Gson;
import mpicbg.spim.data.XmlHelpers;
import mpicbg.spim.data.generic.sequence.AbstractSequenceDescription;
import mpicbg.spim.data.generic.sequence.ImgLoaderIo;
import mpicbg.spim.data.generic.sequence.XmlIoBasicImgLoader;
import org.jdom2.Element;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import static mpicbg.spim.data.XmlKeys.IMGLOADER_FORMAT_ATTRIBUTE_NAME;

@ImgLoaderIo( format = "spimreconstruction.biop_bioformatsimageloader", type = BioFormatsImageLoader.class )
public class XmlIoBioFormatsImgLoader implements XmlIoBasicImgLoader< BioFormatsImageLoader > {

    public static final String OPENER_CLASS_TAG = "opener_class";
    public static final String OPENER_TAG = "opener";
    public static final String DATASET_NUMBER_TAG = "dataset_number";

    @Override
    public Element toXml(BioFormatsImageLoader imgLoader, File basePath) {
        final Element elem = new Element( "ImageLoader" );
        elem.setAttribute( IMGLOADER_FORMAT_ATTRIBUTE_NAME, this.getClass().getAnnotation( ImgLoaderIo.class ).format() );
        // For potential extensibility
        elem.addContent(XmlHelpers.textElement( OPENER_CLASS_TAG, BioFormatsBdvOpener.class.getName()));
        elem.addContent(XmlHelpers.intElement( DATASET_NUMBER_TAG, imgLoader.openers.size()));

        Gson gson = new Gson();
        for (int i=0;i<imgLoader.openers.size();i++) {
            // Opener serialization
            elem.addContent(XmlHelpers.textElement(OPENER_TAG+"_"+i, gson.toJson(imgLoader.openers.get(i))));
        }
        return elem;
    }

    @Override
    public BioFormatsImageLoader fromXml(Element elem, File basePath, AbstractSequenceDescription<?, ?, ?> sequenceDescription) {
        try
        {
            final int number_of_datasets = XmlHelpers.getInt( elem, DATASET_NUMBER_TAG );
            List<BioFormatsBdvOpener> openers = new ArrayList<>();

            String openerClassName = XmlHelpers.getText( elem, OPENER_CLASS_TAG );

            if (!openerClassName.equals(BioFormatsBdvOpener.class.getName())) {
                throw new UnsupportedOperationException("Error class "+openerClassName+" not recognized.");
            }

            Gson gson = new Gson();
            for (int i=0;i<number_of_datasets;i++) {
                // Opener de-serialization
                String jsonInString = XmlHelpers.getText( elem, OPENER_TAG+"_"+i );
                BioFormatsBdvOpener opener = gson.fromJson(jsonInString, BioFormatsBdvOpener.class);
                openers.add(opener);
            }

            return new BioFormatsImageLoader( openers, sequenceDescription);
        }
        catch ( final Exception e )
        {
            throw new RuntimeException( e );
        }
    }
}
