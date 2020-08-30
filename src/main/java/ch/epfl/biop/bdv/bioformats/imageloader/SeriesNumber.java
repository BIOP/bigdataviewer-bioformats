package ch.epfl.biop.bdv.bioformats.imageloader;


import mpicbg.spim.data.generic.base.NamedEntity;

public class SeriesNumber extends NamedEntity implements Comparable<SeriesNumber>
{

    public SeriesNumber(final int id, final String name)
    {
        super( id, name );
    }

    public SeriesNumber(final int id )
    {
        this( id, Integer.toString( id ) );
    }

    /**
     * Get the unique id of this location.
     */
    @Override
    public int getId()
    {
        return super.getId();
    }

    /**
     * Get the name of this tile.
     *
     * The name is used for example to replace it in filenames when
     * opening individual 3d-stacks (e.g. SPIM_TL20_Tile1_Angle45.tif)
     */
    @Override
    public String getName()
    {
        return super.getName();
    }

    /**
     * Set the name of this tile.
     */
    @Override
    public void setName( final String name )
    {
        super.setName( name );
    }

    /**
     * Compares the {@link #getId() ids}.
     */
    @Override
    public int compareTo( final SeriesNumber o )
    {
        return getId() - o.getId();
    }

    protected SeriesNumber()
    {}
}
