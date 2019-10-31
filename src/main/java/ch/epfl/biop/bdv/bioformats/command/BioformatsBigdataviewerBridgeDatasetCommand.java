package ch.epfl.biop.bdv.bioformats.command;

import ch.epfl.biop.bdv.bioformats.BioFormatsMetaDataHelper;
import ch.epfl.biop.bdv.bioformats.bioformatssource.BioFormatsBdvOpener;
import ome.units.quantity.Length;
import ome.units.unit.Unit;
import org.scijava.command.Command;
import org.scijava.plugin.Parameter;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

abstract public class BioformatsBigdataviewerBridgeDatasetCommand implements Command {

    static public Map<String, Object> getDefaultParameters() {
        Map<String, Object> def = new HashMap();
        def.put("unit", "MILLIMETER");
        def.put("splitRGBChannels",false);
        def.put("positionIsCenter","AUTO");
        def.put("switchZandC","AUTO");
        def.put("flipPosition","AUTO");
        def.put("useBioFormatsCacheBlockSize",true);
        def.put("cacheSizeX",512);
        def.put("cacheSizeY",512);
        def.put("cacheSizeZ",1);
        def.put("refFrameSizeInUnitLocation",1);
        def.put("refFrameSizeInUnitVoxSize",1);
        return def;
    }

    // Parameter for dataset creation
    @Parameter(required = false, label="Physical units of the dataset", choices = {"MILLIMETER","MICROMETER","NANOMETER"})
    public String unit = "MILLIMETER";

    @Parameter(required = false, label="Split RGB channels")
    public boolean splitRGBChannels = false;

    @Parameter(required = false, choices = {"AUTO", "TRUE", "FALSE"})
    public String positionIsCenter = "AUTO";

    @Parameter(required = false, choices = {"AUTO", "TRUE", "FALSE"})
    public String switchZandC = "AUTO";

    @Parameter(required = false, choices = {"AUTO", "TRUE", "FALSE"})
    public String flipPosition = "AUTO";

    @Parameter(required = false)
    public boolean useBioFormatsCacheBlockSize = true;

    @Parameter(required = false)
    public int cacheSizeX = 512, cacheSizeY = 512, cacheSizeZ = 1;

    @Parameter(required = false, label="Reference frame size in unit (position)")
    public double refFrameSizeInUnitLocation = 1;

    @Parameter(required = false, label="Reference frame size in unit (voxel size)")
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
