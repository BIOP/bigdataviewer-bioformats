package ch.epfl.biop.bdv.bioformats.samples;

import bdv.util.BdvFunctions;
import bdv.util.BdvHandle;
import bdv.util.BdvStackSource;
import ch.epfl.biop.bdv.bioformats.BioformatsBdvDisplayHelper;
import ch.epfl.biop.bdv.bioformats.bioformatssource.BioFormatsBdvOpener;
import ch.epfl.biop.bdv.bioformats.export.spimdata.BioFormatsConvertFilesToSpimData;
import mpicbg.spim.data.generic.AbstractSpimData;
import net.imglib2.img.array.ArrayImg;
import net.imglib2.realtransform.AffineTransform3D;
import ome.units.UNITS;
import ome.units.quantity.Length;
import org.scijava.ItemIO;
import org.scijava.command.Command;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;

import java.io.File;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;

@Plugin(type = Command.class,menuPath = "BDV_SciJava>SpimDataset>Open>Open and show sample dataset")

public class OpenSample implements Command {

    @Parameter(choices = {"VSI", "JPG_RGB", "OLYMPUS_OIR", "LIF", "TIF_TIMELAPSE_3D", "ND2_20X", "ND2_60X", "BOTH_ND2"})
    String datasetName;

    @Parameter(type = ItemIO.OUTPUT)
    BdvHandle bdvh_out;

    public void run() {
        // Find the datasetname through reflection
        Field[] fields = DatasetHelper.class.getFields();
        if (datasetName.equals("BOTH_ND2")) {
            File f20 = DatasetHelper.getDataset(DatasetHelper.ND2_20X);
            File f60 = DatasetHelper.getDataset(DatasetHelper.ND2_60X);


            Length nanometer = new Length(1, UNITS.NANOMETER);


            Length zero = new Length(0, UNITS.MICROMETER);

            Length micron = new Length(1, UNITS.MICROMETER);

            Length millimeter = new Length(1, UNITS.MILLIMETER);

            //AffineTransform3D zKill = new AffineTransform3D();
            //zKill.set(0.01,2,2); // cannot be singular

            BioFormatsBdvOpener opener20 =
                    BioFormatsBdvOpener.getOpener().location(f20).auto()
                            .centerPositionConvention()
                            .millimeter()
                            .voxSizeReferenceFrameLength(millimeter)
                            .positionReferenceFrameLength(micron);
                            //.setPositionPostTransform(zKill);

            BioFormatsBdvOpener opener60 =
                    BioFormatsBdvOpener.getOpener().location(f60).auto()
                            .centerPositionConvention()
                            .millimeter()
                            .voxSizeReferenceFrameLength(millimeter)
                            .positionReferenceFrameLength(micron);
                            //.setPositionPostTransform(zKill);

            ArrayList<BioFormatsBdvOpener> openers = new ArrayList<>();
            openers.add(opener20);
            openers.add(opener60);

            AbstractSpimData spimData = BioFormatsConvertFilesToSpimData.getSpimData(openers);

            List<BdvStackSource<?>> lbss = BdvFunctions.show(spimData);
            bdvh_out = lbss.get(0).getBdvHandle();
            BioformatsBdvDisplayHelper.autosetColorsAngGrouping(lbss, spimData, true, 0, 5000, true);

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

                    AbstractSpimData spimData = BioFormatsConvertFilesToSpimData.getSpimData(BioFormatsBdvOpener
                            .getOpener()
                            .location(file).auto()
                            .voxSizeReferenceFrameLength(new Length(1, UNITS.MILLIMETER))
                            .positionReferenceFrameLength(new Length(1, UNITS.MILLIMETER)));
                    List<BdvStackSource<?>> lbss = BdvFunctions.show(spimData);
                    bdvh_out = lbss.get(0).getBdvHandle();
                    BioformatsBdvDisplayHelper.autosetColorsAngGrouping(lbss, spimData, true, 0, 255, true);

                    return;
                } catch (IllegalAccessException e) {
                    e.printStackTrace();
                }
            }
        }

        System.err.println("Dataset not found!");
    }
}