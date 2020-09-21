package ch.epfl.biop.bdv.bioformats.imageloader;

import mpicbg.spim.data.SpimDataException;
import mpicbg.spim.data.generic.base.ViewSetupAttributeIo;
import mpicbg.spim.data.generic.base.XmlIoNamedEntity;
import org.jdom2.Element;

@ViewSetupAttributeIo( name = "seriesnumber", type = SeriesNumber.class )
public class XmlIoSeriesNumber extends XmlIoNamedEntity< SeriesNumber >
{
    public XmlIoSeriesNumber()
    {
        super( "seriesnumber", SeriesNumber.class );
    }

    @Override
    public Element toXml( final SeriesNumber fi )
    {
        final Element elem = super.toXml( fi );
        return elem;
    }

    @Override
    public SeriesNumber fromXml( final Element elem ) throws SpimDataException
    {
        final SeriesNumber tile = super.fromXml( elem );
        return tile;
    }
}