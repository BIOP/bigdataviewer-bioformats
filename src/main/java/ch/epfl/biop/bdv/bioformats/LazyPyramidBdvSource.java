package ch.epfl.biop.bdv.bioformats;

import bdv.util.DefaultInterpolators;
import bdv.viewer.Interpolation;
import bdv.viewer.Source;
import mpicbg.spim.data.sequence.VoxelDimensions;
import net.imglib2.*;
import net.imglib2.cache.img.DiskCachedCellImgFactory;
import net.imglib2.cache.img.DiskCachedCellImgOptions;
import net.imglib2.img.Img;
import net.imglib2.realtransform.AffineTransform3D;
import net.imglib2.type.NativeType;
import net.imglib2.type.numeric.NumericType;
import net.imglib2.util.Util;
import net.imglib2.view.ExtendedRandomAccessibleInterval;
import net.imglib2.view.Views;

import java.util.concurrent.ConcurrentHashMap;

import static net.imglib2.cache.img.DiskCachedCellImgOptions.options;

public class LazyPyramidBdvSource< T extends NumericType<T> & NativeType<T>> implements Source<T> {

    protected final DefaultInterpolators< T > interpolators = new DefaultInterpolators<>();
    Source<T> origin;

    // Concurrent HashMap containing the randomAccessibleInterval of the source
    volatile ConcurrentHashMap<Integer, ConcurrentHashMap<Integer, RandomAccessibleInterval<T>>> raiMap = new ConcurrentHashMap<>();


    public LazyPyramidBdvSource(Source<T> source) {
        this.origin=source;
        this.scaleX=2;
        this.scaleY=2;
        this.scaleZ=2;
        this.nLevels=2;
    }

    int scaleX, scaleY, scaleZ, nLevels;

    public void setPyramidParameters(int scaleX, int scaleY, int scaleZ, int nLevels) {
        this.scaleX=scaleX;
        this.scaleY=scaleY;
        this.scaleZ=scaleZ;
        this.nLevels=nLevels;
    }

    @Override
    public boolean isPresent(int t) {
        return origin.isPresent(t);
    }

    @Override
    public RandomAccessibleInterval<T> getSource(int t, int level) {
        if (raiMap.containsKey(t)) {
            if (raiMap.get(t).containsKey(level)) {
                return raiMap.get(t).get(level);
            }
        }
        return createSource(t,level);
       /* if (level==0) {
            return origin.getSource(t,0);
        } else {

            return null;
        }*/
    }

    public RandomAccessibleInterval<T> createSource(int t, int level) {
        if (level==0) {
            if (!raiMap.containsKey(t)) {
                raiMap.put(t, new ConcurrentHashMap<>());
            }
            raiMap.get(t).put(level, origin.getSource(t,0));
        } else {
            System.out.println("Building level "+level);
            if (!raiMap.containsKey(t)) {
                raiMap.put(t, new ConcurrentHashMap<>());
            }

            //reader.setResolution(level);
            int sx = Math.max(1,(int) (origin.getSource(t,0).dimension(0)/Math.pow(scaleX,level)));
            int sy = Math.max(1,(int) (origin.getSource(t,0).dimension(1)/Math.pow(scaleY,level)));
            int sz = Math.max(1,(int) (origin.getSource(t,0).dimension(2)/Math.pow(scaleZ,level)));

            final int[] cellDimensions = new int[]{512,512,sz>1?16:1};

            // Cached Image Factory Options
            final DiskCachedCellImgOptions factoryOptions = options()
                    .cellDimensions(cellDimensions)
                    .cacheType(DiskCachedCellImgOptions.CacheType.BOUNDED)
                    .maxCacheSize(1000);


            // Creates cached image factory of Type Byte
            final DiskCachedCellImgFactory<T> factory = new DiskCachedCellImgFactory<>(Util.getTypeFromInterval(origin.getSource(t,0)), factoryOptions);

            int xc = cellDimensions[0];
            int yc = cellDimensions[1];
            int zc = cellDimensions[2];

            // Creates border image, with cell Consumer method, which creates the image



            final Img<T> rai =

            factory.create(new FinalInterval(new long[]{sx, sy, sz}),
                    cell -> {

                            while (raiMap.get(t).contains(level-1)) {
                                Thread.sleep(1000);
                            }
                            Cursor<T> out = Views.flatIterable(cell).cursor();
                            int minZ = (int) cell.min(2);
                            int maxZ = Math.min(minZ + zc, sz);

                            for (int z = minZ; z < maxZ; z++) {

                                int minX = (int) cell.min(0);
                                int maxX = Math.min(minX + xc, sx);

                                int minY = (int) cell.min(1);
                                int maxY = Math.min(minY + yc, sy);

                                int w = maxX - minX;
                                int h = maxY - minY;

                                while ((out.hasNext())) {
                                    out.next().setZero();//.set(bytes[idxPx]);
                                }
                            }
                    }, options().initializeCellsAsDirty(true));
            // Views.subsample(this.getSource(t, level-1), new long[]{scaleX, scaleY, scaleZ})
            raiMap.get(t).put(level, rai);
        }
        System.out.println("Building level "+level+" done!");
        return raiMap.get(t).get(level);
    }


    @Override
    public RealRandomAccessible<T> getInterpolatedSource(int t, int level, Interpolation method) {
        final T zero = getType();
        zero.setZero();
        ExtendedRandomAccessibleInterval<T, RandomAccessibleInterval< T >>
                eView = Views.extendZero(getSource( t, level ));
        RealRandomAccessible< T > realRandomAccessible = Views.interpolate( eView, interpolators.get(method) );
        return realRandomAccessible;
    }

    // Concurrent HashMap containing the affine transforms of the source - limitation : they are not changing over time
    volatile ConcurrentHashMap<Integer, AffineTransform3D> transforms = new ConcurrentHashMap<>();


    @Override
    public void getSourceTransform(int t, int level, AffineTransform3D transform) {
        if (!transforms.contains(level)) {
            if (level==0) {
                AffineTransform3D rootTransform = new AffineTransform3D();
                origin.getSourceTransform(t,0,rootTransform);
                transforms.put(0, rootTransform);
            } else {
                AffineTransform3D tr = new AffineTransform3D();
                origin.getSourceTransform(t,0, tr);
                double pXmm = tr.get(0,3);
                double pYmm = tr.get(1,3);
                double pZmm = tr.get(2,3);

                tr.translate(-pXmm, -pYmm, -pZmm);

                tr.set(tr.get(0,0)/Math.pow(scaleX,level),0,0);
                tr.set(tr.get(1,1)/Math.pow(scaleY,level),1,1);
                tr.set(tr.get(2,2)/Math.pow(scaleZ,level),2,2);

                tr.translate(pXmm, pYmm, pZmm);
                transforms.put(level, tr);
            }
        }
        transform.set(transforms.get(level));
    }

    @Override
    public T getType() {
        return origin.getType();
    }

    @Override
    public String getName() {
        return origin.getName();
    }

    @Override
    public VoxelDimensions getVoxelDimensions() {
        return origin.getVoxelDimensions();
    }

    @Override
    public int getNumMipmapLevels() {
        return nLevels;
    }
}
