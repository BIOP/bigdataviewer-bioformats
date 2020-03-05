package ch.epfl.biop.bdv.bioformats.bioformatssource;

import bdv.util.volatiles.SharedQueue;
import bdv.viewer.Source;
import ch.epfl.biop.bdv.bioformats.BioFormatsMetaDataHelper;
import loci.formats.*;
import loci.formats.meta.IMetadata;
import net.imglib2.FinalInterval;
import net.imglib2.realtransform.AffineTransform3D;
import net.imglib2.type.numeric.NumericType;
import ome.units.UNITS;
import ome.units.quantity.Length;
import ome.units.unit.Unit;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.stream.Collectors;


public class BioFormatsBdvOpener {

    // For copying the object
    public BioFormatsBdvOpener copy() {
        return new BioFormatsBdvOpener(this);
    }

    public BioFormatsBdvOpener(BioFormatsBdvOpener opener) {
        dataLocation=opener.dataLocation;
        useBioFormatsXYBlockSize = opener.useBioFormatsXYBlockSize;
        cacheBlockSize = new FinalInterval(opener.cacheBlockSize);
        swZC = opener.swZC;
        splitRGBChannels=opener.splitRGBChannels;
        u = opener.u; // No deep copy
        if (opener.positionPreTransformMatrixArray!=null)
            positionPreTransformMatrixArray = opener.positionPreTransformMatrixArray.clone();
        if (opener.positionPostTransformMatrixArray!=null)
            positionPostTransformMatrixArray = opener.positionPostTransformMatrixArray.clone();
        positionReferenceFrameLength = opener.positionReferenceFrameLength; // no deep copy
        positionIgnoreBioFormatsMetaData = opener.positionIgnoreBioFormatsMetaData;
        positionIsImageCenter = opener.positionIsImageCenter;
        if (opener.voxSizePreTransformMatrixArray!=null)
            voxSizePreTransformMatrixArray = opener.voxSizePreTransformMatrixArray.clone();
        if (opener.voxSizePostTransformMatrixArray!=null)
            voxSizePostTransformMatrixArray = opener.voxSizePostTransformMatrixArray.clone();
        voxSizeReferenceFrameLength = opener.voxSizeReferenceFrameLength;
        voxSizeIgnoreBioFormatsMetaData = opener.voxSizeIgnoreBioFormatsMetaData;
        axesOfImageFlip = opener.axesOfImageFlip.clone();
        nFetcherThread = opener.nFetcherThread;
        numPriorities = opener.numPriorities;
    }

    public BioFormatsBdvOpener() {}

    public SharedQueue getCacheControl() {
        if (cc==null) {
            cc = new SharedQueue(nFetcherThread, numPriorities);
        }
        return cc;
    }

    // All serializable fields
    public String dataLocation = null; // URL or File

    public boolean useBioFormatsXYBlockSize = true; // Block size : use the one defined by BioFormats or
    public FinalInterval cacheBlockSize = new FinalInterval(new long[]{0,0,0}, new long[]{512,512,1}); // needs a default size for z

    // Channels options
    public boolean swZC; // Switch Z and Channels
    public boolean splitRGBChannels = false;

    // Unit used for display
    public Unit u;

    // Bioformats location fix
    public double[] positionPreTransformMatrixArray;
    public double[] positionPostTransformMatrixArray;
    public Length positionReferenceFrameLength;
    public boolean positionIgnoreBioFormatsMetaData = false;
    public boolean positionIsImageCenter = false; // Top left corner otherwise

    // Bioformats voxsize fix
    public double[] voxSizePreTransformMatrixArray;
    public double[] voxSizePostTransformMatrixArray;
    public Length voxSizeReferenceFrameLength;
    public boolean voxSizeIgnoreBioFormatsMetaData = false;
    public boolean[] axesOfImageFlip = new boolean[]{false, false, false};

    public int nFetcherThread = 2;

    public int numPriorities = 4;

    public int maxCacheSize = 1;

    transient SharedQueue cc = new SharedQueue(2,4);

