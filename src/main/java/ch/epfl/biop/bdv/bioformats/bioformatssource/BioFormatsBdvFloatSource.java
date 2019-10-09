package ch.epfl.biop.bdv.bioformats.bioformatssource;

import loci.formats.IFormatReader;
import net.imglib2.Cursor;
import net.imglib2.FinalInterval;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.cache.img.DiskCachedCellImgFactory;
import net.imglib2.cache.img.DiskCachedCellImgOptions;
import net.imglib2.img.Img;
import net.imglib2.type.numeric.real.FloatType;
import net.imglib2.view.Views;
import ome.units.unit.Unit;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.concurrent.ConcurrentHashMap;

import static net.imglib2.cache.img.DiskCachedCellImgOptions.options;

public class BioFormatsBdvFloatSource extends BioFormatsBdvSource<FloatType> {
    public BioFormatsBdvFloatSource(IFormatReader reader,
                                    int image_index,
                                    int channel_index,
                                    boolean sw,
                                    FinalInterval cacheBlockSize,
                                    boolean useBioFormatsXYBlockSize,
                                    boolean ignoreBioFormatsLocationMetaData,
                                    boolean ignoreBioFormatsVoxelSizeMetaData,
                                    boolean positionConventionIsCenter, Unit u) {
        super(reader, image_index, channel_index, sw, cacheBlockSize, useBioFormatsXYBlockSize,
                ignoreBioFormatsLocationMetaData, ignoreBioFormatsVoxelSizeMetaData,
                positionConventionIsCenter,u);
    }


    @Override
    public RandomAccessibleInterval<FloatType> createSource(int t, int level) {
        synchronized(reader) {
            if (!raiMap.containsKey(t)) {
                raiMap.put(t, new ConcurrentHashMap<>());
            }

            reader.setResolution(level);

            boolean littleEndian = reader.isLittleEndian();

            int sx = reader.getSizeX();
            int sy = reader.getSizeY();
            int sz = (!is3D)?1:reader.getSizeZ();

            final DiskCachedCellImgOptions factoryOptions = options()
                    .cellDimensions( cellDimensions )
                    .cacheType( DiskCachedCellImgOptions.CacheType.BOUNDED )
                    .maxCacheSize( 1000 );


            // Creates cached image factory of Type Byte
            final DiskCachedCellImgFactory<FloatType> factory = new DiskCachedCellImgFactory<>( new FloatType() , factoryOptions );

            int xc = cellDimensions[0];
            int yc = cellDimensions[1];
            int zc = cellDimensions[2];

            // Creates border image, with cell Consumer method, which creates the image

            final Img<FloatType> rai = factory.create(new FinalInterval(new long[]{sx, sy, sz}),
                    cell -> {
                        synchronized(reader) {
                            reader.setResolution(level);
                            Cursor<FloatType> out = Views.flatIterable(cell).cursor();
                            int minZ = (int) cell.min(2);
                            int maxZ = Math.min(minZ + zc, reader.getSizeZ());

                            for (int z=minZ;z<maxZ;z++) {
                                int minX = (int) cell.min(0);
                                int maxX = Math.min(minX + xc, reader.getSizeX());

                                int minY = (int) cell.min(1);
                                int maxY = Math.min(minY + yc, reader.getSizeY());

                                int w = maxX - minX;
                                int h = maxY - minY;


                                int totBytes = (w * h)*4;

                                int idxPx = 0;

                                byte[] bytes = reader.openBytes(switchZandC?reader.getIndex(cChannel,z,t):reader.getIndex(z,cChannel,t), minX, minY, w, h);

                                byte[] curBytes = new byte[4];
                                if (littleEndian) { // TODO improve this dirty switch block
                                    while ((out.hasNext()) && (idxPx < totBytes)) {
                                        curBytes[0]= bytes[idxPx];
                                        curBytes[1]= bytes[idxPx+1];
                                        curBytes[2]= bytes[idxPx+2];
                                        curBytes[3]= bytes[idxPx+3];
                                        out.next().set( ByteBuffer.wrap(curBytes).order(ByteOrder.LITTLE_ENDIAN).getFloat());

                                        idxPx += 4;
                                    }
                                } else {
                                    while ((out.hasNext()) && (idxPx < totBytes)) {

                                        curBytes[0]= bytes[idxPx];
                                        curBytes[1]= bytes[idxPx+1];
                                        curBytes[2]= bytes[idxPx+2];
                                        curBytes[3]= bytes[idxPx+3];
                                        out.next().set( ByteBuffer.wrap(curBytes).order(ByteOrder.BIG_ENDIAN).getFloat());

                                        idxPx += 4;
                                    }
                                }
                            }
                        }
                    }, options().initializeCellsAsDirty(true));

            raiMap.get(t).put(level, rai);

            return raiMap.get(t).get(level);
        }
    }

    @Override
    public FloatType getType() {
        return new FloatType();
    }
}
