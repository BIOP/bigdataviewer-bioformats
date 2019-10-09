package ch.epfl.biop.bdv.bioformats.bioformatssource;

import bdv.util.volatiles.SharedQueue;
import bdv.viewer.Source;
import ch.epfl.biop.bdv.bioformats.BioFormatsMetaDataHelper;
import loci.formats.IFormatReader;
import loci.formats.ImageReader;
import loci.formats.Memoizer;
import loci.formats.MetadataTools;
import loci.formats.meta.IMetadata;
import net.imglib2.FinalInterval;
import net.imglib2.type.numeric.NumericType;
import ome.units.UNITS;
import ome.units.unit.Unit;

import java.io.File;
import java.net.URL;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.stream.Collectors;


public class BioFormatsBdvOpener {
    //public File inputFile;
    public String dataLocation=null; // URL or File
    public boolean swZC;
    public FinalInterval cacheBlockSize;
    public boolean useBioFormatsXYBlockSize = true;
    public boolean ignoreBioFormatsLocationMetaData = false;
    public boolean ignoreBioFormatsVoxelSizeMetaData = false;
    public boolean positionConventionIsCenter = false;
    public Unit u;

    public String getDataLocation() {
        return dataLocation;
    }

    public BioFormatsBdvOpener with(Consumer<BioFormatsBdvOpener> builderFunction) {
        builderFunction.accept(this);
        return this;
    }

    public BioFormatsBdvOpener file(File f) {
        this.dataLocation=f.getAbsolutePath();
        return this;
    }

