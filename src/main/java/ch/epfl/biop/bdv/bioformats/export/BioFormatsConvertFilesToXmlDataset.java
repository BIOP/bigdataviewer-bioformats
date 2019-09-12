package ch.epfl.biop.bdv.bioformats.export;

//https://github.com/PreibischLab/multiview-reconstruction/blob/master/src/main/java/net/preibisch/mvrecon/fiji/datasetmanager/FileListDatasetDefinition.java

//https://github.com/PreibischLab/multiview-reconstruction/blob/master/src/main/java/net/preibisch/mvrecon/fiji/datasetmanager/FileListDatasetDefinition.java

import loci.formats.*;
import loci.formats.meta.IMetadata;
import mpicbg.spim.data.SpimData;
import mpicbg.spim.data.XmlIoSpimData;
import mpicbg.spim.data.registration.ViewRegistration;
import mpicbg.spim.data.registration.ViewRegistrations;
import mpicbg.spim.data.sequence.*;
import net.imglib2.Dimensions;
import net.imglib2.realtransform.AffineTransform3D;
import ome.units.UNITS;
import org.apache.commons.io.FilenameUtils;
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

import static ch.epfl.biop.bdv.bioformats.export.BioFormatsToXmlUtils.getChannelHashFromBFMeta;

/**
 * Converting BioFormats structure into an Xml Dataset, compatible for BigDataViewer and FIJI BIG Plugins
 * Limitation
 * Series are considered as Tiles, no Illumination or Angle is considered
 *
 * @author nicolas.chiaruttini@epfl.ch
 */

@Plugin(type = Command.class,menuPath = "Plugins>BigDataViewer>SciJava>Save as Xml Dataset (SciJava)")
public class BioFormatsConvertFilesToXmlDataset implements Command {
    @Parameter(label = "Image Files")
    public File[] inputFiles;

    @Parameter(required=false, label = "output path, empty = same folder as input", style = "directory") // To append datasets potentially
    public File xmlFilePath;

    @Parameter(required=false, label = "output file name") // To append datasets potentially
    public String xmlFileName;

    Consumer<String> log = s -> System.out.println(s);

    int viewSetupCounter = 0;

    int nTileCounter = 0;

    int maxTimepoints = -1;

    Map<Integer,Integer> channelHashToId = new HashMap<>();

    Map<Integer,Integer> fileIdxToNumberOfSeries = new HashMap<>();
    Map<Integer,Channel> channelIdToChannel = new HashMap<>();

    Map<Integer, SeriesTps> fileIdxToNumberOfSeriesAndTimepoints = new HashMap<>();
    Map<Integer, FileSerieChannel> viewSetupToBFFileSerieChannel = new HashMap<>();

    @Override
    public void run() {
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
                    Dimensions dims = BioFormatsToXmlUtils.getDimensions(omeMeta, iSerie, UNITS.MILLIMETER);
                    VoxelDimensions voxDims = BioFormatsToXmlUtils.getVoxelDimensions(omeMeta, iSerie, UNITS.MILLIMETER);
                    // Register Setups (one per channel and one per timepoint)
                    channels.forEach(
                            iCh -> {
                                int ch_id = getChannelId(omeMeta, iSerie, iCh);
                                String channelName = omeMeta.getChannelName(iSerie, iCh);
                                IntStream timepoints = IntStream.range(0, omeMeta.getPixelsSizeT(iSerie).getNumberValue().intValue());
                                timepoints.forEach(
                                        iTp -> {
                                            String setupName = imageName
                                                    + "-" + channelName + ":" + iTp;
                                            System.out.println(setupName);
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
                                        });
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
            SequenceDescription sd = new SequenceDescription( new TimePoints( timePoints ), viewSetups , new BioFormatsImageLoader(inputFilesArray,null), null);

            final ArrayList<ViewRegistration> registrations = new ArrayList<>();

            for (int iF=0;iF<inputFiles.length;iF++) {
                int iFile = iF;
                int nSeries = fileIdxToNumberOfSeries.get(iF);
                // Need to set view registrations : identity ? how does that work with the one given by the image loader ?
                IntStream series = IntStream.range(0, nSeries);
                series.forEach(iSerie -> {
                    int iS = iSerie;
                    IntStream timepoints = IntStream.range(0, omeMetaOmeXml.getPixelsSizeT(iS).getNumberValue().intValue());
                    timePoints.forEach(iTp -> {
                        viewSetupToBFFileSerieChannel
                                .keySet()
                                .stream()
                                .filter(viewSetupId -> (viewSetupToBFFileSerieChannel.get(viewSetupId).iFile == iFile))
                                .filter(viewSetupId -> (viewSetupToBFFileSerieChannel.get(viewSetupId).iSerie == iSerie))
                                .forEach(viewSetupId -> registrations.add(new ViewRegistration(iTp.getId(), viewSetupId, new AffineTransform3D()))//BioFormatsToXmlUtils.getRootTransform(omeMeta,iSerie,UNITS.MILLIMETER)
                                );
                    });
                });
            }

            if (inputFiles.length==1) {
                File inputFile = inputFiles[0];
                if ((xmlFilePath==null)||(xmlFilePath.equals(""))) {
                    String outputPath = FilenameUtils.removeExtension(inputFile.getAbsolutePath())+".xml";
                    System.out.println(outputPath);
                    final SpimData spimData = new SpimData( inputFile.getParentFile(), sd, new ViewRegistrations( registrations ) );
                    new XmlIoSpimData().save( spimData, outputPath );
                } else {
                    String outputFileName = FilenameUtils.getBaseName(inputFile.getAbsolutePath())+".xml";
                    System.out.println(outputFileName);
                    final SpimData spimData = new SpimData( xmlFilePath, sd, new ViewRegistrations( registrations ) );
                    new XmlIoSpimData().save( spimData, new File(xmlFilePath,outputFileName).getAbsolutePath() );
                }
            } else {
                final SpimData spimData = new SpimData( xmlFilePath, sd, new ViewRegistrations( registrations ) );
                new XmlIoSpimData().save( spimData, new File(xmlFilePath,xmlFileName).getAbsolutePath() );
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    int channelCounter = 0;

    int getChannelId(IMetadata omeMeta, int iSerie, int iChannel) {
        int channelHash = getChannelHashFromBFMeta(omeMeta, iSerie, iChannel);
        if (!channelHashToId.containsKey(channelHash)) {
            // No : add it in the channel hashmap
            channelHashToId.put(channelHash,channelCounter);
            log.accept(" \t \t \t New Channel, set as number "+channelCounter);
            channelIdToChannel.put(channelCounter, new Channel(channelCounter));
            channelCounter++;
        }
        int idChannel = channelHashToId.get(channelHash);
        return idChannel;
    }

}
