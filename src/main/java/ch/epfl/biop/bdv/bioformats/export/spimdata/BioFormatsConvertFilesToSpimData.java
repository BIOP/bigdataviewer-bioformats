package ch.epfl.biop.bdv.bioformats.export.spimdata;

import ch.epfl.biop.bdv.bioformats.BioFormatsMetaDataHelper;
import ch.epfl.biop.bdv.bioformats.bioformatssource.BioFormatsBdvOpener;
import ch.epfl.biop.bdv.bioformats.imageloader.*;
import loci.formats.*;
import loci.formats.meta.IMetadata;
import mpicbg.spim.data.SpimData;
import mpicbg.spim.data.generic.AbstractSpimData;
import mpicbg.spim.data.registration.ViewRegistration;
import mpicbg.spim.data.registration.ViewRegistrations;
import mpicbg.spim.data.sequence.*;
import net.imglib2.Dimensions;
import net.imglib2.type.numeric.ARGBType;
import ome.units.UNITS;
import spimdata.util.Displaysettings;

import java.awt.*;
import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.stream.IntStream;

import static ch.epfl.biop.bdv.bioformats.BioFormatsMetaDataHelper.getColorFromWavelength;

/**
 * Converting BioFormats structure into an Xml Dataset, compatible for BigDataViewer and FIJI BIG Plugins
 * Limitation
 * Series are considered as Tiles, no Illumination or Angle is considered
 *
 * @author nicolas.chiaruttini@epfl.ch, BIOP, EPFL 2020
 */

public class BioFormatsConvertFilesToSpimData {

    public static Consumer<String> log = s -> {};

    private int getChannelId(IMetadata omeMeta, int iSerie, int iChannel, boolean isRGB) {
        BioFormatsMetaDataHelper.BioformatsChannel channel = new BioFormatsMetaDataHelper.BioformatsChannel(omeMeta, iSerie, iChannel, false);
        if (!channelToId.containsKey(channel)) {
            // No : add it in the channel hashmap
            channelToId.put(channel,channelCounter);
            log.accept(" \t \t \t New Channel, set as number "+channelCounter);
            channelIdToChannel.put(channelCounter, new Channel(channelCounter));
            channelCounter++;
        }
        int idChannel = channelIdToChannel.get(channelToId.get(channel)).getId();
        return idChannel;
    }

    int viewSetupCounter = 0;
    int nTileCounter = 0;
    int maxTimepoints = -1;
    int channelCounter = 0;

    Map<Integer,Channel> channelIdToChannel = new HashMap<>();
    Map<BioFormatsMetaDataHelper.BioformatsChannel,Integer> channelToId = new HashMap<>();
    Map<Integer,Integer> fileIdxToNumberOfSeries = new HashMap<>();
    Map<Integer, SeriesTps> fileIdxToNumberOfSeriesAndTimepoints = new HashMap<>();
    Map<Integer, FileSerieChannel> viewSetupToBFFileSerieChannel = new HashMap<>();

