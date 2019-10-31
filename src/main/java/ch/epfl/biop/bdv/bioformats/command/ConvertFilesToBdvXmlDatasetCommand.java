package ch.epfl.biop.bdv.bioformats.command;

import ch.epfl.biop.bdv.bioformats.bioformatssource.BioFormatsBdvOpener;
import ch.epfl.biop.bdv.bioformats.export.spimdata.BioFormatsConvertFilesToSpimData;
import mpicbg.spim.data.SpimData;
import mpicbg.spim.data.SpimDataException;
import mpicbg.spim.data.XmlIoSpimData;
import mpicbg.spim.data.generic.AbstractSpimData;
import org.apache.commons.io.FilenameUtils;
import org.scijava.ItemIO;
import org.scijava.command.Command;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

@Plugin(type = Command.class,menuPath = "BDV_SciJava>Export>Convert Files to Bdv Dataset with Bioformats (SciJava)")
public class ConvertFilesToBdvXmlDatasetCommand extends BioformatsBigdataviewerBridgeDatasetCommand {

    @Parameter
    File[] files;

    @Parameter(type = ItemIO.OUTPUT)
    AbstractSpimData spimData;

    @Parameter
    boolean saveDataset = false;

    @Parameter(required=false)
    File datasetFileName;

    public void run() {

        String renamedXmlFileName = FilenameUtils.removeExtension(datasetFileName.getAbsolutePath())+".xml";
        List<BioFormatsBdvOpener> openers = new ArrayList<>();
        for (File f:files) {
            openers.add(getOpener(f));
        }
        spimData = BioFormatsConvertFilesToSpimData.getSpimData(openers);
        if (saveDataset) {
            spimData.setBasePath(datasetFileName.getParentFile());
            try {
                new XmlIoSpimData().save( (SpimData) spimData, renamedXmlFileName );
            } catch (SpimDataException e) {
                e.printStackTrace();
            }
        }
    }
}
