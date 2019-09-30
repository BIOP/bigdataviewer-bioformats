package ch.epfl.biop.bdv.bioformats;

import bdv.util.BdvHandle;
import loci.common.DebugTools;
import loci.formats.IFormatReader;
import loci.formats.ImageReader;
import loci.formats.Memoizer;
import loci.formats.MetadataTools;
import loci.formats.meta.IMetadata;
import net.imagej.ImageJ;
import org.apache.commons.lang3.tuple.MutablePair;
import org.apache.commons.lang3.tuple.Pair;
import org.scijava.ItemIO;
import org.scijava.command.Command;
import org.scijava.command.CommandModule;
import org.scijava.command.CommandService;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;

import java.io.File;
import java.util.ArrayList;
import java.util.concurrent.Future;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.logging.Logger;

// TODO : add lookuptable

@Plugin(type = Command.class,menuPath = "BDV_SciJava>Open>Open with BioFormats in Bdv")
public class BioFormatsOpenPlugInSciJava implements Command
{
    private static final Logger LOGGER = Logger.getLogger( BioFormatsOpenPlugInSciJava.class.getName() );

    @Parameter(label = "Image File")
    public File inputFile;

    @Parameter(label = "Open in new BigDataViewer window")
    public boolean createNewWindow;

    // ItemIO.BOTH required because it can be modified in case of appending new data to BDV (-> requires INPUT), or created (-> requires OUTPUT)
    @Parameter(label = "BigDataViewer Frame", type = ItemIO.BOTH, required = false)
    public BdvHandle bdv_h;

    @Parameter(label="Series and channel, * for everything")
    public String sourceIndexStringNewFull = "0";

    @Parameter(label="Display type ()", choices = {"Volatile","Standard", "Volatile + Standard"})
    public String appendMode = "Volatile";

    @Parameter
    public boolean autoscale = true;

    @Parameter
    public boolean switchZandC = false;

    @Parameter
    public boolean keepBdv3d = false;

    @Parameter
    public boolean ignoreMetadata = true;

    @Parameter(choices = {"Millimeters", "Microns"})
    public String unit;

    @Parameter
    public CommandService cs;

    @Parameter
    public boolean letBioFormatDecideCacheBlockXY = true;

    @Parameter
    public int cacheBlockSizeX = 512;

    @Parameter
    public int cacheBlockSizeY = 512;

    @Parameter
    public int cacheBlockSizeZ = 32;

