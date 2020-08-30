package ch.epfl.biop.bdv.bioformats.samples;

import ch.epfl.biop.bdv.bioformats.bioformatssource.BioFormatsBdvOpener;
import ch.epfl.biop.bdv.bioformats.export.spimdata.BioFormatsConvertFilesToSpimData;
import mpicbg.spim.data.generic.AbstractSpimData;
import ome.units.UNITS;
import ome.units.quantity.Length;
import org.scijava.ItemIO;
import org.scijava.command.Command;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;

import java.io.File;
import java.lang.reflect.Field;
import java.util.ArrayList;

@Plugin(type = Command.class,
        menuPath = "BigDataViewer>BDVDataset>Open sample dataset",
        label = "Open sample datasets",
        description = "Downloads and cache datasets on first open attempt.")

public class OpenSample implements Command {

    @Parameter(label = "Choose a sample dataset", choices = {"VSI", "JPG_RGB", "OLYMPUS_OIR", "LIF", "TIF_TIMELAPSE_3D", "ND2_20X", "ND2_60X", "BOTH_ND2"})
    String datasetName;

    @Parameter(type = ItemIO.OUTPUT)
    AbstractSpimData spimData;

    public void run() {
        // Find the datasetname through reflection
        Field[] fields = DatasetHelper.class.getFields();
        if (datasetName.equals("BOTH_ND2")) {
            File f20 = DatasetHelper.getDataset(DatasetHelper.ND2_20X);
            File f60 = DatasetHelper.getDataset(DatasetHelper.ND2_60X);

            Length micron = new Length(1, UNITS.MICROMETER);

            Length millimeter = new Length(1, UNITS.MILLIMETER);

            BioFormatsBdvOpener opener20 =
                    BioFormatsBdvOpener.getOpener().location(f20).auto()
                            .centerPositionConvention()
                            .millimeter()
                            .voxSizeReferenceFrameLength(millimeter)
                            .positionReferenceFrameLength(micron);

            BioFormatsBdvOpener opener60 =
                    BioFormatsBdvOpener.getOpener().location(f60).auto()
                            .centerPositionConvention()
                            .millimeter()
                            .voxSizeReferenceFrameLength(millimeter)
                            .positionReferenceFrameLength(micron);

            ArrayList<BioFormatsBdvOpener> openers = new ArrayList<>();
            openers.add(opener20);
            openers.add(opener60);

            spimData = BioFormatsConvertFilesToSpimData.getSpimData(openers);

            return;
        }
        for (Field f:fields) {
            if (f.getName().toUpperCase().equals(datasetName.toUpperCase())) {
                try {
                    // Dataset found
                    datasetName = (String) f.get(null);
                    System.out.println(datasetName);
                    if (datasetName.equals(DatasetHelper.VSI)) {
                        DatasetHelper.getSampleVSIDataset();
                    }

                    File file = DatasetHelper.getDataset(datasetName);

                    spimData = BioFormatsConvertFilesToSpimData.getSpimData(BioFormatsBdvOpener
                            .getOpener()
                            .location(file).auto()
                            .voxSizeReferenceFrameLength(new Length(1, UNITS.MILLIMETER))
                            .positionReferenceFrameLength(new Length(1, UNITS.MILLIMETER)));

                    return;
                } catch (IllegalAccessException e) {
                    e.printStackTrace();
                }
            }
        }

        System.err.println("Dataset not found!");
    }
}
