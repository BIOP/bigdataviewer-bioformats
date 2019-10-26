package explore;

import ch.epfl.biop.bdv.bioformats.show.OpenImageWithBioformatsBigdataviewerBridge;
import net.imagej.ImageJ;

import java.io.File;

public class OpenAndDisplayImageWithBBBCommand {
    public static void main( String[] args )
    {
        DatasetHelper.getSampleVSIDataset(); // Cached : no need to worry about double download

        final ImageJ ij = new ImageJ();
        ij.ui().showUI();
        /*ij.command().run(OpenImageWithBioformatsBigdataviewerBridge.class,true,
                    "files", new File[] {
                                DatasetHelper.getDataset(DatasetHelper.ND2_20X),
                                DatasetHelper.getDataset(DatasetHelper.ND2_60X)
                            },
                            "autosetColor", true,
                            "setGrouping", true,
                            "unit", "micrometer",
                            "advancedParameters", false,
                            "minDisplay",0,
                            "maxDisplay",255
                    );*/

        ij.command().run(OpenImageWithBioformatsBigdataviewerBridge.class,true,
                "files", new File[] {
                        DatasetHelper.getDataset(DatasetHelper.VSI),
                       // DatasetHelper.getDataset(DatasetHelper.JPG_RGB),
                       // DatasetHelper.getDataset(DatasetHelper.TIF_TIMELAPSE_3D),
                },
                "autosetColor", true,
                "setGrouping", true,
                "unit", "millimeter",
                "advancedParameters", false,
                "minDisplay",0,
                "maxDisplay",255
        );
    }
}
