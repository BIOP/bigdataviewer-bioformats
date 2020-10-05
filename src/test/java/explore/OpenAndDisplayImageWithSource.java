package explore;
import bdv.util.BdvFunctions;
import bdv.util.BdvHandle;
import bdv.util.BdvOptions;
import bdv.util.BdvStackSource;
import bdv.viewer.Source;
import ch.epfl.biop.bdv.bioformats.BioFormatsMetaDataHelper;
import ch.epfl.biop.bdv.bioformats.bioformatssource.BioFormatsBdvOpener;
import ch.epfl.biop.bdv.bioformats.samples.DatasetHelper;
import net.imagej.ImageJ;
import ome.units.UNITS;
import ome.units.quantity.Length;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Example of opening and displaying a file by using Source ond opener
 */

public class OpenAndDisplayImageWithSource
{
	public static void main( String[] args )
	{
		final ImageJ ij = new ImageJ();
		ij.ui().showUI();

		DatasetHelper.getSampleVSIDataset(); // Cached : no need to worry about double download

		List<Source> sources =
				BioFormatsBdvOpener.getOpener()
                        .location(DatasetHelper.getDataset(DatasetHelper.VSI))
						//.location(DatasetHelper.getDataset(DatasetHelper.TIF_TIMELAPSE_3D))
						//.location(DatasetHelper.getDataset(DatasetHelper.JPG_RGB))
						//.location(DatasetHelper.getDataset(DatasetHelper.OLYMPUS_OIR))
						//.location(DatasetHelper.getDataset(DatasetHelper.LIF))
						.auto() // patches opener based on specific file formats (-> PR to be  modified)
						//.splitRGBChannels() // split RGB channels into 3 channels
						//.switchZandC(true) // switch Z and C
						//.centerPositionConvention() // bioformats location is center of the image
						.cornerPositionConvention() // bioformats location is corner of the image
						//.useCacheBlockSizeFromBioFormats(true) // true by default
						//.cacheBlockSize(512,512,10) // size of cache block used by diskcached image
						//.micronmeter() // unit = micrometer
						.millimeter() // unit = millimeter
						//.unit(UNITS.YARD) // Ok, if you really want...
						//.getConcreteSources()
						.positionReferenceFrameLength(new Length(1, UNITS.MICROMETER)) // Compulsory
						.voxSizeReferenceFrameLength(new Length(100, UNITS.MICROMETER))
						.getVolatileSources()
						.stream().map(src -> (Source) src).collect(Collectors.toList());
						//.getVolatileSources();

		BdvStackSource<?> bss = BdvFunctions.show(sources.get(0));

		bss.setColor(BioFormatsMetaDataHelper.getSourceColor(sources.get(0)));

		bss.setDisplayRange(0,255);

		BdvHandle bdvh = bss.getBdvHandle();

		BdvOptions options = BdvOptions.options().addTo(bdvh);

		for (int i=1;i<sources.size();i++) {
			bss = BdvFunctions.show(sources.get(i), options);
			bss.setColor(BioFormatsMetaDataHelper.getSourceColor(sources.get(i)));
			bss.setDisplayRange(0,255);
		}
	}
}
