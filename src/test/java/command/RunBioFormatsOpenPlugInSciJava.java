package command;

import ch.epfl.biop.bdv.bioformats.BioFormatsOpenPlugInSciJava;
import net.imagej.ImageJ;

public class RunBioFormatsOpenPlugInSciJava
{
	public static void main( String[] args )
	{
		final ImageJ ij = new ImageJ();
		ij.ui().showUI();

		ij.command().run( BioFormatsOpenPlugInSciJava.class, true );
	}
}
