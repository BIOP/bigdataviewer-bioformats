package ch.epfl.biop.bdv.bioformats.bioformatssource;

import loci.formats.IFormatReader;
import net.imglib2.Cursor;
import net.imglib2.FinalInterval;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.cache.img.DiskCachedCellImgFactory;
import net.imglib2.cache.img.DiskCachedCellImgOptions;
import net.imglib2.img.Img;
import net.imglib2.realtransform.AffineTransform3D;
import net.imglib2.type.numeric.integer.UnsignedByteType;
import net.imglib2.view.Views;
import ome.units.quantity.Length;
import ome.units.unit.Unit;

import java.util.concurrent.ConcurrentHashMap;

import static net.imglib2.cache.img.DiskCachedCellImgOptions.options;

public class BioFormatsBdvSourceUnsignedByte extends BioFormatsBdvSource<UnsignedByteType> {
    public BioFormatsBdvSourceUnsignedByte(IFormatReader reader,
                                           int image_index,
                                           int channel_index,
                                           boolean swZC,
                                           FinalInterval cacheBlockSize,
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
        super(reader,
                image_index,
                channel_index,
                swZC,
                cacheBlockSize,
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
    public RandomAccessibleInterval<UnsignedByteType> createSource(int t, int level) {
        synchronized(reader) {
            if (!raiMap.containsKey(t)) {
                raiMap.put(t, new ConcurrentHashMap<>());
            }

            reader.setResolution(level);
            int sx = reader.getSizeX();
            int sy = reader.getSizeY();
            int sz = (!is3D)?1:reader.getSizeZ();

            // Cached Image Factory Options
            final DiskCachedCellImgOptions factoryOptions = options()
                    .cellDimensions( cellDimensions )
                    .cacheType( DiskCachedCellImgOptions.CacheType.BOUNDED )
                    .maxCacheSize( 1000 );


            // Creates cached image factory of Type Byte
            final DiskCachedCellImgFactory<UnsignedByteType> factory = new DiskCachedCellImgFactory<>( new UnsignedByteType() , factoryOptions );

            int xc = cellDimensions[0];
            int yc = cellDimensions[1];
            int zc = cellDimensions[2];

            final Img<UnsignedByteType> rai = factory.create(new FinalInterval(new long[]{sx, sy, sz}),
                    cell -> {
                        synchronized(reader) {
                            reader.setResolution(level);
                            Cursor<UnsignedByteType> out = Views.flatIterable(cell).cursor();
                            int minZ = (int) cell.min(2);
                            int maxZ = Math.min(minZ + zc, reader.getSizeZ());

                            for (int z=minZ;z<maxZ;z++) {

                                int minX = (int) cell.min(0);
                                int maxX = Math.min(minX + xc, reader.getSizeX());

                                int minY = (int) cell.min(1);
                                int maxY = Math.min(minY + yc, reader.getSizeY());

                                int w = maxX - minX;
                                int h = maxY - minY;

                                byte[] bytes = reader.openBytes(switchZandC ? reader.getIndex(cChannel, z, t) : reader.getIndex(z, cChannel, t), minX, minY, w, h);

                                int idxPx = 0;

                                int totBytes = (w * h);
                                while ((out.hasNext()) && (idxPx < totBytes)) {
                                    out.next().set(bytes[idxPx]);
                                    idxPx++;
                                }
                            }
                        }
                    }, options().initializeCellsAsDirty(true));

            raiMap.get(t).put(level, rai);

            return raiMap.get(t).get(level);
        }
    }

    @Override
    public UnsignedByteType getType() {
        return new UnsignedByteType();
    }
}