    public BioFormatsBdvOpener auto() {
        // Special cases based on File formats are handled here
        if (this.dataLocation==null) {
            // dataLocation not set -> we can't do anything
            return this;
        }
        IFormatReader readerIdx = new ImageReader();

        readerIdx.setFlattenedResolutions(false);
        Memoizer memo = new Memoizer( readerIdx );

        final IMetadata omeMetaOmeXml = MetadataTools.createOMEXMLMetadata();
        memo.setMetadataStore(omeMetaOmeXml);

        try {
            memo.setId( dataLocation );
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
        final IFormatReader reader = memo;

        System.out.println("Attempts to set opener settings for file format "+reader.getFormat());

        // Adjustements here! Especially center convention

        return this;
    }

    public BioFormatsBdvOpener url(URL url) {
        this.dataLocation=url.toString();
        return this;
    }

    public BioFormatsBdvOpener location(String location) {
        this.dataLocation=location;
        return this;
    }

    public BioFormatsBdvOpener unit(Unit u) {
        this.u=u;
        return this;
    }

    public BioFormatsBdvOpener millimeter() {
        this.u= UNITS.MILLIMETER;
        return this;
    }

    public BioFormatsBdvOpener micron() {
        this.u= UNITS.MICROMETER;
        return this;
    }

    public BioFormatsBdvOpener centerPositionConvention() {
        this.positionConventionIsCenter = true;
        return this;
    }

    public BioFormatsBdvOpener cornerPositionConvention() {
        this.positionConventionIsCenter = false;
        return this;
    }

    public BioFormatsBdvOpener ignoreMetadata() {
        this.ignoreBioFormatsLocationMetaData=true;
        this.ignoreBioFormatsVoxelSizeMetaData=true;
        return this;
    }

    public BioFormatsBdvOpener useCacheBlockSizeFromBioFormats(boolean flag) {
        useBioFormatsXYBlockSize = flag;
        return this;
    }

    public BioFormatsBdvOpener switchZandC(boolean flag) {
        this.swZC = flag;
        return this;
    }

    public BioFormatsBdvOpener cacheBlockSize(int sx, int sy, int sz) {
        useBioFormatsXYBlockSize = false;
        cacheBlockSize = new FinalInterval(sx, sy, sz);
        return this;
    }

    public BioFormatsBdvSource getConcreteSource(int image_index, int channel_index) {
        try {

            System.out.println("Serie:\t"+image_index+"\t Channel:\t"+channel_index);
            IFormatReader reader = new ImageReader();
            reader.setFlattenedResolutions(false);
            Memoizer memo = new Memoizer( reader );
            final IMetadata omeMetaIdxOmeXml = MetadataTools.createOMEXMLMetadata();
            memo.setMetadataStore(omeMetaIdxOmeXml);
            memo.setId( dataLocation );
            final IFormatReader readerIdx = memo;

            Class<? extends BioFormatsBdvSource> c = BioFormatsBdvSource.getBioformatsBdvSourceClass(readerIdx, image_index);

            BioFormatsBdvSource bdvSrc = c.getConstructor(
                    IFormatReader.class,
                    int.class,
                    int.class,
                    boolean.class,
                    FinalInterval.class,
                    boolean.class,
                    boolean.class,
                    boolean.class,
                    boolean.class,
                    Unit.class
            ).newInstance(
                    readerIdx,
                    image_index,
                    channel_index,
                    swZC,
                    cacheBlockSize,
                    useBioFormatsXYBlockSize,
                    ignoreBioFormatsLocationMetaData,
                    ignoreBioFormatsVoxelSizeMetaData,
                    positionConventionIsCenter,
                    u);
            return bdvSrc;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    public VolatileBdvSource getVolatileSource(int image_index, int channel_index) {
        BioFormatsBdvSource concreteSource = this.getConcreteSource(image_index, channel_index);
        VolatileBdvSource volatileSource = new VolatileBdvSource(concreteSource,
                BioFormatsBdvSource.getVolatileOf((NumericType)concreteSource.getType()),
                new SharedQueue(1));
        return volatileSource;
    }

    public Map<String, Source> getConcreteAndVolatileSources(int image_index, int channel_index) {
        BioFormatsBdvSource concreteSource = this.getConcreteSource(image_index, channel_index);
        VolatileBdvSource volatileSource = new VolatileBdvSource(concreteSource,
                BioFormatsBdvSource.getVolatileOf((NumericType)concreteSource.getType()),
                new SharedQueue(1));
        Map<String, Source> sources = new HashMap();
        sources.put(BioFormatsBdvSource.CONCRETE,concreteSource);
        sources.put(BioFormatsBdvSource.VOLATILE,volatileSource);
        return sources;
    }

    public List<VolatileBdvSource> getVolatileSources(String codeSerieChannel) {
        List<VolatileBdvSource> sources = BioFormatsMetaDataHelper.getListOfSeriesAndChannels(dataLocation,codeSerieChannel)
                .stream()
                .map(sc ->
                        sc.getRight().stream().map(
                                ch -> this.getVolatileSource(sc.getLeft(),ch)
                        ).collect(Collectors.toList())
                ).collect(Collectors.toList())
                .stream()
                .flatMap(List::stream)
                .collect(Collectors.toList());
        return sources;
    }

    public List<BioFormatsBdvSource> getConcreteSources(String codeSerieChannel) {
        List<BioFormatsBdvSource> sources = BioFormatsMetaDataHelper.getListOfSeriesAndChannels(dataLocation,codeSerieChannel)
                .stream()
                .map(sc ->
                        sc.getRight().stream().map(
                                ch -> (BioFormatsBdvSource) this.getConcreteSource(sc.getLeft(),ch)
                        ).collect(Collectors.toList())
                ).collect(Collectors.toList())
                .stream()
                .flatMap(List::stream)
                .collect(Collectors.toList());
        return sources;
    }

    public static BioFormatsBdvOpener getOpener() {
        BioFormatsBdvOpener opener = new BioFormatsBdvOpener()
                .millimeter()
                .useCacheBlockSizeFromBioFormats(true);
        return opener;
    }

    /**
     * Does this method exits somewhere else ??
     * @param t
     * @return
     */
}
