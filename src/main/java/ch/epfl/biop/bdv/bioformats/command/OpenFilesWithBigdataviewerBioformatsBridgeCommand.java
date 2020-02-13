package ch.epfl.biop.bdv.bioformats.command;

import ch.epfl.biop.bdv.bioformats.BioFormatsMetaDataHelper;
import ch.epfl.biop.bdv.bioformats.BioformatsBdvDisplayHelper;
import ch.epfl.biop.bdv.bioformats.bioformatssource.BioFormatsBdvOpener;
import ch.epfl.biop.bdv.bioformats.bioformatssource.BioFormatsBdvSource;
import ch.epfl.biop.bdv.bioformats.export.spimdata.BioFormatsConvertFilesToSpimData;
import mpicbg.spim.data.generic.AbstractSpimData;
import org.scijava.ItemIO;
import org.scijava.command.Command;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

@Plugin(type = Command.class,
        menuPath = "BigDataViewer>SpimDataset>Open [BioFormats Bdv Bridge]",
        label = "Opens and show in a bdv window files by using bioformats reader",
        description = "Support bioformmats multiresolution api. Attempts to set colors based" +
                "on bioformats metadata. Do not attempt auto contrast.")
public class OpenFilesWithBigdataviewerBioformatsBridgeCommand extends BioformatsBigdataviewerBridgeDatasetCommand {

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
