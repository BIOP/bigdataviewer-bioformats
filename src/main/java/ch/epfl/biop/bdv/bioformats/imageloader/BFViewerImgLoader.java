package ch.epfl.biop.bdv.bioformats.imageloader;

import bdv.AbstractViewerSetupImgLoader;
import bdv.viewer.Source;
import ch.epfl.biop.bdv.bioformats.bioformatssource.BioFormatsBdvSource;
import mpicbg.spim.data.generic.sequence.ImgLoaderHint;
import mpicbg.spim.data.sequence.MultiResolutionSetupImgLoader;
import mpicbg.spim.data.sequence.VoxelDimensions;
import net.imglib2.*;
import net.imglib2.cache.img.DiskCachedCellImg;
import net.imglib2.converter.Converter;
import net.imglib2.converter.Converters;
import net.imglib2.img.Img;
import net.imglib2.img.ImgFactory;
import net.imglib2.img.array.ArrayImgFactory;
import net.imglib2.realtransform.AffineTransform3D;
import net.imglib2.type.numeric.ARGBType;
import net.imglib2.type.numeric.NumericType;
import net.imglib2.type.numeric.integer.AbstractIntegerType;
import net.imglib2.type.numeric.real.FloatType;
import ome.units.UNITS;
import ome.units.unit.Unit;

import java.io.File;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

public class BFViewerImgLoader<T extends NumericType<T>,V extends Volatile<T> & NumericType<V>> extends AbstractViewerSetupImgLoader<T, V> implements MultiResolutionSetupImgLoader< T > {

    Source<T> bdvSrc;

    Source<V> vSrc;

    int[] cellDimensions;

    Function<RandomAccessibleInterval<T>, RandomAccessibleInterval<FloatType>> cvtRaiToFloatRai;

    final Converter<T,FloatType> cvt;

    Consumer<String> errlog = s -> System.err.println(BFViewerImgLoader.class+" error:"+s);

    public BFViewerImgLoader(File inputFile,
                             int sourceIndex,
                             int channelIndex,
                             boolean switchZandC,
                             boolean autoscale,
                             boolean letBioFormatDecideCacheBlockXY,
                             int cacheBlockSizeX,
                             int cacheBlockSizeY,
                             int cacheBlockSizeZ,
                             T t,
                             V v) {
        super(t, v);

        Map<String, Source> sources = (new BioFormatsBdvSource.Opener())
                .file(inputFile)
                .cacheBlockSize(cacheBlockSizeX,cacheBlockSizeY,cacheBlockSizeZ)
                .useCacheBlockSizeFromBioFormats(letBioFormatDecideCacheBlockXY)
                .switchZandC(switchZandC)
                .ignoreMetadata()
                .unit(UNITS.MILLIMETER)
                .getConcreteAndVolatileSources(sourceIndex, channelIndex);

        bdvSrc = (Source<T>) sources.get(BioFormatsBdvSource.CONCRETE);
        vSrc = (Source<V>) sources.get(BioFormatsBdvSource.VOLATILE);

        cellDimensions = ((BioFormatsBdvSource) bdvSrc).cellDimensions;

        //T t = getT.get();

        if (t instanceof FloatType) {
            cvt = null;
            cvtRaiToFloatRai = rai -> (RandomAccessibleInterval<FloatType>) rai; // Nothing to be done
        }else if (t instanceof ARGBType) {
            // Average of RGB value
            cvt = (input, output) -> {
                int val = ((ARGBType) input).get();
                int r = ARGBType.red(val);
                int g = ARGBType.green(val);
                int b = ARGBType.blue(val);
                output.set(r+g+b);
            };
            cvtRaiToFloatRai = rai -> Converters.convert( rai, cvt, new FloatType());
        }else if (t instanceof AbstractIntegerType) {
            cvt = (input, output) -> output.set(((AbstractIntegerType) input).getRealFloat());
            cvtRaiToFloatRai = rai -> Converters.convert( rai, cvt, new FloatType());
        }else {
            cvt = null;
            cvtRaiToFloatRai = e -> {
                errlog.accept("Conversion of "+t.getClass()+" to FloatType unsupported.");
                return null;
            };
        }
    }

    @Override
    public RandomAccessibleInterval<V> getVolatileImage(int timepointId, int level, ImgLoaderHint... hints) {
        return vSrc.getSource(timepointId,level);
    }

    @Override
    public RandomAccessibleInterval<FloatType> getFloatImage(int timepointId, int level, boolean normalize, ImgLoaderHint... hints) {
        return cvtRaiToFloatRai.apply(getImage(timepointId,level));
    }

    @Override
    public Dimensions getImageSize(int timepointId, int level) {
        return new Dimensions() {
            @Override
            public void dimensions(long[] dimensions) {
                bdvSrc.getSource(timepointId,level).dimensions(dimensions);
            }

            @Override
            public long dimension(int d) {
                return bdvSrc.getSource(timepointId,level).dimension(d);
            }

            @Override
            public int numDimensions() {
                return bdvSrc.getSource(timepointId,level).numDimensions();
            }
        };
    }

    @Override
    public RandomAccessibleInterval<T> getImage(int timepointId, int level, ImgLoaderHint... hints) {
        return bdvSrc.getSource(timepointId,level);
    }

    @Override
    public double[][] getMipmapResolutions() {
        // Needs to compute mipmap resolutions... pfou
        double [][] mmResolutions = new double[bdvSrc.getNumMipmapLevels()][3];
        mmResolutions[0][0]=1;
        mmResolutions[0][1]=1;
        mmResolutions[0][2]=1;

        RandomAccessibleInterval srcL0 = bdvSrc.getSource(0,0);
        for (int iLevel=1;iLevel<bdvSrc.getNumMipmapLevels();iLevel++) {
            RandomAccessibleInterval srcLi = bdvSrc.getSource(0,iLevel);
            mmResolutions[iLevel][0] = (double)srcL0.dimension(0)/(double)srcLi.dimension(0);
            mmResolutions[iLevel][1] = (double)srcL0.dimension(1)/(double)srcLi.dimension(1);
            mmResolutions[iLevel][2] = (double)srcL0.dimension(2)/(double)srcLi.dimension(2);
        }
        return mmResolutions;
    }

    @Override
    public AffineTransform3D[] getMipmapTransforms() {
        AffineTransform3D[] ats = new AffineTransform3D[bdvSrc.getNumMipmapLevels()];

        for (int iLevel=0;iLevel<bdvSrc.getNumMipmapLevels();iLevel++) {
            AffineTransform3D at = new AffineTransform3D();
            bdvSrc.getSourceTransform(0,iLevel,at);
            ats[iLevel] = at;
        }

        return ats;
    }

    @Override
    public int numMipmapLevels() {
        return bdvSrc.getNumMipmapLevels();
    }

    @Override
    public RandomAccessibleInterval<FloatType> getFloatImage(int timepointId, boolean normalize, ImgLoaderHint... hints) {
        return cvtRaiToFloatRai.apply(getImage(timepointId,0));
    }

    @Override
    public Dimensions getImageSize(int timepointId) {
        return getImageSize(0,0);
    }

    @Override
    public VoxelDimensions getVoxelSize(int timepointId) {
        return bdvSrc.getVoxelDimensions();
    }
}
