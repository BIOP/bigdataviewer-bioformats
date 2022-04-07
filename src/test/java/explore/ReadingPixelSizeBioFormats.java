/*-
 * #%L
 * Commands and function for opening, conversion and easy use of bioformats format into BigDataViewer
 * %%
 * Copyright (C) 2019 - 2022 Nicolas Chiaruttini, BIOP, EPFL
 * %%
 * Redistribution and use in source and binary forms, with or without modification,
 * are permitted provided that the following conditions are met:
 * 
 * 1. Redistributions of source code must retain the above copyright notice, this
 *    list of conditions and the following disclaimer.
 * 
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 * 
 * 3. Neither the name of the BIOP nor the names of its contributors
 *    may be used to endorse or promote products derived from this software without
 *    specific prior written permission.
 * 
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED.
 * IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT,
 * INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING,
 * BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
 * DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF
 * LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE
 * OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED
 * OF THE POSSIBILITY OF SUCH DAMAGE.
 * #L%
 */
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
