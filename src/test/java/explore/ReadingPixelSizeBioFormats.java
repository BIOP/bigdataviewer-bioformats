package explore;

import ch.epfl.biop.bdv.bioformats.samples.DatasetHelper;
import loci.common.services.ServiceFactory;
import loci.formats.IFormatReader;
import loci.formats.ImageReader;
import loci.formats.meta.IMetadata;
import loci.formats.services.OMEXMLService;

import java.io.File;

public class ReadingPixelSizeBioFormats
{
	public static void main( String[] args )
	{
		DatasetHelper.getDataset(DatasetHelper.ND2_60X);

		final double pixelWidth = getNanometerPixelWidthUsingBF( DatasetHelper.getDataset(DatasetHelper.ND2_60X) );

		System.out.println( "Pixel Width = "+pixelWidth+" nm");
	}

	public static double getNanometerPixelWidthUsingBF( File file )
	{
		System.out.println( "Reading voxel size from file " + file.getName() );

		// create OME-XML metadata store
		ServiceFactory factory = null;
		try
		{
			factory = new ServiceFactory();
			OMEXMLService service = factory.getInstance(OMEXMLService.class);
			IMetadata meta = service.createOMEXMLMetadata();

			// create format reader
			IFormatReader reader = new ImageReader();
			reader.setMetadataStore( meta );

			// initialize file
			reader.setId( file.getAbsolutePath() );
			reader.setSeries(0);

			String unit = meta.getPixelsPhysicalSizeX( 0 ).unit().getSymbol();
			final double value = meta.getPixelsPhysicalSizeX( 0 ).value().doubleValue();

			double voxelSize = asNanometers( value, unit );

			return voxelSize;

		}
		catch ( Exception e )
		{
			e.printStackTrace();
		}

		return 0.0;
	}

	public static double asNanometers( double value, String unit )
	{
		double voxelSize = value;

		if ( unit != null )
		{
			if ( unit.equals( "nm" )
					|| unit.equals( "nanometer" )
					|| unit.equals( "nanometers" ) )
			{
				voxelSize = value * 1D;
			}
			else if ( unit.equals( "\u00B5m" )
					|| unit.equals( "um" )
					|| unit.equals( "micrometer" )
					|| unit.equals( "micrometers" )
					|| unit.equals( "microns" )
					|| unit.equals( "micronmeter" ) )
			{
				voxelSize = value * 1000D;
			}
			else if ( unit.hashCode() == 197 || unit.equals( "Angstrom") )
			{
				voxelSize = value / 10D;
			}
			else
			{
				System.out.println( "Could not interpret physical pixel calibration! Unit was: " + unit );
				return value;
			}
		}

		return voxelSize;
	}

}
