package explore;
import bdv.util.BdvFunctions;
import bdv.util.BdvStackSource;
import ch.epfl.biop.bdv.bioformats.bioformatssource.BioFormatsBdvOpener;
import ch.epfl.biop.bdv.bioformats.export.spimdata.BioFormatsConvertFilesToSpimData;
import ch.epfl.biop.bdv.bioformats.samples.DatasetHelper;
import mpicbg.spim.data.generic.AbstractSpimData;
import mpicbg.spim.data.generic.sequence.BasicViewSetup;
import net.imagej.ImageJ;
import net.imglib2.type.numeric.ARGBType;
import ome.units.UNITS;
import ome.units.quantity.Length;
import spimdata.util.Displaysettings;

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

		AbstractSpimData asd = BioFormatsConvertFilesToSpimData.getSpimData(
				opener
						.voxSizeReferenceFrameLength(new Length(1, UNITS.MILLIMETER))
						.positionReferenceFrameLength(new Length(1,UNITS.METER)));

		List<BdvStackSource<?>> bss_list =  BdvFunctions.show(asd);

		for (BdvStackSource bss : bss_list) {
			int viewSetupId = bss_list.indexOf(bss);

			BasicViewSetup bvs = (BasicViewSetup)(asd.getSequenceDescription()
					.getViewSetups().get(viewSetupId));
			Displaysettings ds = bvs.getAttribute(Displaysettings.class);

			bss.setDisplayRange(ds.min,ds.max);

			bss.setColor(new ARGBType(ARGBType.rgba(ds.color[0],ds.color[1], ds.color[2],ds.color[3])));
		}
	}
}
