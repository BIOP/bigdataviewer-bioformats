package explore;
import bdv.util.BdvFunctions;
import bdv.util.BdvStackSource;
import ch.epfl.biop.bdv.bioformats.bioformatssource.BioFormatsBdvOpener;
import ch.epfl.biop.bdv.bioformats.export.spimdata.BioFormatsConvertFilesToSpimData;
import ch.epfl.biop.bdv.bioformats.samples.DatasetHelper;
import net.imagej.ImageJ;
import ome.units.UNITS;
import ome.units.quantity.Length;

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

		File f = DatasetHelper.getDataset(DatasetHelper.VSI);
		BioFormatsBdvOpener opener = BioFormatsConvertFilesToSpimData.getDefaultOpener(f.getAbsolutePath());

		List<BdvStackSource<?>> bss =	BdvFunctions.show(BioFormatsConvertFilesToSpimData.getSpimData(
				opener
						.voxSizeReferenceFrameLength(new Length(1, UNITS.MILLIMETER))
				        .positionReferenceFrameLength(new Length(1,UNITS.METER))
		));

		for (int i=0;i<bss.size();i++) {
			bss.get(i).setDisplayRange(0,255);
		}
	}
}
