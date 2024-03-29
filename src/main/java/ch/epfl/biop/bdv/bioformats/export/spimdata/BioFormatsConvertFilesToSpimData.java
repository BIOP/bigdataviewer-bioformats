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

package ch.epfl.biop.bdv.bioformats.export.spimdata;

import ch.epfl.biop.bdv.bioformats.BioFormatsMetaDataHelper;
import ch.epfl.biop.bdv.bioformats.imageloader.BioFormatsBdvOpener;
import ch.epfl.biop.bdv.bioformats.imageloader.BioFormatsImageLoader;
import ch.epfl.biop.bdv.bioformats.imageloader.FileIndex;
import ch.epfl.biop.bdv.bioformats.imageloader.FileSerieChannel;
import ch.epfl.biop.bdv.bioformats.imageloader.SeriesNumber;
import ch.epfl.biop.bdv.bioformats.imageloader.SeriesTps;
import loci.formats.*;
import loci.formats.meta.IMetadata;
import mpicbg.spim.data.SpimData;
import mpicbg.spim.data.generic.AbstractSpimData;
import mpicbg.spim.data.registration.ViewRegistration;
import mpicbg.spim.data.registration.ViewRegistrations;
import mpicbg.spim.data.sequence.*;
import net.imglib2.Dimensions;
import net.imglib2.realtransform.AffineTransform3D;
import net.imglib2.type.numeric.ARGBType;
import org.apache.commons.io.FilenameUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import spimdata.util.Displaysettings;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.IntStream;

/**
 * Converting BioFormats structure into an Xml Dataset, compatible for
 * BigDataViewer and FIJI BIG Plugins Limitation Series are considered as Tiles,
 * no Illumination or Angle is considered
 *
 * @author nicolas.chiaruttini@epfl.ch, BIOP, EPFL 2020
 */

public class BioFormatsConvertFilesToSpimData {

	final protected static Logger logger = LoggerFactory.getLogger(
		BioFormatsConvertFilesToSpimData.class);

	private int getChannelId(IMetadata omeMeta, int iSerie, int iChannel,
		boolean isRGB)
	{
		BioFormatsMetaDataHelper.BioformatsChannel channel =
			new BioFormatsMetaDataHelper.BioformatsChannel(omeMeta, iSerie, iChannel,
				false);
		if (!channelToId.containsKey(channel)) {
			// No : add it in the channel hashmap
			channelToId.put(channel, channelCounter);
			logger.debug("New Channel for series " + iSerie + ", channel " +
				iChannel + ", set as number " + channelCounter);
			channelIdToChannel.put(channelCounter, new Channel(channelCounter));
			channelCounter++;
		}
		else {
			logger.debug("Channel for series " + iSerie + ", channel " + iChannel +
				", already known.");
		}

		return channelIdToChannel.get(channelToId.get(channel)).getId();
	}

	int viewSetupCounter = 0;
	int nTileCounter = 0;
	int maxTimepoints = -1;
	int channelCounter = 0;

	final Map<Integer, Channel> channelIdToChannel = new HashMap<>();
	final Map<BioFormatsMetaDataHelper.BioformatsChannel, Integer> channelToId =
		new HashMap<>();
	final Map<Integer, Integer> fileIdxToNumberOfSeries = new HashMap<>();
	final Map<Integer, SeriesTps> fileIdxToNumberOfSeriesAndTimepoints =
		new HashMap<>();
	final Map<Integer, FileSerieChannel> viewSetupToBFFileSerieChannel =
		new HashMap<>();

