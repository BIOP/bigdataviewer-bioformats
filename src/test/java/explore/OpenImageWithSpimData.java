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

public class OpenImageWithSpimData
{
	public static void main( String[] args )
	{
		final ImageJ ij = new ImageJ();
		ij.ui().showUI();

		DatasetHelper.getSampleVSIDataset(); // Cached : no need to worry about double download

		BioFormatsConvertFilesToSpimData cvt = new BioFormatsConvertFilesToSpimData();
		File f = DatasetHelper.getDataset(DatasetHelper.VSI);

		cvt.inputFiles = new File[] {f};
		cvt.xmlFilePath = new File(f.getParent());
		cvt.useBioFormatsCacheBlockSize=true;
		cvt.xmlFileName="dataset.xml";
		cvt.unit = "millimeter";
		cvt.saveDataset=false; // Put true if you want to save an xml file for the spimdata
		cvt.switchZandC=false;
		cvt.positionConventionIsCenter=false;

		cvt.run();

		List<BdvStackSource<?>> bsss = BdvFunctions.show(cvt.asd);

		cvt.asd.getSequenceDescription().getViewSetupsOrdered().forEach(id_vs ->{
					int idx = ((mpicbg.spim.data.sequence.ViewSetup)id_vs).getId();
					BioFormatsSetupLoader bfsl = (BioFormatsSetupLoader) cvt.asd.getSequenceDescription().getImgLoader().getSetupImgLoader(idx);
					bsss.get(idx).setColor(
							BioFormatsMetaDataHelper.getSourceColor((BioFormatsBdvSource) bfsl.concreteSource)
					);
					}
				);
	}
}
