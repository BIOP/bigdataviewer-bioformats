package ch.epfl.biop.bdv.bioformats.export.spimdata;

import ch.epfl.biop.bdv.bioformats.BioFormatsMetaDataHelper;
import ch.epfl.biop.bdv.bioformats.bioformatssource.BioFormatsBdvOpener;
import ch.epfl.biop.bdv.bioformats.imageloader.BioFormatsImageLoader;
import ch.epfl.biop.bdv.bioformats.imageloader.FileSerieChannel;
import ch.epfl.biop.bdv.bioformats.imageloader.SeriesTps;
import loci.formats.*;
import loci.formats.meta.IMetadata;
import mpicbg.spim.data.SpimData;
import mpicbg.spim.data.XmlIoSpimData;
import mpicbg.spim.data.generic.AbstractSpimData;
import mpicbg.spim.data.registration.ViewRegistration;
import mpicbg.spim.data.registration.ViewRegistrations;
import mpicbg.spim.data.sequence.*;
import net.imglib2.Dimensions;
import ome.units.quantity.Length;
import ome.units.unit.Unit;
import org.apache.commons.io.FilenameUtils;
import org.scijava.ItemIO;
import org.scijava.command.Command;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.stream.IntStream;

/**
 * Converting BioFormats structure into an Xml Dataset, compatible for BigDataViewer and FIJI BIG Plugins
 * Limitation
 * Series are considered as Tiles, no Illumination or Angle is considered
 *
 * @author nicolas.chiaruttini@epfl.ch
 */

@Plugin(type = Command.class,menuPath = "BDV_SciJava>Export>Convert Files to Xml Dataset with Bioformats (SciJava)")
public class BioFormatsConvertFilesToSpimData implements Command {

    public static final String MILLIMETER = "millimeter"; // Should match UNITS.MICROMETER.getSymbol() but cannot be used in choices if defined like that cf tischi's test
    public static final String MICROMETER = "micrometer";
    public static final String NANOMETER = "nanometer";

    @Parameter(label = "Image Files")
    public File[] inputFiles;

    @Parameter(required=false, label = "output path, empty = same folder as input", style = "directory") // To append datasets potentially
    public File xmlFilePath;

    @Parameter
    public boolean useBioFormatsCacheBlockSize = true;

    @Parameter
    public int cacheSizeX, cacheSizeY, cacheSizeZ;

    @Parameter(choices = {MILLIMETER,MICROMETER,NANOMETER})
    public String unit;

    @Parameter(choices = {"AUTO", "TRUE", "FALSE"})
    public String positionIsCenter = "AUTO";

    @Parameter(choices = {"AUTO", "TRUE", "FALSE"})
    public String switchZandC = "AUTO";

    @Parameter(choices = {"AUTO", "TRUE", "FALSE"})
    public String flipPosition = "AUTO";

    @Parameter(required=false, label = "output file name") // To append datasets potentially
    public String xmlFileName;

    @Parameter(type = ItemIO.OUTPUT)
    public AbstractSpimData asd;

    @Parameter
    public boolean saveDataset=true;

    @Parameter
    public boolean verbose = false;

    @Parameter(label = "Reference Frame Position value in specified unit ")
    public double refFramePositionValue = 1;

    @Parameter(label = "Reference Frame Voxel Size value in specified unit ")
    public double refFrameVoxSizeValue = 1;

    public Consumer<String> log = s -> {};

    int viewSetupCounter = 0;

    int nTileCounter = 0;

    int maxTimepoints = -1;

    int channelCounter = 0;

    Map<BioFormatsMetaDataHelper.BioformatsChannel,Integer> channelToId = new HashMap<>();

    Map<Integer,Integer> fileIdxToNumberOfSeries = new HashMap<>();
    Map<Integer,Channel> channelIdToChannel = new HashMap<>();

    Map<Integer, SeriesTps> fileIdxToNumberOfSeriesAndTimepoints = new HashMap<>();
    Map<Integer, FileSerieChannel> viewSetupToBFFileSerieChannel = new HashMap<>();

    Unit bfUnit;

