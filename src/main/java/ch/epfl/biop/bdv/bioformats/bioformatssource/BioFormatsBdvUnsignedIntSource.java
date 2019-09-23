package ch.epfl.biop.bdv.bioformats.bioformatssource;

import loci.formats.IFormatReader;
import net.imglib2.Cursor;
import net.imglib2.FinalInterval;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.cache.img.DiskCachedCellImgFactory;
import net.imglib2.cache.img.DiskCachedCellImgOptions;
import net.imglib2.img.Img;
import net.imglib2.type.numeric.integer.UnsignedIntType;
import net.imglib2.view.Views;

import java.util.concurrent.ConcurrentHashMap;

import static net.imglib2.cache.img.DiskCachedCellImgOptions.options;

// TODO : say interleaved channels not supported
public class BioFormatsBdvUnsignedIntSource extends BioFormatsBdvSource<UnsignedIntType> {
    public BioFormatsBdvUnsignedIntSource(IFormatReader reader, int image_index, int channel_index, boolean sw, FinalInterval cacheBlockSize, boolean useBioFormatsXYBlockSize) {
        super(reader, image_index, channel_index, sw, cacheBlockSize, useBioFormatsXYBlockSize);
    }


    @Override
    public RandomAccessibleInterval<UnsignedIntType> createSource(int t, int level) {
        //assert is8bit;
        synchronized(reader) {
            //System.out.println("Building level "+level);
            if (!raiMap.containsKey(t)) {
                raiMap.put(t, new ConcurrentHashMap<>());
            }

            reader.setResolution(level);

            boolean littleEndian = reader.isLittleEndian();

            //System.out.println("reader.getCoreIndex()="+reader.getCoreIndex());
            //System.out.println(reader.getDatasetStructureDescription());

            int sx = reader.getSizeX();
            int sy = reader.getSizeY();
            int sz = (!is3D)?1:reader.getSizeZ();

            /*cellDimensions = new int[] {
                    useBioFormatsXYBlockSize?reader.getOptimalTileWidth():(int)cacheBlockSize.dimension(0),
                    useBioFormatsXYBlockSize?reader.getOptimalTileHeight():(int)cacheBlockSize.dimension(1),
                    (!is3D)?1:(int)cacheBlockSize.dimension(2)};*/
            // Cached Image Factory Options
            final DiskCachedCellImgOptions factoryOptions = options()
                    .cellDimensions( cellDimensions )
                    .cacheType( DiskCachedCellImgOptions.CacheType.BOUNDED )
                    .maxCacheSize( 1000 );


            // Creates cached image factory of Type Byte
            final DiskCachedCellImgFactory<UnsignedIntType> factory = new DiskCachedCellImgFactory<>( new UnsignedIntType() , factoryOptions );

            int xc = cellDimensions[0];
            int yc = cellDimensions[1];
            int zc = cellDimensions[2];

            // Creates border image, with cell Consumer method, which creates the image

            final Img<UnsignedIntType> rai = factory.create(new FinalInterval(new long[]{sx, sy, sz}),
                    cell -> {
                        synchronized(reader) {
                            reader.setResolution(level);
                            Cursor<UnsignedIntType> out = Views.flatIterable(cell).cursor();
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

                                int idxPx = 0;//reader.getIndex(0,cChannel,0);//totBytes*cChannel;

                                byte[] bytes = reader.openBytes(switchZandC?reader.getIndex(cChannel,z,t):reader.getIndex(z,cChannel,t), minX, minY, w, h);

                                if (littleEndian) { // TODO improve this dirty switch block
                                    while ((out.hasNext()) && (idxPx < totBytes)) {
                                        int v = ( (bytes[idxPx + 3] & 0xff) << 24) | ((bytes[idxPx + 2] & 0xff) << 16) | ((bytes[idxPx + 1] & 0xff) << 8) | (bytes[idxPx] & 0xff);
                                        out.next().set(v);
                                        idxPx += 4;
                                    }
                                } else {
                                    while ((out.hasNext()) && (idxPx < totBytes)) {
                                        int v = ( (bytes[idxPx] & 0xff) << 24) | ((bytes[idxPx + 1] & 0xff) << 16) | ((bytes[idxPx + 2] & 0xff) << 8) | (bytes[idxPx + 3] & 0xff);
                                        out.next().set(v);
                                        idxPx += 4;
                                    }
                                }
                            }
                        }
                    }, options().initializeCellsAsDirty(true));

            raiMap.get(t).put(level, rai);

            //System.out.println("Building level "+level+" done!");
            return raiMap.get(t).get(level);
        }
    }

    @Override
    public UnsignedIntType getType() {
        return new UnsignedIntType();
    }
}