	public AbstractSpimData getSpimDataInstance(
		List<BioFormatsBdvOpener> openers)
	{
		openers.forEach(o -> o.ignoreMetadata()); // necessary for spimdata
		viewSetupCounter = 0;
		nTileCounter = 0;
		maxTimepoints = -1;
		channelCounter = 0;

		// No Illumination
		Illumination dummy_ill = new Illumination(0);
		// No Angle
		Angle dummy_ang = new Angle(0);
		// Many View Setups
		List<ViewSetup> viewSetups = new ArrayList<>();

		try {
			for (int iF = 0; iF < openers.size(); iF++) {

				FileIndex fi = new FileIndex(iF);
				String dataLocation = openers.get(iF).getDataLocation();
				fi.setName(dataLocation);
				logger.debug("Data located at " + dataLocation);

				IFormatReader memo = openers.get(iF).getNewReader();

				final int iFile = iF;

				final int seriesCount = memo.getSeriesCount();
				logger.debug("Number of Series " + seriesCount);
				final IMetadata omeMeta = (IMetadata) memo.getMetadataStore();

				fileIdxToNumberOfSeries.put(iF, seriesCount);

				// -------------------------- SETUPS For each Series : one per timepoint
				// and one per channel
				IntStream series = IntStream.range(0, seriesCount);
				series.forEach(iSerie -> {
					memo.setSeries(iSerie);
					SeriesNumber sn = new SeriesNumber(iSerie, omeMeta.getImageName(
						iSerie));
					fileIdxToNumberOfSeriesAndTimepoints.put(iFile, new SeriesTps(
						seriesCount, omeMeta.getPixelsSizeT(iSerie).getNumberValue()
							.intValue()));
					// One serie = one Tile
					Tile tile = new Tile(nTileCounter);
					nTileCounter++;
					// ---------- Serie >
					// ---------- Serie > Timepoints
					logger.debug("\t Serie " + iSerie + " Number of timesteps = " +
						omeMeta.getPixelsSizeT(iSerie).getNumberValue().intValue());
					// ---------- Serie > Channels
					logger.debug("\t Serie " + iSerie + " Number of channels = " + omeMeta
						.getChannelCount(iSerie));
					// final int iS = iSerie;
					// Properties of the serie
					IntStream channels = IntStream.range(0, omeMeta.getChannelCount(
						iSerie));
					if (omeMeta.getPixelsSizeT(iSerie).getNumberValue()
						.intValue() > maxTimepoints)
					{
						maxTimepoints = omeMeta.getPixelsSizeT(iSerie).getNumberValue()
							.intValue();
					}
					String imageName = getImageName(dataLocation, seriesCount, omeMeta,
						iSerie);
					sn.setName(imageName);
					Dimensions dims = BioFormatsMetaDataHelper.getSeriesDimensions(
						omeMeta, iSerie); // number of pixels .. no calibration
					logger.debug("X:" + dims.dimension(0) + " Y:" + dims.dimension(1) +
						" Z:" + dims.dimension(2));
					VoxelDimensions voxDims = BioFormatsMetaDataHelper
						.getSeriesVoxelDimensions(omeMeta, iSerie, openers.get(iFile).u,
							openers.get(iFile).voxSizeReferenceFrameLength);
					// Register Setups (one per channel and one per timepoint)
					channels.forEach(iCh -> {
						int ch_id = getChannelId(omeMeta, iSerie, iCh, memo.isRGB());
						String channelName = getChannelName(omeMeta, iSerie, iCh);

						String setupName = imageName + "-" + channelName;
						logger.debug(setupName);
						ViewSetup vs = new ViewSetup(viewSetupCounter, setupName, dims,
							voxDims, tile, // Tile is index of Serie
							channelIdToChannel.get(ch_id), dummy_ang, dummy_ill);
						vs.setAttribute(fi);
						vs.setAttribute(sn);

						// Attempt to set color
						Displaysettings ds = new Displaysettings(viewSetupCounter);
						ds.min = 0;
						ds.max = 255;
						ds.isSet = false;

						// ----------- Color
						ARGBType color = BioFormatsMetaDataHelper.getColorFromMetadata(
							omeMeta, iSerie, iCh);

						if (color != null) {
							ds.isSet = true;
							ds.color = new int[] { ARGBType.red(color.get()), ARGBType.green(
								color.get()), ARGBType.blue(color.get()), ARGBType.alpha(color
									.get()) };
						}
						vs.setAttribute(ds);

						viewSetups.add(vs);
						viewSetupToBFFileSerieChannel.put(viewSetupCounter,
							new FileSerieChannel(iFile, iSerie, iCh));
						viewSetupCounter++;

					});
				});
				memo.close();
			}

			// ------------------- BUILDING SPIM DATA
			ArrayList<String> inputFilesArray = new ArrayList<>();
			for (BioFormatsBdvOpener opener : openers) {
				inputFilesArray.add(opener.getDataLocation());
			}
			List<TimePoint> timePoints = new ArrayList<>();
			IntStream.range(0, maxTimepoints).forEach(tp -> timePoints.add(
				new TimePoint(tp)));

			final ArrayList<ViewRegistration> registrations = new ArrayList<>();

			List<ViewId> missingViews = new ArrayList<>();
			for (int iF = 0; iF < openers.size(); iF++) {
				int iFile = iF;

				IFormatReader memo = openers.get(iF).getNewReader();

				logger.debug("Number of Series : " + memo.getSeriesCount());
				final IMetadata omeMeta = (IMetadata) memo.getMetadataStore();

				int nSeries = fileIdxToNumberOfSeries.get(iF);
				// Need to set view registrations : identity ? how does that work with
				// the one given by the image loader ?
				IntStream series = IntStream.range(0, nSeries);

				series.forEach(iSerie -> {
					final int nTimepoints = omeMeta.getPixelsSizeT(iSerie)
						.getNumberValue().intValue();
					AffineTransform3D rootTransform = BioFormatsMetaDataHelper
						.getSeriesRootTransform(omeMeta, iSerie, openers.get(iFile).u,
							openers.get(iFile).positionPreTransformMatrixArray, // AffineTransform3D
																																	// positionPreTransform,
							openers.get(iFile).positionPostTransformMatrixArray, // AffineTransform3D
																																		// positionPostTransform,
							openers.get(iFile).positionReferenceFrameLength, openers.get(
								iFile).positionIsImageCenter, // boolean positionIsImageCenter,
							openers.get(iFile).voxSizePreTransformMatrixArray, // voxSizePreTransform,
							openers.get(iFile).voxSizePostTransformMatrixArray, // AffineTransform3D
																																	// voxSizePostTransform,
							openers.get(iFile).voxSizeReferenceFrameLength, // null, //Length
																															// voxSizeReferenceFrameLength,
							openers.get(iFile).axesOfImageFlip // axesOfImageFlip
					);
					timePoints.forEach(iTp -> {
						viewSetupToBFFileSerieChannel.keySet().stream().filter(
							viewSetupId -> (viewSetupToBFFileSerieChannel.get(
								viewSetupId).iFile == iFile)).filter(
									viewSetupId -> (viewSetupToBFFileSerieChannel.get(
										viewSetupId).iSerie == iSerie)).forEach(viewSetupId -> {
											if (iTp.getId() < nTimepoints) {

												registrations.add(new ViewRegistration(iTp.getId(),
													viewSetupId, rootTransform));
											}
											else {
												missingViews.add(new ViewId(iTp.getId(), viewSetupId));
											}
										});
					});

				});
				memo.close();
			}

			SequenceDescription sd = new SequenceDescription(new TimePoints(
				timePoints), viewSetups, null, new MissingViews(missingViews));
			sd.setImgLoader(new BioFormatsImageLoader(openers, sd, openers.get(
				0).nFetcherThread, openers.get(0).numPriorities));

			return new SpimData(null, sd, new ViewRegistrations(registrations));
		}
		catch (Exception e) {
			e.printStackTrace();
		}

		return null;
	}

