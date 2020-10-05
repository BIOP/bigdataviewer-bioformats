package explore;

import bdv.util.BdvFunctions;
import bdv.util.BdvStackSource;
import ch.epfl.biop.bdv.bioformats.command.BioformatsBigdataviewerBridgeDatasetCommand;
import ch.epfl.biop.bdv.bioformats.command.OpenFilesWithBigdataviewerBioformatsBridgeCommand;
import ch.epfl.biop.bdv.bioformats.export.spimdata.Displaysettings;
import ch.epfl.biop.bdv.bioformats.samples.DatasetHelper;
import mpicbg.spim.data.generic.AbstractSpimData;
import mpicbg.spim.data.generic.sequence.BasicViewSetup;
import net.imagej.ImageJ;
import net.imglib2.type.numeric.ARGBType;

import java.io.File;
import java.util.List;
import java.util.Map;

public class OpenAndDisplayImages {

    public static void main( String[] args ) throws Exception
    {
        DatasetHelper.getSampleVSIDataset();
        final ImageJ ij = new ImageJ();
        ij.ui().showUI();

        DatasetHelper.getSampleVSIDataset(); // Cached : no need to worry about double download

        Map<String, Object> params = BioformatsBigdataviewerBridgeDatasetCommand.getDefaultParameters();

        params.put("files", new File[] {
                DatasetHelper.getDataset(DatasetHelper.VSI)
        });

        AbstractSpimData asd = (AbstractSpimData) ij.command()
                .run(OpenFilesWithBigdataviewerBioformatsBridgeCommand.class,true,params)
                .get().getOutput("spimData");

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