    @Override
    public void run() {

        this.positionIsCenter = positionIsCenter.trim().toUpperCase();
        this.switchZandC = switchZandC.trim().toUpperCase();

        if (verbose) {
            log = s -> System.out.println(s);
        } else {
            log = s -> {};
        }

        bfUnit = BioFormatsMetaDataHelper.getUnitFromString(unit);

        Length positionReferenceFrameLength = new Length(refFramePositionValue, bfUnit);
        Length voxSizeReferenceFrameLength = new Length(refFrameVoxSizeValue, bfUnit);

        IFormatReader readerIdx = new ImageReader();

        readerIdx.setFlattenedResolutions(false);
        Memoizer memo = new Memoizer( readerIdx );

        final IMetadata omeMetaOmeXml = MetadataTools.createOMEXMLMetadata();
        memo.setMetadataStore(omeMetaOmeXml);

        // No Illumination
        Illumination dummy_ill = new Illumination(0);
        // No Angle
        Angle dummy_ang = new Angle(0);
        // Many View Setups
        List<ViewSetup> viewSetups = new ArrayList<>();

        try {
            for (int iF=0;iF<inputFiles.length;iF++) {
                log.accept("File : "+ inputFiles[iF].getAbsolutePath());

                memo.setId(inputFiles[iF].getAbsolutePath());
                final int iFile = iF;
                final IFormatReader reader = memo;

                log.accept("Number of Series : " + reader.getSeriesCount());
                final IMetadata omeMeta = (IMetadata) reader.getMetadataStore();

                fileIdxToNumberOfSeries.put(iF, reader.getSeriesCount() );

                // -------------------------- SETUPS For each Series : one per timepoint and one per channel
                IntStream series = IntStream.range(0, reader.getSeriesCount());
                series.forEach(iSerie -> {
                    reader.setSeries(iSerie);

                    fileIdxToNumberOfSeriesAndTimepoints.put(iFile, new SeriesTps(reader.getSeriesCount(),omeMeta.getPixelsSizeT(iSerie).getNumberValue().intValue()));
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
                    VoxelDimensions voxDims = BioFormatsMetaDataHelper.getSeriesVoxelDimensions(omeMeta, iSerie, bfUnit);
                    // Register Setups (one per channel and one per timepoint)
                    channels.forEach(
                            iCh -> {
                                int ch_id = getChannelId(omeMeta, iSerie, iCh, reader.isRGB());
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
                                            viewSetups.add(vs);
                                            viewSetupToBFFileSerieChannel.put(viewSetupCounter, new FileSerieChannel(iFile, iSerie, iCh));
                                            viewSetupCounter++;
                                 //       });
                            });
                });
                reader.close();
            }

            // ------------------- BUILDING SPIM DATA
            ArrayList<File> inputFilesArray = new ArrayList<>();
            for (File f:inputFiles) {
                inputFilesArray.add(f);
            }
            List<TimePoint> timePoints = new ArrayList<>();
            IntStream.range(0,maxTimepoints).forEach(tp -> timePoints.add(new TimePoint(tp)));

            /*---  Creates an Opener for each file */

            List<BioFormatsBdvOpener> openers = new ArrayList<>();

            inputFilesArray.forEach(f -> {
                BioFormatsBdvOpener opener = BioFormatsBdvOpener.getOpener()
                        .file(f)
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

                opener = opener.unit(BioFormatsMetaDataHelper.getUnitFromString(unit));

                opener = opener.positionReferenceFrameLength(positionReferenceFrameLength);

                opener = opener.voxSizeReferenceFrameLength(voxSizeReferenceFrameLength);

                openers.add(opener);
            });


            final ArrayList<ViewRegistration> registrations = new ArrayList<>();

            List<ViewId> missingViews = new ArrayList<>();
            for (int iF=0;iF<inputFiles.length;iF++) {
                int iFile = iF;

                memo.setId(inputFiles[iF].getAbsolutePath());
                final IFormatReader reader = memo;

                log.accept("Number of Series : " + reader.getSeriesCount());
                final IMetadata omeMeta = (IMetadata) reader.getMetadataStore();

                int nSeries = fileIdxToNumberOfSeries.get(iF);
                // Need to set view registrations : identity ? how does that work with the one given by the image loader ?
                IntStream series = IntStream.range(0, nSeries);


                series.forEach(iSerie -> {
                    //int iS = iSerie;
                    //int nTimepoints = omeMetaOmeXml.getPixelsSizeT(iS).getNumberValue().intValue();
                    //IntStream timepoints = IntStream.range(0, nTimepoints);
                    final int nTimepoints = omeMetaOmeXml.getPixelsSizeT(iSerie).getNumberValue().intValue();

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
                                              bfUnit,
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
                reader.close();
            }

            SequenceDescription sd = new SequenceDescription( new TimePoints( timePoints ), viewSetups , null, new MissingViews(missingViews));
            sd.setImgLoader(new BioFormatsImageLoader(openers,sd));


            if (inputFiles.length==1) {
                File inputFile = inputFiles[0];
                if ((xmlFilePath==null)||(xmlFilePath.equals(""))) {
                    String outputPath = FilenameUtils.removeExtension(inputFile.getAbsolutePath())+".xml";
                    log.accept(outputPath);
                    final SpimData spimData = new SpimData( inputFile.getParentFile(), sd, new ViewRegistrations( registrations ) );
                    asd = spimData;
                    if (saveDataset) new XmlIoSpimData().save( spimData, outputPath );
                } else {
                    String outputFileName = FilenameUtils.getBaseName(inputFile.getAbsolutePath())+".xml";
                    log.accept(outputFileName);
                    final SpimData spimData = new SpimData( xmlFilePath, sd, new ViewRegistrations( registrations ) );
                    asd = spimData;
                    if (saveDataset) new XmlIoSpimData().save( spimData, new File(xmlFilePath,outputFileName).getAbsolutePath() );
                }
            } else {
                final SpimData spimData = new SpimData( xmlFilePath, sd, new ViewRegistrations( registrations ) );
                asd = spimData;
                if (saveDataset) new XmlIoSpimData().save( spimData, new File(xmlFilePath,xmlFileName).getAbsolutePath() );
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    int getChannelId(IMetadata omeMeta, int iSerie, int iChannel, boolean isRGB) {
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

}
