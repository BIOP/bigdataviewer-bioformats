package explore;
import bdv.util.BdvFunctions;
import bdv.util.BdvHandle;
import bdv.util.BdvOptions;
import ch.epfl.biop.bdv.bioformats.bioformatssource.BioFormatsBdvOpener;
import ch.epfl.biop.bdv.bioformats.bioformatssource.BioFormatsBdvSource;
import ch.epfl.biop.bdv.bioformats.bioformatssource.VolatileBdvSource;
import net.imagej.ImageJ;

import java.io.File;
import java.util.List;

public class ExploreOpeningImages
{
	public static void main( String[] args )
	{
		final ImageJ ij = new ImageJ();
		ij.ui().showUI();

		File f = new File("C:\\Users\\nicol\\Desktop\\DemoWSR\\Run31_EB1.vsi");

		List<VolatileBdvSource> sources = BioFormatsBdvOpener.getOpener()
															   .file(f)
															   .auto()
															   .getVolatileSources("0:-2.*");

		// Because we cannot create an empty viewer
		BdvHandle bdvh = BdvFunctions.show(sources.get(0)).getBdvHandle();

		BdvOptions options = BdvOptions.options().addTo(bdvh);

		if (sources.size()>1) {
			for (int i=1;i<sources.size();i++) {
				BdvFunctions.show(sources.get(i),options);
			}
		}
		/*
		final BioFormatsOpenImageFileInBdvCommand command = new BioFormatsOpenImageFileInBdvCommand();
		command.cs = ij.command();
		command.createNewWindow = true;
		command.inputFile = new File("/Users/tischer/Desktop/20x_g5_a1.nd2");
		command.ignoreMetadata = false;
		command.unit = BioFormatsMetaDataHelper.MICROMETER;
		command.run();

		final BioFormatsOpenImageFileInBdvCommand command2 = new BioFormatsOpenImageFileInBdvCommand();
		command2.bdv_h = command.bdv_h;
		command2.cs = ij.command();
		command2.createNewWindow = false;
		command2.inputFile = new File("/Users/tischer/Desktop/60x_g5_a1.nd2");
		command2.ignoreMetadata = false;
		command.unit = BioFormatsMetaDataHelper.MICROMETER;
		command2.run();
		*/
	}
}