    public String getDataLocation() {
        return dataLocation;
    }

    public BioFormatsBdvOpener with(Consumer<BioFormatsBdvOpener> builderFunction) {
        builderFunction.accept(this);
        return this;
    }

    public BioFormatsBdvOpener file(File f) {
        this.dataLocation = f.getAbsolutePath();
        return this;
    }

    public BioFormatsBdvOpener positionReferenceFrameLength(Length l) {
        this.positionReferenceFrameLength = l;
        return this;
    }

    public BioFormatsBdvOpener voxSizeReferenceFrameLength(Length l) {
        this.voxSizeReferenceFrameLength = l;
        return this;
    }

    public BioFormatsBdvOpener cacheOptions(int numFetcherThreads, int numPriorities, int maxCacheSize) {
        this.nFetcherThread = numFetcherThreads;
        this.numPriorities = numPriorities;
        this.maxCacheSize = maxCacheSize;
        this.cc = new SharedQueue(this.nFetcherThread, this.numPriorities);
        return this;
    }

    public BioFormatsBdvOpener setCache(SharedQueue sq) {
        this.cc = sq;
        return this;
    }

    public BioFormatsBdvOpener file(String filePath) {
        this.dataLocation = filePath;
        return this;
    }

    public BioFormatsBdvOpener splitRGBChannels() {
        splitRGBChannels = true;
        return this;
    }

    public BioFormatsBdvOpener flipPositionXYZ() {
        if (this.positionPreTransformMatrixArray == null) {
            positionPreTransformMatrixArray = new AffineTransform3D().getRowPackedCopy();
        }
        AffineTransform3D at3D = new AffineTransform3D();
        at3D.set(positionPreTransformMatrixArray);
        at3D.scale(-1);
        positionPreTransformMatrixArray = at3D.getRowPackedCopy();
        return this;
    }

    public BioFormatsBdvOpener flipPositionX() {
        if (this.positionPreTransformMatrixArray == null) {
            positionPreTransformMatrixArray = new AffineTransform3D().getRowPackedCopy();
        }
        AffineTransform3D at3D = new AffineTransform3D();
        at3D.set(positionPreTransformMatrixArray);
        at3D.scale(-1,1,1);
        positionPreTransformMatrixArray = at3D.getRowPackedCopy();
        return this;
    }

    public BioFormatsBdvOpener flipPositionY() {
        if (this.positionPreTransformMatrixArray == null) {
            positionPreTransformMatrixArray = new AffineTransform3D().getRowPackedCopy();
        }
        AffineTransform3D at3D = new AffineTransform3D();
        at3D.set(positionPreTransformMatrixArray);
        at3D.scale(1,-1,1);
        positionPreTransformMatrixArray = at3D.getRowPackedCopy();
        return this;
    }

    public BioFormatsBdvOpener flipPositionZ() {
        if (this.positionPreTransformMatrixArray == null) {
            positionPreTransformMatrixArray = new AffineTransform3D().getRowPackedCopy();
        }
        AffineTransform3D at3D = new AffineTransform3D();
        at3D.set(positionPreTransformMatrixArray);
        at3D.scale(1,1,-1);
        positionPreTransformMatrixArray = at3D.getRowPackedCopy();
        return this;
    }

    public BioFormatsBdvOpener setPositionPreTransform(AffineTransform3D at3d) {
        positionPreTransformMatrixArray = at3d.getRowPackedCopy();
        return this;
    }

    public BioFormatsBdvOpener setPositionPostTransform(AffineTransform3D at3d) {
        positionPostTransformMatrixArray = at3d.getRowPackedCopy();
        return this;
    }

