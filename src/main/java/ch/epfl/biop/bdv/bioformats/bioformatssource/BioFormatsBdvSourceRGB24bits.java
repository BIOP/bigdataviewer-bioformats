package ch.epfl.biop.bdv.bioformats.bioformatssource;

import loci.formats.IFormatReader;
import net.imglib2.Cursor;
import net.imglib2.FinalInterval;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.cache.img.ReadOnlyCachedCellImgFactory;
import net.imglib2.cache.img.ReadOnlyCachedCellImgOptions;
import net.imglib2.cache.img.optional.CacheOptions;
import net.imglib2.img.Img;
import net.imglib2.realtransform.AffineTransform3D;
import net.imglib2.type.numeric.ARGBType;
import net.imglib2.view.Views;
import ome.units.quantity.Length;
import ome.units.unit.Unit;

import java.util.concurrent.ConcurrentHashMap;

public class BioFormatsBdvSourceRGB24bits extends BioFormatsBdvSource<ARGBType> {

    public BioFormatsBdvSourceRGB24bits(ReaderPool readerPool,
                                        int image_index,
                                        int channel_index,
                                        boolean swZC,
                                        FinalInterval cacheBlockSize,
                                        int maxCacheSize,
                                        boolean useBioFormatsXYBlockSize,
                                        boolean ignoreBioFormatsLocationMetaData,
                                        boolean ignoreBioFormatsVoxelSizeMetaData,
                                        boolean positionConventionIsCenter,
                                        Length locationReferenceFrameLength,
                                        Length voxSizeReferenceFrameLength,
                                        Unit u,
                                        AffineTransform3D locationPreTransform,
                                        AffineTransform3D locationPostTransform,
                                        AffineTransform3D voxSizePreTransform,
                                        AffineTransform3D voxSizePostTransform,
                                        boolean[] axesFlip) {
        super(readerPool,
                image_index,
                channel_index,
                swZC,
                cacheBlockSize,
                maxCacheSize,
                useBioFormatsXYBlockSize,
                ignoreBioFormatsLocationMetaData,
                ignoreBioFormatsVoxelSizeMetaData,
                positionConventionIsCenter,
                locationReferenceFrameLength,
                voxSizeReferenceFrameLength,
                u,
                locationPreTransform,
                locationPostTransform,
                voxSizePreTransform,
                voxSizePostTransform,
                axesFlip);
    }

    /**
     * The core of the source...
     * @param t
     * @param level
     * @return
     */
    public RandomAccessibleInterval<ARGBType> createSource(int t, int level) {
        try {
            IFormatReader reader_init = readerPool.acquire();
            reader_init.setSeries(this.cSerie);

            if (!raiMap.containsKey(t)) {
                raiMap.put(t, new ConcurrentHashMap<>());
            }

            reader_init.setResolution(level);
            int sx = reader_init.getSizeX();
            int sy = reader_init.getSizeY();
            int sz = (!is3D)?1:reader_init.getSizeZ();
            
            // Cached Image Factory Options
            final ReadOnlyCachedCellImgOptions factoryOptions = ReadOnlyCachedCellImgOptions.options()
                    .cellDimensions( cellDimensions )
                    .cacheType( CacheOptions.CacheType.SOFTREF );

            // Creates read only cached image factory
            final ReadOnlyCachedCellImgFactory factory = new ReadOnlyCachedCellImgFactory( factoryOptions );

            int xc = cellDimensions[0];
            int yc = cellDimensions[1];
            int zc = cellDimensions[2];

            // TODO improve interleave case
            final Img<ARGBType> rai;
            if (reader_init.isInterleaved()) {
                rai = factory.create(new long[]{sx, sy, sz}, new ARGBType(),
                        cell -> {
                            try {
                                IFormatReader reader = readerPool.acquire();
                                reader.setSeries(cSerie);

                                reader.setResolution(level);
                                Cursor<ARGBType> out = Views.flatIterable(cell).cursor();
                                int minZ = (int) cell.min(2);
                                int maxZ = Math.min(minZ + zc, reader.getSizeZ());

                                for (int z=minZ;z<maxZ;z++) {
                                    int minX = (int) cell.min(0);
                                    int maxX = Math.min(minX + xc, reader.getSizeX());

                                    int minY = (int) cell.min(1);
                                    int maxY = Math.min(minY + yc, reader.getSizeY());

                                    int w = maxX - minX;
                                    int h = maxY - minY;

                                    byte[] bytes = reader.openBytes(switchZandC?reader.getIndex(cChannel,z,t):reader.getIndex(z,cChannel,t),
                                            minX, minY, w, h);

                                    int idxPx = 0;

                                    int totBytes = (w * h) * 3;
                                    while ((out.hasNext()) && (idxPx < totBytes)) {
                                        int v = ((0xff) << 24 ) | ((bytes[idxPx] & 0xff) << 16 ) | ((bytes[idxPx + 1] & 0xff) << 8) | (bytes[idxPx+2] & 0xff);
                                        out.next().set(v);
                                        idxPx += 3;
                                    }
                                }
                                readerPool.recycle(reader);
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                        });
            } else {
                rai = factory.create(new long[]{sx, sy, sz}, new ARGBType(),
                        cell -> {
                            try {
                                IFormatReader reader = readerPool.acquire();
                                reader.setSeries(cSerie);

                                reader.setResolution(level);
                                Cursor<ARGBType> out = Views.flatIterable(cell).cursor();
                                int minZ = (int) cell.min(2);
                                int maxZ = Math.min(minZ + zc, reader.getSizeZ());

                                for (int z=minZ;z<maxZ;z++) {
                                    int minX = (int) cell.min(0);
                                    int maxX = Math.min(minX + xc, reader.getSizeX());

                                    int minY = (int) cell.min(1);
                                    int maxY = Math.min(minY + yc, reader.getSizeY());

                                    int w = maxX - minX;
                                    int h = maxY - minY;


                                    byte[] bytesR = reader.openBytes(switchZandC?reader.getIndex(cChannel,z,t):reader.getIndex(z,cChannel,t),
                                            minX, minY, w, h);

                                    int idxPx = 0;


                                    int totBytes = (w * h) ;

                                    int gOffset = totBytes;

                                    int bOffset = 2*totBytes;

                                    while ((out.hasNext()) && (idxPx < totBytes)) {
                                        int v = ((bytesR[idxPx] & 0xff) << 16 ) | ((bytesR[idxPx+gOffset] & 0xff) << 8) | (bytesR[idxPx+bOffset] & 0xff);
                                        out.next().set(v);
                                        idxPx += 1;
                                    }
                                }
                                readerPool.recycle(reader);
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                        });
            }

            raiMap.get(t).put(level, rai);
            readerPool.recycle(reader_init);
            return raiMap.get(t).get(level);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    @Override
    public ARGBType getType() {
        return new ARGBType();
    }

}
