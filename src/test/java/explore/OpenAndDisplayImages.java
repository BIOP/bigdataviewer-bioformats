/*-
 * #%L
 * Commands and function for opening, conversion and easy use of bioformats format into BigDataViewer
 * %%
 * Copyright (C) 2019 - 2022 Nicolas Chiaruttini, BIOP, EPFL
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
import bdv.util.BdvStackSource;
import ch.epfl.biop.bdv.bioformats.command.BioformatsBigdataviewerBridgeDatasetCommand;
import ch.epfl.biop.bdv.bioformats.command.OpenFilesWithBigdataviewerBioformatsBridgeCommand;
import ch.epfl.biop.bdv.bioformats.samples.DatasetHelper;
import mpicbg.spim.data.SpimData;
import mpicbg.spim.data.XmlIoSpimData;
import mpicbg.spim.data.generic.AbstractSpimData;
import mpicbg.spim.data.generic.sequence.BasicViewSetup;
import net.imagej.ImageJ;
import net.imglib2.type.numeric.ARGBType;
import spimdata.util.Displaysettings;

import java.io.File;
import java.util.List;
import java.util.Map;

public class OpenAndDisplayImages {

	public static void main(String[] args) throws Exception {
		DatasetHelper.getSampleVSIDataset();
		final ImageJ ij = new ImageJ();
		ij.ui().showUI();

		DatasetHelper.getSampleVSIDataset(); // Cached : no need to worry about
																					// double download

		Map<String, Object> params = BioformatsBigdataviewerBridgeDatasetCommand
			.getDefaultParameters();
		// params.put("splitRGBChannels",true);

		params.put("files", new File[] {
			// new File("C:\\Users\\chiarutt\\Desktop\\vsipb\\Image_01.vsi") // See
			// https://forum.image.sc/t/qupath-omero-weird-pyramid-levels/65484
			DatasetHelper.getDataset(DatasetHelper.VSI) });

		AbstractSpimData asd = (AbstractSpimData) ij.command().run(
			OpenFilesWithBigdataviewerBioformatsBridgeCommand.class, true, params)
			.get().getOutput("spimdata");

		List<BdvStackSource<?>> bss_list = BdvFunctions.show(asd);

		for (BdvStackSource bss : bss_list) {
			int viewSetupId = bss_list.indexOf(bss);

			BasicViewSetup bvs = (BasicViewSetup) (asd.getSequenceDescription()
				.getViewSetups().get(viewSetupId));
			Displaysettings ds = bvs.getAttribute(Displaysettings.class);

			bss.setDisplayRange(ds.min, ds.max);

			bss.setColor(new ARGBType(ARGBType.rgba(ds.color[0], ds.color[1],
				ds.color[2], ds.color[3])));
		}
	}

	/*
	    asd.setBasePath(new File("C:\\Users\\chiarutt\\Desktop\\vsipb\\"));
	    new XmlIoSpimData().save((SpimData) asd, "C:\\Users\\chiarutt\\Desktop\\vsipb\\Image_01.xml");
	    SpimData reopened = new XmlIoSpimData().load("C:\\Users\\chiarutt\\Desktop\\vsipb\\Image_01.xml");
	    BdvFunctions.show(reopened);
	 */
}