    public BioFormatsBdvOpener auto() {
        // Special cases based on File formats are handled here
        if (this.dataLocation == null) {
            // dataLocation not set -> we can't do anything
            return this;
        }
        IFormatReader readerIdx = new ImageReader();

        readerIdx.setFlattenedResolutions(false);
        Memoizer memo = new Memoizer(readerIdx);

        final IMetadata omeMetaOmeXml = MetadataTools.createOMEXMLMetadata();
        memo.setMetadataStore(omeMetaOmeXml);

        try {
            memo.setId(dataLocation);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
        final IFormatReader reader = memo;

        System.out.println("Attempts to set opener settings for file format " + reader.getFormat());

        // Adjustements here!

        if (reader.getFormat().equals("Nikon ND2")) {
            return BioFormatsBdvOpenerFix.fixNikonND2(this);
        } else {
            return this;
        }

    }


    public BioFormatsBdvOpener url(URL url) {
        this.dataLocation = url.toString();
        return this;
    }

    public BioFormatsBdvOpener location(String location) {
        this.dataLocation = location;
        return this;
    }

    public BioFormatsBdvOpener location(File f) {
        this.dataLocation = f.getAbsolutePath();
        return this;
    }

    public BioFormatsBdvOpener unit(Unit u) {
        this.u = u;
        return this;
    }

    public BioFormatsBdvOpener unit(String u) {
        this.u = BioFormatsMetaDataHelper.getUnitFromString(u);
        return this;
    }

    public BioFormatsBdvOpener millimeter() {
        this.u = UNITS.MILLIMETER;
        return this;
    }

    public BioFormatsBdvOpener micrometer() {
        this.u = UNITS.MICROMETER;
        return this;
    }

    public BioFormatsBdvOpener nanometer() {
        this.u = UNITS.NANOMETER;
        return this;
    }

    public BioFormatsBdvOpener centerPositionConvention() {
        this.positionIsImageCenter = true;
        return this;
    }

    public BioFormatsBdvOpener cornerPositionConvention() {
        this.positionIsImageCenter = false;
        return this;
    }

    public BioFormatsBdvOpener ignoreMetadata() {
        this.positionIgnoreBioFormatsMetaData = true;
        this.voxSizeIgnoreBioFormatsMetaData = true;
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

    public IFormatReader getNewReader() {
        IFormatReader reader = new ImageReader();
        reader.setFlattenedResolutions(false);
        if (splitRGBChannels) {reader = new ChannelSeparator(reader);}
        Memoizer memo = new Memoizer(reader);
        final IMetadata omeMetaIdxOmeXml = MetadataTools.createOMEXMLMetadata();
        memo.setMetadataStore(omeMetaIdxOmeXml);
        try {
            memo.setId(dataLocation);
        } catch (FormatException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        final IFormatReader readerIdx = memo;
        return readerIdx;
    }

    IFormatReader reusableReader=null;

    public IFormatReader getCachedReader() {
        if (reusableReader==null) {
            reusableReader = getNewReader();
        }
        return reusableReader;
    }

    public BioFormatsBdvSource getConcreteSource(int image_index, int channel_index) {
        try {

            final IFormatReader readerIdx = getNewReader();

            Class<? extends BioFormatsBdvSource> c = BioFormatsBdvSource.getBioformatsBdvSourceClass(readerIdx, image_index);

            AffineTransform3D positionPreTransform = null;
            if (positionPreTransformMatrixArray!=null) {
                positionPreTransform = new AffineTransform3D();
                positionPreTransform.set(positionPreTransformMatrixArray);
            }

            AffineTransform3D positionPostTransform = null;
            if (positionPostTransformMatrixArray!=null) {
                positionPostTransform = new AffineTransform3D();
                positionPostTransform.set(positionPostTransformMatrixArray);
            }

            AffineTransform3D voxSizePreTransform = null;
            if (voxSizePreTransformMatrixArray!=null) {
                voxSizePreTransform = new AffineTransform3D();
                voxSizePreTransform.set(voxSizePreTransformMatrixArray);
            }

            AffineTransform3D voxSizePostTransform = null;
            if (voxSizePreTransformMatrixArray!=null) {
                voxSizePostTransform = new AffineTransform3D();
                voxSizePostTransform.set(voxSizePostTransformMatrixArray);
            }

            /*
            IFormatReader reader,
                               int image_index,
                               int channel_index,
                               boolean swZC,
                               FinalInterval cacheBlockSize,
                               int maxCacheSize,
                               boolean useBioFormatsXYBlockSize,
                               boolean ignoreBioFormatsLocationMetaData,
                               boolean ignoreBioFormatsVoxelSizeMetaData,
                               boolean positionIsImageCenter,
                               Length positionReferenceFrameLength,
                               Length voxSizeReferenceFrameLength,
                               Unit u,
                               AffineTransform3D positionPreTransform,
                               AffineTransform3D positionPostTransform,
                               AffineTransform3D voxSizePreTransform,
                               AffineTransform3D voxSizePostTransform,
                               boolean[] axesFlip)
             */

            BioFormatsBdvSource bdvSrc = c.getConstructor(
                    IFormatReader.class,
                    int.class,
                    int.class,
                    boolean.class,
                    FinalInterval.class,
                    int.class,
                    boolean.class,
                    boolean.class,
                    boolean.class,
                    boolean.class,
                    Length.class,
                    Length.class,
                    Unit.class,
                    AffineTransform3D.class, // public  positionPreTransform;
                    AffineTransform3D.class, // positionPostTransform;
                    AffineTransform3D.class, // public  positionPreTransform;
                    AffineTransform3D.class, // positionPostTransform;
                    boolean[].class
            ).newInstance(
                    readerIdx,
                    image_index,
                    channel_index,
                    swZC,
                    cacheBlockSize,
                    maxCacheSize,
                    useBioFormatsXYBlockSize,
                    positionIgnoreBioFormatsMetaData,
                    voxSizeIgnoreBioFormatsMetaData,
                    positionIsImageCenter,
                    positionReferenceFrameLength,
                    voxSizeReferenceFrameLength,
                    u,
                    positionPreTransform,
                    positionPostTransform,
                    voxSizePreTransform,
                    voxSizePostTransform,
                    axesOfImageFlip);
            return bdvSrc;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    public VolatileBdvSource getVolatileSource(int image_index, int channel_index) {
        BioFormatsBdvSource concreteSource = this.getConcreteSource(image_index, channel_index);
        VolatileBdvSource volatileSource = new VolatileBdvSource(concreteSource,
                BioFormatsBdvSource.getVolatileOf((NumericType) concreteSource.getType()),cc);
        return volatileSource;
    }

    public Map<String, Source> getConcreteAndVolatileSources(int image_index, int channel_index) {
        BioFormatsBdvSource concreteSource = this.getConcreteSource(image_index, channel_index);
        VolatileBdvSource volatileSource = new VolatileBdvSource(concreteSource,
                BioFormatsBdvSource.getVolatileOf((NumericType) concreteSource.getType()),
                cc);
        Map<String, Source> sources = new HashMap();
        sources.put(BioFormatsBdvSource.CONCRETE, concreteSource);
        sources.put(BioFormatsBdvSource.VOLATILE, volatileSource);
        return sources;
    }

    public List<VolatileBdvSource> getVolatileSources(String codeSerieChannel) {
        List<VolatileBdvSource> sources = BioFormatsMetaDataHelper.getListOfSeriesAndChannels(getCachedReader(), codeSerieChannel)
                .stream()
                .map(sc ->
                        sc.getRight().stream().map(
                                ch -> this.getVolatileSource(sc.getLeft(), ch)
                        ).collect(Collectors.toList())
                ).collect(Collectors.toList())
                .stream()
                .flatMap(List::stream)
                .collect(Collectors.toList());
        return sources;
    }

    public List<VolatileBdvSource> getVolatileSources() {
        return getVolatileSources("*.*");
    }

    public List<BioFormatsBdvSource> getConcreteSources() {
        return getConcreteSources("*.*");
    }

    public List<BioFormatsBdvSource> getConcreteSources(String codeSerieChannel) {
        List<BioFormatsBdvSource> sources = BioFormatsMetaDataHelper.getListOfSeriesAndChannels(getCachedReader(),codeSerieChannel)
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

}
