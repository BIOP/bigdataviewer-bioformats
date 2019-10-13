package explore;
import bdv.util.BdvFunctions;
import bdv.util.BdvHandle;
import bdv.util.BdvOptions;
import bdv.util.BdvStackSource;
import ch.epfl.biop.bdv.bioformats.BioFormatsMetaDataHelper;
import ch.epfl.biop.bdv.bioformats.bioformatssource.BioFormatsBdvOpener;
import ch.epfl.biop.bdv.bioformats.bioformatssource.VolatileBdvSource;
import net.imagej.ImageJ;

import java.util.List;

/**
 * Example of opening and displayingg a file by using Source ond opener
 */

public class OpenAndDisplayImageWithSource
{
	public static void main( String[] args )
	{
		final ImageJ ij = new ImageJ();
		ij.ui().showUI();

		DatasetHelper.getSampleVSIDataset(); // Cached : no need to worry about double download

		List<VolatileBdvSource> sources =
				BioFormatsBdvOpener.getOpener()
                        .location(DatasetHelper.getDataset(DatasetHelper.VSI))
						.getVolatileSources();

		BdvStackSource<?> bss = BdvFunctions.show(sources.get(0));
		bss.setColor(BioFormatsMetaDataHelper.getSourceColor(sources.get(0)));

		BdvHandle bdvh = bss.getBdvHandle();

		BdvOptions options = BdvOptions.options().addTo(bdvh);

		for (int i=1;i<sources.size();i++) {
			bss = BdvFunctions.show(sources.get(i), options);
			bss.setColor(BioFormatsMetaDataHelper.getSourceColor(sources.get(i)));

		}
	}
}
