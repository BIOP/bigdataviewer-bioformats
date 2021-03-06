/*-
 * #%L
 * Commands and function for opening, conversion and easy use of bioformats format into BigDataViewer
 * %%
 * Copyright (C) 2019 - 2021 Nicolas Chiaruttini, BIOP, EPFL
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
package explore;

import bdv.util.BdvFunctions;
import bdv.util.BdvHandle;
import bdv.util.BdvOptions;
import bdv.util.BdvStackSource;
import bdv.viewer.Source;
import ch.epfl.biop.bdv.bioformats.BioFormatsMetaDataHelper;
import ch.epfl.biop.bdv.bioformats.bioformatssource.BioFormatsBdvOpener;
import ch.epfl.biop.bdv.bioformats.samples.DatasetHelper;
import ome.units.UNITS;
import ome.units.quantity.Length;

import java.util.List;
import java.util.stream.Collectors;

public class OpenAndDisplayImageFromFile {

    //final static public String dataLocation = "Path to your file";
    //"omero:https://omero.epfl.ch/webclient/img_detail/1864/?dataset=408";
    final static public String dataLocation = "N:\\Temp Oli\\Kunal\\Project001_Kunal.lif";//C:\\Users\\nicol\\Downloads\\CZI_Testdata\\S=1_HE_Slide_RGB.czi";//"omero:https://omero.epfl.ch/webclient/img_detail/1864/?dataset=408";


    // See also https://github.com/ome/ome-common-java/blob/master/src/main/java/loci/common/Location.java
    // See : https://github.com/imagej/imagej-omero

    static public void main(String... args) {
        List<Source> sources =
                BioFormatsBdvOpener.getOpener()
                        .location(dataLocation)
                        //.location(DatasetHelper.getDataset(DatasetHelper.TIF_TIMELAPSE_3D))
                        //.location(DatasetHelper.getDataset(DatasetHelper.JPG_RGB))
                        //.location(DatasetHelper.getDataset(DatasetHelper.OLYMPUS_OIR))
                        //.location(DatasetHelper.getDataset(DatasetHelper.LIF))
                        .auto() // patches opener based on specific file formats (-> PR to be  modified)
                        //.splitRGBChannels() // split RGB channels into 3 channels necessary for 16 bits images
                        //.switchZandC(true) // switch Z and C
                        //.centerPositionConvention() // bioformats location is center of the image
                        .cornerPositionConvention() // bioformats location is corner of the image
                        //.useCacheBlockSizeFromBioFormats(true) // true by default
                        //.cacheBlockSize(512,512,10) // size of cache block used by diskcached image
                        .micrometer() // unit = micrometer
                        //.millimeter() // unit = millimeter
                        //.unit(UNITS.YARD) // Ok, if you really want...
                        //.getConcreteSources()
                        .positionReferenceFrameLength(new Length(1, UNITS.MICROMETER)) // Compulsory
                        .voxSizeReferenceFrameLength(new Length(100, UNITS.MICROMETER))
                        .getVolatileSources()
                        .stream().map(src -> (Source) src).collect(Collectors.toList());
        //.getVolatileSources();

        BdvStackSource<?> bss = BdvFunctions.show(sources.get(0));

        bss.setColor(BioFormatsMetaDataHelper.getSourceColor(sources.get(0)));

        bss.setDisplayRange(0,255);

        BdvHandle bdvh = bss.getBdvHandle();

        BdvOptions options = BdvOptions.options().addTo(bdvh);

        for (int i=1;i<sources.size();i++) {
            bss = BdvFunctions.show(sources.get(i), options);
            bss.setColor(BioFormatsMetaDataHelper.getSourceColor(sources.get(i)));
            bss.setDisplayRange(0,255);
        }
    }
}
