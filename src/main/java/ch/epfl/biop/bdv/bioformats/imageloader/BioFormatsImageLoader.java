package ch.epfl.biop.bdv.bioformats.imageloader;

import bdv.ViewerImgLoader;
import bdv.cache.CacheControl;
import bdv.img.cache.VolatileGlobalCellCache;
import ch.epfl.biop.bdv.bioformats.bioformatssource.BioFormatsBdvOpener;
import ch.epfl.biop.bdv.bioformats.bioformatssource.BioFormatsBdvSource;
import loci.formats.*;
import loci.formats.meta.IMetadata;
import mpicbg.spim.data.generic.sequence.AbstractSequenceDescription;
import mpicbg.spim.data.sequence.*;
import net.imglib2.Volatile;
import net.imglib2.cache.queue.BlockingFetchQueues;
import net.imglib2.type.Type;
import net.imglib2.type.numeric.NumericType;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.function.Consumer;
import java.util.stream.IntStream;

public class BioFormatsImageLoader implements ViewerImgLoader,MultiResolutionImgLoader {

    public List<BioFormatsBdvOpener> openers;

    final AbstractSequenceDescription<?, ?, ?> sequenceDescription;

    Consumer<String> log = s -> {};//System.out.println(s);

    Map<Integer, FileSerieChannel> viewSetupToBFFileSerieChannel = new HashMap<>();

    int viewSetupCounter = 0;

    Map<Integer,Map<Integer,NumericType>> tTypeGetter = new HashMap<>();

    Map<Integer,Map<Integer,Volatile>> vTypeGetter = new HashMap<>();

    HashMap<Integer, BioFormatsSetupLoader> imgLoaders = new HashMap<>();

    protected VolatileGlobalCellCache cache;

    public BioFormatsImageLoader(List<BioFormatsBdvOpener> openers, final AbstractSequenceDescription<?, ?, ?> sequenceDescription) {
        this.openers = openers;
        this.sequenceDescription = sequenceDescription;
        IFormatReader readerIdx = new ImageReader();

        readerIdx.setFlattenedResolutions(false);
        Memoizer memo = new Memoizer( readerIdx );

        final IMetadata omeMetaOmeXml = MetadataTools.createOMEXMLMetadata();
        memo.setMetadataStore(omeMetaOmeXml);

        IntStream openersIdxStream = IntStream.range(0, openers.size());
        if ((sequenceDescription!=null)) {
            openersIdxStream.forEach(idxOpener -> {
                try {
                    BioFormatsBdvOpener opener = openers.get(idxOpener);
                    memo.setId(opener.getDataLocation());

                    tTypeGetter.put(idxOpener,new HashMap<>());
                    vTypeGetter.put(idxOpener,new HashMap<>());

                    final IFormatReader reader = memo;

                    log.accept("Number of Series : " + reader.getSeriesCount());
                    final IMetadata omeMeta = (IMetadata) reader.getMetadataStore();

                    // -------------------------- SETUPS For each Series : one per timepoint and one per channel

                    IntStream series = IntStream.range(0, reader.getSeriesCount());

                    series.forEach(iSerie -> {
                        reader.setSeries(iSerie);
                        // One serie = one Tile
                        // ---------- Serie >
                        // ---------- Serie > Timepoints
                        log.accept("\t Serie " + iSerie + " Number of timesteps = " + omeMeta.getPixelsSizeT(iSerie).getNumberValue().intValue());
                        // ---------- Serie > Channels
                        log.accept("\t Serie " + iSerie + " Number of channels = " + omeMeta.getChannelCount(iSerie));
                        // Properties of the serie
                        IntStream channels = IntStream.range(0, omeMeta.getChannelCount(iSerie));
                        // Register Setups (one per channel and one per timepoint)
                        channels.forEach(
                                iCh -> {
                                    FileSerieChannel fsc = new FileSerieChannel(idxOpener, iSerie, iCh);
                                    viewSetupToBFFileSerieChannel.put(viewSetupCounter,fsc);
                                    viewSetupCounter++;
                                });
                        Type t = BioFormatsBdvSource.getBioformatsBdvSourceType(reader, iSerie);
                        tTypeGetter.get(idxOpener).put(iSerie,(NumericType)t);
                        Volatile v = BioFormatsBdvSource.getVolatileOf((NumericType)t);
                        vTypeGetter.get(idxOpener).put(iSerie, v);
                    });
                    reader.close();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            });
        }
        // NOT CORRECTLY IMPLEMENTED YET
        final BlockingFetchQueues<Callable<?>> queue = new BlockingFetchQueues<>(1,1);
        cache = new VolatileGlobalCellCache(queue);
    }

    public BioFormatsSetupLoader getSetupImgLoader(int setupId) {
        if (imgLoaders.containsKey(setupId)) {
            return imgLoaders.get(setupId);
        } else {
            int iF = viewSetupToBFFileSerieChannel.get(setupId).iFile;
            int iS = viewSetupToBFFileSerieChannel.get(setupId).iSerie;
            int iC = viewSetupToBFFileSerieChannel.get(setupId).iChannel;
            log.accept("loading file number = "+iF+" setupId = "+setupId);

            BioFormatsSetupLoader imgL = new BioFormatsSetupLoader(
                    openers.get(iF),
                    iS,
                    iC,
                    tTypeGetter.get(iF).get(iS),
                    vTypeGetter.get(iF).get(iS)
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
