package explore;
import bdv.util.BdvFunctions;
import ch.epfl.biop.bdv.bioformats.export.spimdata.BioFormatsConvertFilesToSpimData;
import net.imagej.ImageJ;

import java.io.File;

/**
 * Example of opening and displaying a file by using conversion to SpimData beforehand
 */

public class OpenImageWithSpimData
{
	// Todo : sample images
	// Todo : Color conversion
	public static void main( String[] args )
	{
		final ImageJ ij = new ImageJ();
		ij.ui().showUI();

		File f = new File("C:\\Users\\chiarutt\\Dropbox\\BIOP\\QuPath Formation\\qpath\\Image_06.vsi");

		BioFormatsConvertFilesToSpimData cvt = new BioFormatsConvertFilesToSpimData();

		cvt.inputFiles = new File[] {f};
		cvt.xmlFilePath = new File(f.getParent());
		cvt.useBioFormatsCacheBlockSize=true;
		cvt.xmlFileName="dataset.xml";
		cvt.unit = "millimeter";
		cvt.saveDataset=false; // Put true if you want to save an xml file for the spimdata
		cvt.switchZandC=false;
		cvt.positionConventionIsCenter=false;

		cvt.run();

		BdvFunctions.show(cvt.asd);

	}
}
