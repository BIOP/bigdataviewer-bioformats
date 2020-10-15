/*-
 * #%L
 * Commands and function for opening, conversion and easy use of bioformats format into BigDataViewer
 * %%
 * Copyright (C) 2019 - 2020 Nicolas Chiaruttini, BIOP, EPFL
 * %%
 * Redistribution and use in source and binary forms, with or without modification,
 * are permitted provided that the following conditions are met:
 * 
 * 1. Redistributions of source code must retain the above copyright notice, this
 *    list of conditions and the following disclaimer.
 * 
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 * 
 * 3. Neither the name of the BIOP nor the names of its contributors
 *    may be used to endorse or promote products derived from this software without
 *    specific prior written permission.
 * 
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED.
 * IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT,
 * INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING,
 * BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
 * DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF
 * LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE
 * OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED
 * OF THE POSSIBILITY OF SUCH DAMAGE.
 * #L%
 */
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
import net.imglib2.type.numeric.integer.UnsignedByteType;
import net.imglib2.view.Views;
import ome.units.quantity.Length;
import ome.units.unit.Unit;

import java.util.concurrent.ConcurrentHashMap;

public class BioFormatsBdvSourceUnsignedByte extends BioFormatsBdvSource<UnsignedByteType> {
    public BioFormatsBdvSourceUnsignedByte(ReaderPool readerPool,
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
    public RandomAccessibleInterval<UnsignedByteType> createSource(int t, int level) {
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

            // Creates cached image factory of Type Byte
            final ReadOnlyCachedCellImgFactory factory = new ReadOnlyCachedCellImgFactory( factoryOptions );

            int xc = cellDimensions[0];
            int yc = cellDimensions[1];
            int zc = cellDimensions[2];

            final Img<UnsignedByteType> rai = factory.create(new long[]{sx, sy, sz}, new UnsignedByteType(),
                    cell -> {
                        try {
                            IFormatReader reader = readerPool.acquire();
                            reader.setSeries(this.cSerie);
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
    public UnsignedByteType getType() {
        return new UnsignedByteType();
    }
}
