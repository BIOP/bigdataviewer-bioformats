package explore;

import ch.epfl.biop.bdv.bioformats.command.BioformatsBigdataviewerBridgeDatasetCommand;
import ch.epfl.biop.bdv.bioformats.command.OpenFilesWithBigdataviewerBioformatsBridgeCommand;
import net.imagej.ImageJ;

import java.io.File;
import java.util.Map;

public class OpenAndDisplayImages {
    public static void main( String[] args )
    {
        DatasetHelper.getSampleVSIDataset();
        final ImageJ ij = new ImageJ();
        ij.ui().showUI();
        Map<String, Object> params = BioformatsBigdataviewerBridgeDatasetCommand.getDefaultParameters();

        params.put("files", new File[] {
                DatasetHelper.getDataset(DatasetHelper.ND2_60X)
        });
        params.put("setColor", true);
        params.put("setGrouping", true);
        params.put("minDisplay",1000);
        params.put("maxDisplay",5000);

        ij.command().run(OpenFilesWithBigdataviewerBioformatsBridgeCommand.class,true,params);
    }
}
