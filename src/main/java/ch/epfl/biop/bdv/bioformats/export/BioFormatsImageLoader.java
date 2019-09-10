package ch.epfl.biop.bdv.bioformats.export;

import bdv.ViewerImgLoader;
import bdv.cache.CacheControl;
import bdv.img.cache.VolatileGlobalCellCache;
import bdv.util.volatiles.SharedQueue;
import ch.epfl.biop.bdv.bioformats.*;
import loci.formats.*;
import loci.formats.meta.IMetadata;
import mpicbg.spim.data.generic.sequence.AbstractSequenceDescription;
import mpicbg.spim.data.sequence.*;
import net.imglib2.Dimensions;
import net.imglib2.Volatile;
import net.imglib2.cache.queue.BlockingFetchQueues;
import net.imglib2.cache.queue.FetcherThreads;
import net.imglib2.type.numeric.ARGBType;
import net.imglib2.type.numeric.NumericType;
import net.imglib2.type.numeric.integer.UnsignedByteType;
import net.imglib2.type.numeric.integer.UnsignedIntType;
import net.imglib2.type.numeric.integer.UnsignedShortType;
import net.imglib2.type.numeric.real.FloatType;
import net.imglib2.type.volatiles.*;
import net.imglib2.util.Pair;
import ome.units.UNITS;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.stream.IntStream;

//https://github.com/bigdataviewer/spimdata/tree/master/src/main/java/mpicbg/spim/data/generic
//https://github.com/bigdataviewer/spimdata/blob/master/src/main/java/mpicbg/spim/data/generic/XmlIoAbstractSpimData.java
//https://github.com/bigdataviewer/bigdataviewer-core/blob/master/src/main/java/bdv/spimdata/legacy/XmlIoSpimDataMinimalLegacy.java
//https://github.com/bigdataviewer/bigdataviewer-core/blob/master/src/main/java/bdv/spimdata/XmlIoSpimDataMinimal.java
//https://github.com/fiji/SPIM_Registration/tree/ea9302d92bf975107b48509dcd5ac62992f6ffdc/src/main/java/spim/fiji/spimdata/imgloaders


public class BioFormatsImageLoader implements ViewerImgLoader,MultiResolutionImgLoader {

    public File file;

    final AbstractSequenceDescription<?, ?, ?> sequenceDescription;

    Consumer<String> log = s -> System.out.println(s);

    Map<Integer, Pair<Integer,Integer>> viewSetupToBFSerieChannel = new HashMap<>();

    int viewSetupCounter = 0;

    Map<Integer,Supplier<NumericType>> tTypeGetter = new HashMap<>();

    Map<Integer,Supplier<Volatile>> vTypeGetter = new HashMap<>();

