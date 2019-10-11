package explore;
import bdv.tools.brightness.ConverterSetup;
import bdv.util.BdvFunctions;
import bdv.util.BdvHandle;
import bdv.util.BdvOptions;
import ch.epfl.biop.bdv.bioformats.bioformatssource.BioFormatsBdvOpener;
import ch.epfl.biop.bdv.bioformats.bioformatssource.BioFormatsBdvSource;
import ch.epfl.biop.bdv.bioformats.bioformatssource.VolatileBdvSource;
import com.google.gson.Gson;
import net.imagej.ImageJ;

import java.io.File;
import java.util.List;

public class ExploreOpeningImages
{
	// Todo : sample images
	// Color conversion
	public static void main( String[] args )
	{
		final ImageJ ij = new ImageJ();
		ij.ui().showUI();

		File f = new File("C:\\Users\\nicol\\Dropbox\\BIOP\\QuPath Formation\\qpath\\Image_06.vsi");

		List<VolatileBdvSource> sources =
				BioFormatsBdvOpener.getOpener()
						           .file(f)
						           .getVolatileSources("0:-2.*");

		// Because we cannot create an empty viewer
		BdvHandle bdvh = BdvFunctions.show(sources.get(0)).
				getBdvHandle();

		BdvOptions options = BdvOptions.options().addTo(bdvh);

		if (sources.size()>1) {
			for (int i=1;i<sources.size();i++) {
				BdvFunctions.show(sources.get(i),options);
			}
		}

		/*
                    bdv_h_out = BdvFunctions.show(src_out_first).getBdvHandle();
                    bdv_h_out.getViewerPanel().addSource(new SourceAndConverter<>(src_out_first, cvt));
                    bdv_h_out.getViewerPanel().remove(0);
		 */
	}
}
