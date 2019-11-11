package ch.epfl.biop.bdv.bioformats.command;

import ch.epfl.biop.bdv.bioformats.bioformatssource.BioFormatsBdvOpener;
import ch.epfl.biop.bdv.bioformats.export.spimdata.BioFormatsConvertFilesToSpimData;
import mpicbg.spim.data.generic.AbstractSpimData;
import org.scijava.ItemIO;
import org.scijava.command.Command;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

@Plugin(type = Command.class,menuPath = "BDV_SciJava>SpimDataset>Open>SpimDataset [BioFormats Bdv Bridge]")
public class ConvertFilesToBdvXmlDatasetCommand extends BioformatsBigdataviewerBridgeDatasetCommand {

    @Parameter
    File[] files;

    @Parameter(type = ItemIO.OUTPUT)
    AbstractSpimData spimData;

    public void run() {
        List<BioFormatsBdvOpener> openers = new ArrayList<>();
        for (File f:files) {
            openers.add(getOpener(f));
        }
        spimData = BioFormatsConvertFilesToSpimData.getSpimData(openers);
    }
}