    public AbstractSpimData getSpimDataInstance(List<BioFormatsBdvOpener> openers) {
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
            for (int iF=0;iF<openers.size();iF++) {
                FileIndex fi = new FileIndex(iF);
                fi.setName(openers.get(iF).getDataLocation());
                log.accept("Data : "+ openers.get(iF).getDataLocation());
                IFormatReader readerIdx = new ImageReader();

                readerIdx.setFlattenedResolutions(false);
                Memoizer memo = new Memoizer( readerIdx );

                memo.setId(openers.get(iF).getDataLocation());
                final int iFile = iF;

                log.accept("Number of Series : " + memo.getSeriesCount());
                final IMetadata omeMeta = (IMetadata) memo.getMetadataStore();

                fileIdxToNumberOfSeries.put(iF, memo.getSeriesCount() );

                // -------------------------- SETUPS For each Series : one per timepoint and one per channel
                IntStream series = IntStream.range(0, memo.getSeriesCount());
                series.forEach(iSerie -> {
                    memo.setSeries(iSerie);
                    SeriesNumber sn = new SeriesNumber(iSerie);
                    sn.setName("Series_"+iSerie);
                    fileIdxToNumberOfSeriesAndTimepoints.put(iFile, new SeriesTps(memo.getSeriesCount(),omeMeta.getPixelsSizeT(iSerie).getNumberValue().intValue()));
                    // One serie = one Tile
                    Tile tile = new Tile(nTileCounter);
                    nTileCounter++;
                    // ---------- Serie >
                    // ---------- Serie > Timepoints
                    log.accept("\t Serie " + iSerie + " Number of timesteps = " + omeMeta.getPixelsSizeT(iSerie).getNumberValue().intValue());
                    // ---------- Serie > Channels
                    log.accept("\t Serie " + iSerie + " Number of channels = " + omeMeta.getChannelCount(iSerie));
                    //final int iS = iSerie;
                    // Properties of the serie
                    IntStream channels = IntStream.range(0, omeMeta.getChannelCount(iSerie));
                    if (omeMeta.getPixelsSizeT(iSerie).getNumberValue().intValue() > maxTimepoints) {
                        maxTimepoints = omeMeta.getPixelsSizeT(iSerie).getNumberValue().intValue();
                    }
                    String imageName = omeMeta.getImageName(iSerie);
                    Dimensions dims = BioFormatsMetaDataHelper.getSeriesDimensions(omeMeta, iSerie); // number of pixels .. no calibration
                    VoxelDimensions voxDims = BioFormatsMetaDataHelper.getSeriesVoxelDimensions(omeMeta, iSerie, openers.get(iFile).u, openers.get(iFile).voxSizeReferenceFrameLength);
                    // Register Setups (one per channel and one per timepoint)
                    channels.forEach(
                            iCh -> {
                                int ch_id = getChannelId(omeMeta, iSerie, iCh, memo.isRGB());
                                String channelName = omeMeta.getChannelName(iSerie, iCh);
                                //IntStream timepoints = IntStream.range(0, omeMeta.getPixelsSizeT(iSerie).getNumberValue().intValue());
                                //timepoints.forEach(
                                //        iTp -> {
                                String setupName = imageName
                                        + "-" + channelName;// + ":" + iTp;
                                log.accept(setupName);
                                ViewSetup vs = new ViewSetup(
                                        viewSetupCounter,
                                        setupName,
                                        dims,
                                        voxDims,
                                        tile, // Tile is index of Serie
                                        channelIdToChannel.get(ch_id),
                                        dummy_ang,
                                        dummy_ill);
                                vs.setAttribute(fi);
                                vs.setAttribute(sn);

                                // Attempt to set color
                                Displaysettings ds = new Displaysettings(viewSetupCounter);
                                ds.min = 0;
                                ds.max = 255;
                                ds.isSet = false;

                                // ----------- Color
                                ome.xml.model.primitives.Color c = omeMeta.getChannelColor(iSerie, iCh);
                                ARGBType color = null;
                                if (c != null) {
                                    color = new ARGBType(ARGBType.rgba(c.getRed(), c.getGreen(), c.getBlue(), 255));
                                } else {
                                    if (omeMeta.getChannelEmissionWavelength(iSerie, iCh) != null) {
                                        int emission = omeMeta.getChannelEmissionWavelength(iSerie, iCh)
                                                .value(UNITS.NANOMETER)
                                                .intValue();
                                        Color cAwt = getColorFromWavelength(emission);
                                        color = new ARGBType(ARGBType.rgba(cAwt.getRed(), cAwt.getGreen(), cAwt.getBlue(), 255));
                                    }
                                }

                                if (color!=null) {
                                    ds.isSet = true;
                                    ds.color = new int[]{
                                            ARGBType.red(color.get()),
                                            ARGBType.green(color.get()),
                                            ARGBType.blue(color.get()),
                                            ARGBType.alpha(color.get())};
                                }
                                vs.setAttribute(ds);

                                viewSetups.add(vs);
                                viewSetupToBFFileSerieChannel.put(viewSetupCounter, new FileSerieChannel(iFile, iSerie, iCh));
                                viewSetupCounter++;
                                //       });
                            });
                });
                memo.close();
            }

            // ------------------- BUILDING SPIM DATA
            ArrayList<String> inputFilesArray = new ArrayList<>();
            for (BioFormatsBdvOpener opener:openers) {
                inputFilesArray.add(opener.getDataLocation());
            }
            List<TimePoint> timePoints = new ArrayList<>();
            IntStream.range(0,maxTimepoints).forEach(tp -> timePoints.add(new TimePoint(tp)));

            final ArrayList<ViewRegistration> registrations = new ArrayList<>();

            List<ViewId> missingViews = new ArrayList<>();
            for (int iF=0;iF<openers.size();iF++) {
                int iFile = iF;

                IFormatReader readerIdx = new ImageReader();

                readerIdx.setFlattenedResolutions(false);
                Memoizer memo = new Memoizer( readerIdx );

                memo.setId(openers.get(iF).getDataLocation());
                //final IFormatReader reader = memo;

                log.accept("Number of Series : " + memo.getSeriesCount());
                final IMetadata omeMeta = (IMetadata) memo.getMetadataStore();

                int nSeries = fileIdxToNumberOfSeries.get(iF);
                // Need to set view registrations : identity ? how does that work with the one given by the image loader ?
                IntStream series = IntStream.range(0, nSeries);

                series.forEach(iSerie -> {
                    final int nTimepoints = omeMeta.getPixelsSizeT(iSerie).getNumberValue().intValue();

                    timePoints.forEach(iTp -> {
                        viewSetupToBFFileSerieChannel
                                .keySet()
                                .stream()
                                .filter(viewSetupId -> (viewSetupToBFFileSerieChannel.get(viewSetupId).iFile == iFile))
                                .filter(viewSetupId -> (viewSetupToBFFileSerieChannel.get(viewSetupId).iSerie == iSerie))
                                .forEach(viewSetupId -> {
                                    if (iTp.getId()<nTimepoints) {
                                        registrations.add(new ViewRegistration(iTp.getId(), viewSetupId, BioFormatsMetaDataHelper.getSeriesRootTransform(
                                                omeMeta,
                                                iSerie,
                                                openers.get(iFile).u,
                                                openers.get(iFile).positionPreTransformMatrixArray, //AffineTransform3D positionPreTransform,
                                                openers.get(iFile).positionPostTransformMatrixArray, //AffineTransform3D positionPostTransform,
                                                openers.get(iFile).positionReferenceFrameLength,
                                                openers.get(iFile).positionIsImageCenter, //boolean positionIsImageCenter,
                                                openers.get(iFile).voxSizePreTransformMatrixArray, //voxSizePreTransform,
                                                openers.get(iFile).voxSizePostTransformMatrixArray, //AffineTransform3D voxSizePostTransform,
                                                openers.get(iFile).voxSizeReferenceFrameLength, //null, //Length voxSizeReferenceFrameLength,
                                                openers.get(iFile).axesOfImageFlip // axesOfImageFlip
                                        )));
                                    } else {
                                        missingViews.add(new ViewId(iTp.getId(), viewSetupId));
                                    }
                                });
                    });

                });
                memo.close();
            }

            SequenceDescription sd = new SequenceDescription( new TimePoints( timePoints ), viewSetups , null, new MissingViews(missingViews));
            sd.setImgLoader(new BioFormatsImageLoader(openers,sd,openers.get(0).nFetcherThread, openers.get(0).numPriorities));

            final SpimData spimData = new SpimData( null, sd, new ViewRegistrations( registrations ) );
            return spimData;
        } catch (Exception e) {
            e.printStackTrace();
        }

        return null;
    }

    public static AbstractSpimData getSpimData(List<BioFormatsBdvOpener> openers) {
        return new BioFormatsConvertFilesToSpimData().getSpimDataInstance(openers);
    }

    public static AbstractSpimData getSpimData(BioFormatsBdvOpener opener) {
        ArrayList<BioFormatsBdvOpener> singleOpenerList = new ArrayList<>();
        singleOpenerList.add(opener);
        return new BioFormatsConvertFilesToSpimData().getSpimData(singleOpenerList);
    }

    public static AbstractSpimData getSpimData(File f) {
        BioFormatsBdvOpener opener = getDefaultOpener(f.getAbsolutePath());
        return getSpimData(opener);
    }

    public static AbstractSpimData getSpimData(File[] files) {
        ArrayList<BioFormatsBdvOpener> openers = new ArrayList<>();
        for (File f:files) {
            openers.add(getDefaultOpener(f.getAbsolutePath()));
        }
        return new BioFormatsConvertFilesToSpimData().getSpimData(openers);
    }

    public static BioFormatsBdvOpener getDefaultOpener(String dataLocation) {
        return BioFormatsBdvOpener.getOpener().location(dataLocation).auto();
    }

}