    public BioFormatsImageLoader(File f, final AbstractSequenceDescription<?, ?, ?> sequenceDescription) {
        this.file = f;
        this.sequenceDescription = sequenceDescription;
        IFormatReader readerIdx = new ImageReader();

        readerIdx.setFlattenedResolutions(false);
        Memoizer memo = new Memoizer( readerIdx );

        final IMetadata omeMetaOmeXml = MetadataTools.createOMEXMLMetadata();
        memo.setMetadataStore(omeMetaOmeXml);

        try {
            memo.setId( f.getAbsolutePath() );

            final IFormatReader reader = memo;

            List<ViewSetup> viewSetups = new ArrayList<>();
            log.accept("Number of Series : "+reader.getSeriesCount());
            final IMetadata omeMeta = (IMetadata) reader.getMetadataStore();

            // -------------------------- SETUPS For each Series : one per timepoint and one per channel

            IntStream series = IntStream.range(0,reader.getSeriesCount());

            series.forEach( iSerie -> {
                reader.setSeries(iSerie);
                // One serie = one Tile
                Tile tile = new Tile(iSerie);
                // ---------- Serie >
                // ---------- Serie > Timepoints
                log.accept("\t Serie "+iSerie+" Number of timesteps = "+omeMeta.getPixelsSizeT(iSerie).getNumberValue().intValue());
                // ---------- Serie > Channels
                log.accept("\t Serie "+iSerie+" Number of channels = "+omeMeta.getChannelCount(iSerie));
                //final int iS = iSerie;
                // Properties of the serie
                IntStream channels = IntStream.range(0,omeMeta.getChannelCount(iSerie));
                // Register Setups (one per channel and one per timepoint)
                channels.forEach(
                        iCh -> {
                            IntStream timepoints = IntStream.range(0,omeMeta.getChannelCount(iSerie));
                            timepoints.forEach(
                                    iTp -> {
                                        viewSetupToBFSerieChannel.put(viewSetupCounter, new Pair<Integer, Integer>() {
                                            @Override
                                            public Integer getA() {
                                                return iSerie;
                                            }

                                            @Override
                                            public Integer getB() {
                                                return iCh;
                                            }
                                        });
                                        viewSetupCounter++;
                                    });
                        });

                try {
                    BioFormatsHelper h = new BioFormatsHelper(readerIdx, iSerie);
                    if (h.is24bitsRGB) {
                        tTypeGetter.put(iSerie,() -> new ARGBType());
                        vTypeGetter.put(iSerie,() -> new VolatileARGBType());
                    } else {
                        if (h.is8bits)  {
                            tTypeGetter.put(iSerie,() -> new UnsignedByteType());
                            vTypeGetter.put(iSerie,() -> new VolatileUnsignedByteType());
                        }
                        if (h.is16bits) {
                            tTypeGetter.put(iSerie,() -> new UnsignedShortType());
                            vTypeGetter.put(iSerie,() -> new VolatileUnsignedShortType());
                        }
                        if (h.is32bits) {
                            tTypeGetter.put(iSerie,() -> new UnsignedIntType());
                            vTypeGetter.put(iSerie,() -> new VolatileUnsignedIntType());
                        }
                        if (h.isFloat32bits) {
                            tTypeGetter.put(iSerie,() -> new FloatType());
                            vTypeGetter.put(iSerie,() -> new VolatileFloatType());
                        }
                    }

                } catch (Exception e) {
                    e.printStackTrace();
                }

            });
            final BlockingFetchQueues<Callable< ? >> queue = new BlockingFetchQueues<>( 1 );
            //fetchers = new FetcherThreads( queue, 1 );
            cache = new VolatileGlobalCellCache( queue );
            reader.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    HashMap<Integer, BFViewerImgLoader> imgLoaders = new HashMap<>();

    /*@Override
    public MultiResolutionSetupImgLoader getSetupImgLoader(int setupId) {
        if (imgLoaders.containsKey(setupId)) {
            return imgLoaders.get(setupId);
        } else {
            BioFormatsSetupImageLoader imgL = new BioFormatsSetupImageLoader<>(
                    file,
                    viewSetupToBFSerieChannel.get(setupId).getA(),
                    viewSetupToBFSerieChannel.get(setupId).getB(),
                    false,
                    false,
                    true,
                    -1,
                    -1,
                    -1
            );
            imgLoaders.put(setupId,imgL);
            return imgL;
        }
    }*/

    protected VolatileGlobalCellCache cache;

    public BFViewerImgLoader getSetupImgLoader(int setupId) {
        if (imgLoaders.containsKey(setupId)) {
            return imgLoaders.get(setupId);
        } else {
            int iSerie = viewSetupToBFSerieChannel.get(setupId).getA();
            int iChannel = viewSetupToBFSerieChannel.get(setupId).getB();


            BFViewerImgLoader imgL = new BFViewerImgLoader(
                    file,
                    viewSetupToBFSerieChannel.get(setupId).getA(),
                    viewSetupToBFSerieChannel.get(setupId).getB(),
                    false,
                    false,
                    false,
                    512,
                    512,
                    1,
                    tTypeGetter.get(iSerie),
                    vTypeGetter.get(iSerie)
            );
            imgLoaders.put(setupId,imgL);
            return imgL;
        }
    }

    @Override
    public CacheControl getCacheControl() {
        return cache;
    }
}
