package ch.epfl.biop.bdv.bioformats.command;

import ch.epfl.biop.bdv.bioformats.BioFormatsMetaDataHelper;
import ch.epfl.biop.bdv.bioformats.bioformatssource.BioFormatsBdvOpener;
import ome.units.quantity.Length;
import ome.units.unit.Unit;
import org.scijava.command.Command;
import org.scijava.plugin.Parameter;

import java.io.File;

abstract public class BioformatsBigdataviewerBridgeDatasetCommand implements Command {
    // Parameter for dataset creation
    @Parameter(label="Physical units of the dataset", choices = {"MILLIMETER","MICROMETER","NANOMETER"})
    public String unit = "MILLIMETER";

    @Parameter(label="Switch Z slices and Channels")
    public boolean switchZC = false;

    @Parameter(label="Split RGB channels")
    public boolean splitRGBChannels = false;

    @Parameter(choices = {"AUTO", "TRUE", "FALSE"})
    public String positionIsCenter = "AUTO";

    @Parameter(choices = {"AUTO", "TRUE", "FALSE"})
    public String switchZandC = "AUTO";

    @Parameter(choices = {"AUTO", "TRUE", "FALSE"})
    public String flipPosition = "AUTO";

    @Parameter
    public boolean useBioFormatsCacheBlockSize = true;

    @Parameter
    public int cacheSizeX, cacheSizeY, cacheSizeZ;

    @Parameter(label="Reference frame size in unit (position)")
    public double refFrameSizeInUnitLocation = 1;

    @Parameter(label="Reference frame size in unit (voxel size)")
    public double refFrameSizeInUnitVoxSize = 1;

    public BioFormatsBdvOpener getOpener(String datalocation) {

        Unit bfUnit = BioFormatsMetaDataHelper.getUnitFromString(unit);

        Length positionReferenceFrameLength = new Length(refFrameSizeInUnitLocation, bfUnit);
        Length voxSizeReferenceFrameLength = new Length(refFrameSizeInUnitVoxSize, bfUnit);

        BioFormatsBdvOpener opener = BioFormatsBdvOpener.getOpener()
                .location(datalocation)
                .unit(unit)
                .auto()
                .ignoreMetadata();

        if (!switchZandC.equals("AUTO")) {
            opener = opener.switchZandC(switchZandC.equals("TRUE"));
        }

        if (!useBioFormatsCacheBlockSize) {
            opener = opener.cacheBlockSize(cacheSizeX,cacheSizeY,cacheSizeZ);
        }

        // Not sure it is useful here because the metadata location is handled somewhere else
        if (!positionIsCenter.equals("AUTO")) {
            if (positionIsCenter.equals("TRUE")) {
                opener = opener.centerPositionConvention();
            } else {
                opener=opener.cornerPositionConvention();
            }
        }

        if (!flipPosition.equals("AUTO")) {
            if (flipPosition.equals("TRUE")) {
                opener = opener.flipPosition();
            }
        }

        opener = opener.unit(unit);

        opener = opener.positionReferenceFrameLength(positionReferenceFrameLength);

        opener = opener.voxSizeReferenceFrameLength(voxSizeReferenceFrameLength);

        if (splitRGBChannels) opener = opener.splitRGBChannels();

        return opener;
    }

    public BioFormatsBdvOpener getOpener(File f) {
        return getOpener(f.getAbsolutePath());
    }

}
