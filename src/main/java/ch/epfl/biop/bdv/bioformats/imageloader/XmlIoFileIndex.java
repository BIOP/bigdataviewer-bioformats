package ch.epfl.biop.bdv.bioformats.imageloader;

import mpicbg.spim.data.generic.base.XmlIoNamedEntity;
import org.jdom2.Element;

import mpicbg.spim.data.SpimDataException;
import mpicbg.spim.data.generic.base.ViewSetupAttributeIo;

@ViewSetupAttributeIo( name = "fileindex", type = FileIndex.class )
public class XmlIoFileIndex extends XmlIoNamedEntity< FileIndex >
{
    public XmlIoFileIndex()
    {
        super( "fileindex", FileIndex.class );
    }

    @Override
    public Element toXml( final FileIndex fi )
    {
        final Element elem = super.toXml( fi );
        return elem;
    }

    @Override
    public FileIndex fromXml( final Element elem ) throws SpimDataException
    {
        final FileIndex tile = super.fromXml( elem );
        return tile;
    }
}