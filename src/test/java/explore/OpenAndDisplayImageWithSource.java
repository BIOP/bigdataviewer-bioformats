package explore;
import bdv.util.BdvFunctions;
import bdv.util.BdvHandle;
import bdv.util.BdvOptions;
import ch.epfl.biop.bdv.bioformats.bioformatssource.BioFormatsBdvOpener;
import ch.epfl.biop.bdv.bioformats.bioformatssource.VolatileBdvSource;
import net.imagej.ImageJ;

import java.util.List;

/**
 * Example of opening and displayingg a file by using Source ond opener
 */

public class OpenAndDisplayImageWithSource
{
	// Todo : sample images
	// Todo : Color conversion
	public static void main( String[] args )
	{
		final ImageJ ij = new ImageJ();
		ij.ui().showUI();

		List<VolatileBdvSource> sources =
				BioFormatsBdvOpener.getOpener()
                                   .location("https://biop.epfl.ch/img/splash/physicsTemporal_byRGUIETcrop.jpg")
						           .getVolatileSources();

		BdvHandle bdvh = BdvFunctions.show(sources.get(0)).
				getBdvHandle();

		BdvOptions options = BdvOptions.options().addTo(bdvh);

		if (sources.size()>1) {
			for (int i=1;i<sources.size();i++) {
				BdvFunctions.show(sources.get(i),options);
			}
		}

	}
}
