package ch.epfl.biop.bdv.bioformats.bioformatssource;

import loci.formats.IFormatReader;
import net.imglib2.Cursor;
import net.imglib2.FinalInterval;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.cache.img.DiskCachedCellImgFactory;
import net.imglib2.cache.img.DiskCachedCellImgOptions;
import net.imglib2.cache.img.ReadOnlyCachedCellImgFactory;
import net.imglib2.cache.img.ReadOnlyCachedCellImgOptions;
import net.imglib2.cache.img.optional.CacheOptions;
import net.imglib2.img.Img;
import net.imglib2.realtransform.AffineTransform3D;
import net.imglib2.type.numeric.integer.UnsignedShortType;
import net.imglib2.view.Views;
import ome.units.quantity.Length;
import ome.units.unit.Unit;

import java.util.concurrent.ConcurrentHashMap;

import static net.imglib2.cache.img.DiskCachedCellImgOptions.options;

public class BioFormatsBdvSourceUnsignedShort extends BioFormatsBdvSource<UnsignedShortType> {

    public BioFormatsBdvSourceUnsignedShort(ReaderPool readerPool,
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

    @Override
    public RandomAccessibleInterval<UnsignedShortType> createSource(int t, int level) {
        try {
            IFormatReader reader_init = readerPool.acquire();
            reader_init.setSeries(this.cSerie);
            reader_init.setResolution(level);

            if (!raiMap.containsKey(t)) {
                raiMap.put(t, new ConcurrentHashMap<>());
            }

            boolean littleEndian = reader_init.isLittleEndian();

            int sx = reader_init.getSizeX();
            int sy = reader_init.getSizeY();
            int sz = (!is3D) ? 1 : reader_init.getSizeZ();

            final ReadOnlyCachedCellImgOptions factoryOptions = ReadOnlyCachedCellImgOptions.options()
                    .cellDimensions(cellDimensions)
                    .cacheType(CacheOptions.CacheType.SOFTREF);

            // Creates cached image factory of Type Byte
            final ReadOnlyCachedCellImgFactory factory = new ReadOnlyCachedCellImgFactory(factoryOptions);

            int xc = cellDimensions[0];
            int yc = cellDimensions[1];
            int zc = cellDimensions[2];

            // Creates image, with cell Consumer method, which creates the image
            final Img<UnsignedShortType> rai = factory.create(new long[]{sx, sy, sz}, new UnsignedShortType(),
                    cell -> {
                        try {
                            IFormatReader reader = readerPool.acquire();
                            reader.setSeries(this.cSerie);
                            reader.setResolution(level);
                            Cursor<UnsignedShortType> out = Views.flatIterable(cell).cursor();
                            int minZ = (int) cell.min(2);
                            int maxZ = Math.min(minZ + zc, reader.getSizeZ());

                            for (int z = minZ; z < maxZ; z++) {
                                int minX = (int) cell.min(0);
                                int maxX = Math.min(minX + xc, reader.getSizeX());

                                int minY = (int) cell.min(1);
                                int maxY = Math.min(minY + yc, reader.getSizeY());

                                int w = maxX - minX;
                                int h = maxY - minY;


                                int totBytes = (w * h) * 2;

                                int idxPx = 0;

                                byte[] bytes = reader.openBytes(switchZandC ? reader.getIndex(cChannel, z, t) : reader.getIndex(z, cChannel, t), minX, minY, w, h);

                                if (littleEndian) { // TODO improve this dirty switch block
                                    while ((out.hasNext()) && (idxPx < totBytes)) {
                                        int v = ((bytes[idxPx + 1] & 0xff) << 8) | (bytes[idxPx] & 0xff);
                                        out.next().set(v);
                                        idxPx += 2;
                                    }
                                } else {
                                    while ((out.hasNext()) && (idxPx < totBytes)) {
                                        int v = ((bytes[idxPx] & 0xff) << 8) | (bytes[idxPx + 1] & 0xff);
                                        out.next().set(v);
                                        idxPx += 2;
                                    }
                                }
                            }
                            readerPool.recycle(reader);
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    });

            raiMap.get(t).put(level, rai);
            readerPool.recycle(reader_init);
            return raiMap.get(t).get(level);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    @Override
    public UnsignedShortType getType() {
        return new UnsignedShortType();
    }
}