/*-
 * #%L
 * Commands and function for opening, conversion and easy use of bioformats format into BigDataViewer
 * %%
 * Copyright (C) 2019 - 2020 Nicolas Chiaruttini, BIOP, EPFL
 * %%
 * Redistribution and use in source and binary forms, with or without modification,
 * are permitted provided that the following conditions are met:
 * 
 * 1. Redistributions of source code must retain the above copyright notice, this
 *    list of conditions and the following disclaimer.
 * 
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 * 
 * 3. Neither the name of the BIOP nor the names of its contributors
 *    may be used to endorse or promote products derived from this software without
 *    specific prior written permission.
 * 
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED.
 * IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT,
 * INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING,
 * BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
 * DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF
 * LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE
 * OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED
 * OF THE POSSIBILITY OF SUCH DAMAGE.
 * #L%
 */
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
        def.put("flipPositionX","AUTO");
        def.put("flipPositionY","AUTO");
        def.put("flipPositionZ","AUTO");
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
    public String flipPositionX = "AUTO";

    @Parameter(required = false, choices = {"AUTO", "TRUE", "FALSE"})
    public String flipPositionY = "AUTO";

    @Parameter(required = false, choices = {"AUTO", "TRUE", "FALSE"})
    public String flipPositionZ = "AUTO";

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

        if (!flipPositionX.equals("AUTO")) {
            if (flipPositionX.equals("TRUE")) {
                opener = opener.flipPositionX();
            }
        }

        if (!flipPositionY.equals("AUTO")) {
            if (flipPositionY.equals("TRUE")) {
                opener = opener.flipPositionY();
            }
        }

        if (!flipPositionZ.equals("AUTO")) {
            if (flipPositionZ.equals("TRUE")) {
                opener = opener.flipPositionZ();
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