	private String getChannelName(IMetadata omeMeta, int iSerie, int iCh) {
		String channelName = omeMeta.getChannelName(iSerie, iCh);
		channelName = (channelName == null || channelName.equals("")) ? "ch" + iCh
			: channelName;
		return channelName;
	}

	private String getImageName(String dataLocation, int seriesCount,
		IMetadata omeMeta, int iSerie)
	{
		String imageName = omeMeta.getImageName(iSerie);
		String fileNameWithoutExtension = FilenameUtils.removeExtension(new File(
			dataLocation).getName());
		fileNameWithoutExtension = fileNameWithoutExtension.replace(".ome", ""); // above
																																							// only
																																							// removes
																																							// .tif
		if (imageName == null || imageName.equals("")) {
			imageName = fileNameWithoutExtension;
			if (seriesCount > 1) {
				return imageName + "-s" + iSerie;
			}
			else {
				return imageName;
			}
		}
		else {
			return imageName;
		}
	}

	public static AbstractSpimData getSpimData(
		List<BioFormatsBdvOpener> openers)
	{
		return new BioFormatsConvertFilesToSpimData().getSpimDataInstance(openers);
	}

	public static AbstractSpimData getSpimData(BioFormatsBdvOpener opener) {
		ArrayList<BioFormatsBdvOpener> singleOpenerList = new ArrayList<>();
		singleOpenerList.add(opener);
		return BioFormatsConvertFilesToSpimData.getSpimData(singleOpenerList);
	}

	public static AbstractSpimData getSpimData(File f) {
		BioFormatsBdvOpener opener = getDefaultOpener(f.getAbsolutePath());
		return getSpimData(opener);
	}

	public static AbstractSpimData getSpimData(File[] files) {
		ArrayList<BioFormatsBdvOpener> openers = new ArrayList<>();
		for (File f : files) {
			openers.add(getDefaultOpener(f.getAbsolutePath()));
		}
		return BioFormatsConvertFilesToSpimData.getSpimData(openers);
	}

	public static BioFormatsBdvOpener getDefaultOpener(String dataLocation) {
		return BioFormatsBdvOpener.getOpener().location(dataLocation).auto();
	}

}