    @Override
    public void run()
    {
        //DebugTools.enableIJLogging(false);
        DebugTools.enableLogging("INFO");
        try {

            IFormatReader readerIdx = new ImageReader();

            readerIdx.setFlattenedResolutions(false);
            Memoizer memo = new Memoizer( readerIdx );

            final IMetadata omeMetaOmeXml = MetadataTools.createOMEXMLMetadata();
            memo.setMetadataStore(omeMetaOmeXml);

            memo.setId( inputFile.getAbsolutePath() );
            final IFormatReader reader = memo;

            LOGGER.info("reader.getSeriesCount()="+reader.getSeriesCount());

            ArrayList<Pair<Integer, ArrayList<Integer>>>
                listOfSources =
                    commaSeparatedListToArrayOfArray(
                        sourceIndexStringNewFull,
                        idxSeries ->(idxSeries>=0)?idxSeries:reader.getSeriesCount()+idxSeries, // apparently -1 is necessary -> I don't really understand
                        (idxSeries, idxChannel) ->
                                (idxChannel>=0)?idxChannel:omeMetaOmeXml.getChannelCount(idxSeries)+idxChannel
                    );

            listOfSources.stream().forEach(p -> {
                p.getRight().stream().forEach(idCh -> {
                    try {

                        LOGGER.info("omeMetaOmeXml.getChannelCount("+p.getLeft()+")="+omeMetaOmeXml.getChannelCount(p.getLeft()));
                        CommandModule cm;
                        if (!appendMode.equals("Volatile + Standard")) {
                            Future<CommandModule> module = cs.run(BioFormatsOpenPlugInSingleSourceSciJava.class, false,
                                    "sourceIndex", p.getLeft(),
                                    "channelIndex", idCh,
                                    "bdv_h", bdv_h,
                                    "createNewWindow", createNewWindow,
                                    "inputFile", inputFile,
                                    "switchZandC", switchZandC,
                                    "autoscale", autoscale,
                                    "appendMode", appendMode,
                                    "keepBdv3d", keepBdv3d,
                                    "cacheBlockSizeX", cacheBlockSizeX,
                                    "cacheBlockSizeY", cacheBlockSizeY,
                                    "cacheBlockSizeZ", cacheBlockSizeZ,
                                    "letBioFormatDecideCacheBlockXY", letBioFormatDecideCacheBlockXY,
                                    "ignoreMetadata", ignoreMetadata,
                                    "unit", unit

                            );

                            final BioFormatsOpenPlugInSingleSourceSciJava command = new BioFormatsOpenPlugInSingleSourceSciJava();
                            command.bdv_h = bdv_h;
                            command.run();

                            cm = module.get();
                        } else {
                            Future<CommandModule> module = cs.run(BioFormatsOpenPlugInSingleSourceSciJava.class, false,
                                    "sourceIndex", p.getLeft(),
                                    "channelIndex", idCh,
                                    "bdv_h", bdv_h,
                                    "createNewWindow", createNewWindow,
                                    "inputFile", inputFile,
                                    "switchZandC", switchZandC,
                                    "autoscale", autoscale,
                                    "appendMode", "Volatile",
                                    "keepBdv3d", keepBdv3d,
                                    "cacheBlockSizeX", cacheBlockSizeX,
                                    "cacheBlockSizeY", cacheBlockSizeY,
                                    "cacheBlockSizeZ", cacheBlockSizeZ,
                                    "letBioFormatDecideCacheBlockXY", letBioFormatDecideCacheBlockXY,
                                    "ignoreMetadata", ignoreMetadata,
                                    "unit", unit
                            );
                            module.get();
                            module = cs.run(BioFormatsOpenPlugInSingleSourceSciJava.class, false,
                                    "sourceIndex", p.getLeft(),
                                    "channelIndex", idCh,
                                    "bdv_h", bdv_h,
                                    "createNewWindow", createNewWindow,
                                    "inputFile", inputFile,
                                    "switchZandC", switchZandC,
                                    "autoscale", autoscale,
                                    "appendMode", "Standard",
                                    "keepBdv3d", keepBdv3d,
                                    "cacheBlockSizeX", cacheBlockSizeX,
                                    "cacheBlockSizeY", cacheBlockSizeY,
                                    "cacheBlockSizeZ", cacheBlockSizeZ,
                                    "letBioFormatDecideCacheBlockXY", letBioFormatDecideCacheBlockXY,
                                    "ignoreMetadata", ignoreMetadata,
                                    "unit", unit
                            );
                            cm = module.get();

                        }
                        bdv_h = (BdvHandle) cm.getOutput("bdv_h");

                        createNewWindow = false;

                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                });
            });

        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    /**
     * BiFunction necessary to be able to find index of negative values
     */
    static public ArrayList<Pair<Integer, ArrayList<Integer>>> commaSeparatedListToArrayOfArray(String expression, Function<Integer, Integer> fbounds, BiFunction<Integer, Integer, Integer> f) {
        String[] splitIndexes = expression.split(";");

        ArrayList<Pair<Integer, ArrayList<Integer>>> arrayOfArrayOfIndexes = new ArrayList<>();

        for (String str : splitIndexes) {
            str.trim();
            String seriesIdentifier = str;
            String channelIdentifier = "*";
            if (str.contains(".")) {
                String[] boundIndex = str.split("\\.");
                if (boundIndex.length==2) {
                    seriesIdentifier = boundIndex[0];
                    channelIdentifier = boundIndex[1];
                } else {
                    LOGGER.warning("Number format problem with expression:"+str+" - Expression ignored");
                    break;
                }
            }
            // TODO Need to split by comma
                // No sub array specifier -> equivalent to * in subchannel
                try {
                    if (seriesIdentifier.trim().equals("*")) {
                        int maxIndex = fbounds.apply(-1);
                        System.out.println("maxIndex="+maxIndex);
                        for (int index = 0; index <=maxIndex; index++) {
                            MutablePair<Integer, ArrayList<Integer>> current = new MutablePair<>();
                            final int idxCp = index;
                            current.setLeft(idxCp);
                            current.setRight(
                                    expressionToArray(channelIdentifier, i -> f.apply(idxCp,i))
                            );
                            arrayOfArrayOfIndexes.add(current);
                        }
                    } else {
                        int indexMin, indexMax;

                        if (seriesIdentifier.trim().contains(":")) {
                            String[] boundIndex = seriesIdentifier.split(":");
                            assert boundIndex.length==2;
                            indexMin = fbounds.apply(Integer.valueOf(boundIndex[0].trim()));
                            indexMax = fbounds.apply(Integer.valueOf(boundIndex[1].trim()));
                        } else {
                            indexMin = fbounds.apply(Integer.valueOf(seriesIdentifier.trim()));
                            indexMax = indexMin;
                        }
                        if (indexMax>=indexMin) {
                            for (int index=indexMin;index<=indexMax;index++) {
                                MutablePair<Integer, ArrayList<Integer>> current = new MutablePair<>();
                                final int idxCp = index;
                                current.setLeft(index);
                                current.setRight(
                                        expressionToArray(channelIdentifier, i -> f.apply(idxCp,i))
                                );
                                arrayOfArrayOfIndexes.add(current);
                            }
                        } else {
                            for (int index=indexMax;index>=indexMin;index--) {
                                MutablePair<Integer, ArrayList<Integer>> current = new MutablePair<>();
                                final int idxCp = index;
                                current.setLeft(index);
                                current.setRight(
                                        expressionToArray(channelIdentifier, i -> f.apply(idxCp,i))
                                );
                                arrayOfArrayOfIndexes.add(current);
                            }
                        }

                    }
                } catch (NumberFormatException e) {
                    LOGGER.warning("Number format problem with expression:"+str+" - Expression ignored");
                }

        }
        return arrayOfArrayOfIndexes;
    }

    /**
     * Convert a comma separated list of indexes into an arraylist of integer
     *
     * For instance 1,2,5:7,10:12,14 returns an ArrayList containing
     * [1,2,5,6,7,10,11,12,14]
     *
     * Invalid format are ignored and an error message is displayed
     *
     * @param expression
     * @return list of indexes in ArrayList
     */

    static public ArrayList<Integer> expressionToArray(String expression, Function<Integer, Integer> fbounds) {
        String[] splitIndexes = expression.split(",");
        ArrayList<Integer> arrayOfIndexes = new ArrayList<>();
        for (String str : splitIndexes) {
            str.trim();
            if (str.contains(":")) {
                // Array of source, like 2:5 = 2,3,4,5
                String[] boundIndex = str.split(":");
                if (boundIndex.length==2) {
                    try {
                        int b1 = fbounds.apply(Integer.valueOf(boundIndex[0].trim()));
                        int b2 = fbounds.apply(Integer.valueOf(boundIndex[1].trim()));
                        if (b1<b2) {
                            for (int index = b1; index <= b2; index++) {
                                arrayOfIndexes.add(index);
                            }
                        }  else {
                            for (int index = b2; index >= b1; index--) {
                                arrayOfIndexes.add(index);
                            }
                        }
                    } catch (NumberFormatException e) {
                        LOGGER.warning("Number format problem with expression:"+str+" - Expression ignored");
                    }
                } else {
                    LOGGER.warning("Cannot parse expression "+str+" to pattern 'begin-end' (2-5) for instance, omitted");
                }
            } else {
                // Single source
                try {
                    if (str.trim().equals("*")) {
                        int maxIndex = fbounds.apply(-1);
                        for (int index = 0; index <=maxIndex; index++) {
                            arrayOfIndexes.add(index);
                        }
                    } else {
                        int index = fbounds.apply(Integer.valueOf(str.trim()));
                        arrayOfIndexes.add(index);
                    }
                } catch (NumberFormatException e) {
                    LOGGER.warning("Number format problem with expression:"+str+" - Expression ignored");
                }
            }
        }
        return arrayOfIndexes;
    }

    public static void main(String... args) {
        // Arrange
        //  create the ImageJ application context with all available services
        final ImageJ ij = new ImageJ();
        ij.ui().showUI();
        ij.command().run(BioFormatsOpenPlugInSciJava.class, true);

    }
}
