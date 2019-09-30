package command;

import ch.epfl.biop.bdv.bioformats.export.xmlbdvdataset.BioFormatsConvertFilesToXmlDataset;
import net.imagej.ImageJ;

public class RunBioFormatsConvertFilesToXmlDataset
{
	public static void main( String[] args )
	{
		final ImageJ ij = new ImageJ();
		ij.ui().showUI();

		ij.command().run( BioFormatsConvertFilesToXmlDataset.class, true );
	}
}
