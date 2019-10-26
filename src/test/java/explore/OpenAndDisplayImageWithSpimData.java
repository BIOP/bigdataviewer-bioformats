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
		File f = DatasetHelper.getDataset(DatasetHelper.VSI);

		cvt.inputFiles = new File[] {
				DatasetHelper.getDataset(DatasetHelper.VSI),
				DatasetHelper.getDataset(DatasetHelper.JPG_RGB),
				DatasetHelper.getDataset(DatasetHelper.TIF_TIMELAPSE_3D),
		};
		// OR
		cvt.inputFiles = new File[] {
				DatasetHelper.getDataset(DatasetHelper.ND2_20X),
				DatasetHelper.getDataset(DatasetHelper.ND2_60X)
		};
		cvt.xmlFilePath = new File(f.getParent());
		cvt.useBioFormatsCacheBlockSize=true;
		   // ignored if useBioFormatsCacheBlockSize is true
		   cvt.cacheSizeX=64;
		   cvt.cacheSizeY=64;
		   cvt.cacheSizeZ=64;
		cvt.xmlFileName="dataset.xml";
		cvt.unit = "micrometer";
		cvt.saveDataset=true; // Put true if you want to save an xml file for the spimdata
		cvt.switchZandC="AUTO";
		cvt.positionIsCenter="AUTO";
		cvt.flipPosition="AUTO";
		cvt.verbose=false;//true;

		cvt.run();

		List<BdvStackSource<?>> lbss = BdvFunctions.show(cvt.asd);

		cvt.asd.getSequenceDescription().getViewSetupsOrdered().forEach(id_vs ->{
					int idx = ((mpicbg.spim.data.sequence.ViewSetup)id_vs).getId();
					BioFormatsSetupLoader bfsl = (BioFormatsSetupLoader) cvt.asd.getSequenceDescription().getImgLoader().getSetupImgLoader(idx);
					lbss.get(idx).setColor(
							BioFormatsMetaDataHelper.getSourceColor((BioFormatsBdvSource) bfsl.concreteSource)
					);
					lbss.get(idx).setDisplayRange(0,255);
					}
				);
	}
}
