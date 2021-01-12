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
import net.imagej.ImageJ;
import ome.units.UNITS;
import ome.units.quantity.Length;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Example of opening and displaying a file by using Source ond opener
 */

public class OpenAndDisplayImageWithSource
{
	public static void main( String[] args )
	{
		final ImageJ ij = new ImageJ();
		ij.ui().showUI();

		DatasetHelper.getSampleVSIDataset(); // Cached : no need to worry about double download

		List<Source> sources =
				BioFormatsBdvOpener.getOpener()
                        .location(DatasetHelper.getDataset(DatasetHelper.VSI))
						//.location(DatasetHelper.getDataset(DatasetHelper.TIF_TIMELAPSE_3D))
						//.location(DatasetHelper.getDataset(DatasetHelper.JPG_RGB))
						//.location(DatasetHelper.getDataset(DatasetHelper.OLYMPUS_OIR))
						//.location(DatasetHelper.getDataset(DatasetHelper.LIF))
						.auto() // patches opener based on specific file formats (-> PR to be  modified)
						//.splitRGBChannels() // split RGB channels into 3 channels
						//.switchZandC(true) // switch Z and C
						//.centerPositionConvention() // bioformats location is center of the image
						.cornerPositionConvention() // bioformats location is corner of the image
						//.useCacheBlockSizeFromBioFormats(true) // true by default
						//.cacheBlockSize(512,512,10) // size of cache block used by diskcached image
						//.micronmeter() // unit = micrometer
						.millimeter() // unit = millimeter
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
