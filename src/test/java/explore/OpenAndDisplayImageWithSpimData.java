package explore;
import bdv.util.BdvFunctions;
import bdv.util.BdvStackSource;
import ch.epfl.biop.bdv.bioformats.BioFormatsMetaDataHelper;
import ch.epfl.biop.bdv.bioformats.bioformatssource.BioFormatsBdvSource;
import ch.epfl.biop.bdv.bioformats.export.spimdata.BioFormatsConvertFilesToSpimData;
import ch.epfl.biop.bdv.bioformats.imageloader.BioFormatsSetupLoader;
import net.imagej.ImageJ;

import java.io.File;
import java.util.List;

/**
 * Example of opening and displaying a file by using conversion to SpimData beforehand
 */

public class OpenAndDisplayImageWithSpimData
{
	public static void main( String[] args )
	{
		final ImageJ ij = new ImageJ();
		ij.ui().showUI();

		DatasetHelper.getSampleVSIDataset(); // Cached : no need to worry about double download

		BioFormatsConvertFilesToSpimData cvt = new BioFormatsConvertFilesToSpimData();
		//File f = DatasetHelper.getDataset(DatasetHelper.VSI);
		//File f = DatasetHelper.getDataset(DatasetHelper.JPG_RGB);
		//File f = DatasetHelper.getDataset("https://www.terroir-fribourg.ch/Media/s/87db6c74daa2645f54a3fa1773662d3a5caed8ae93b1e0.01095846/747.398.1.70/terroir-fribourg-2019-fondue-02-web@2x.jpg");
		File f = DatasetHelper.getDataset(DatasetHelper.TIF_TIMELAPSE_3D);


		cvt.inputFiles = new File[] {f};
		cvt.xmlFilePath = new File(f.getParent());
		cvt.useBioFormatsCacheBlockSize=true;
		cvt.xmlFileName="dataset.xml";
		cvt.unit = "millimeter";
		cvt.saveDataset=false; // Put true if you want to save an xml file for the spimdata
		cvt.switchZandC=false;
		cvt.positionConventionIsCenter=false;
		cvt.verbose=true;

		cvt.run();

		List<BdvStackSource<?>> lbss = BdvFunctions.show(cvt.asd);

		cvt.asd.getSequenceDescription().getViewSetupsOrdered().forEach(id_vs ->{
					int idx = ((mpicbg.spim.data.sequence.ViewSetup)id_vs).getId();
					BioFormatsSetupLoader bfsl = (BioFormatsSetupLoader) cvt.asd.getSequenceDescription().getImgLoader().getSetupImgLoader(idx);
					lbss.get(idx).setColor(
							BioFormatsMetaDataHelper.getSourceColor((BioFormatsBdvSource) bfsl.concreteSource)
					);
					lbss.get(idx).setDisplayRange(0,5000);
					}
				);
	}
}
