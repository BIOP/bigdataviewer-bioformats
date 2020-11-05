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

import bdv.util.DefaultInterpolators;
import bdv.viewer.Interpolation;
import bdv.viewer.Source;
import ch.epfl.biop.bdv.bioformats.BioFormatsMetaDataHelper;
import loci.formats.*;
import loci.formats.meta.IMetadata;
import mpicbg.spim.data.sequence.VoxelDimensions;
import net.imglib2.FinalInterval;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.RealRandomAccessible;
import net.imglib2.Volatile;
import net.imglib2.cache.img.ReadOnlyCachedCellImgOptions;
import net.imglib2.img.Img;
import net.imglib2.realtransform.AffineTransform3D;
import net.imglib2.type.Type;
import net.imglib2.type.numeric.ARGBType;
import net.imglib2.type.numeric.NumericType;
import net.imglib2.type.numeric.integer.UnsignedByteType;
import net.imglib2.type.numeric.integer.UnsignedIntType;
import net.imglib2.type.numeric.integer.UnsignedShortType;
import net.imglib2.type.numeric.real.FloatType;
import net.imglib2.type.volatiles.*;
import net.imglib2.view.ExtendedRandomAccessibleInterval;
import net.imglib2.view.Views;
import ome.units.quantity.Length;
import ome.units.unit.Unit;
import ome.xml.model.enums.PixelType;

import java.util.concurrent.ConcurrentHashMap;

/**
 * BigDataViewer multiresolution source built from BioFormat reader
 *
 * - NumericType requirement is just for Zero extension out of bounds strategy
 *
 * Useful resources:
 * All the bio-formats-examples repository on GitHub
 *
 * @param <T> pixel type
 *
 * @author Nicolas Chiaruttini, BIOP, EPFL, 2019
 */

public abstract class BioFormatsBdvSource<T extends NumericType< T > > implements Source<T> {

    protected final DefaultInterpolators< T > interpolators = new DefaultInterpolators<>();

    // Fix BioFormat confusion between c and z in some file formats
    public boolean switchZandC;

    // Number of timepoints of the source
    public int numberOfTimePoints;

    // Position of dataset + voxel spacing
    public double pXmm, pYmm, pZmm, dXmm, dYmm, dZmm;

    // Affine transform of the highest resolution image
    AffineTransform3D rootTransform = new AffineTransform3D();

    // Concurrent HashMap containing the affine transforms of the source - limitation : they are not changing over time
    volatile ConcurrentHashMap<Integer, AffineTransform3D> transforms = new ConcurrentHashMap<>();

    // Concurrent HashMap containing the randomAccessibleInterval of the source
    volatile ConcurrentHashMap<Integer, ConcurrentHashMap<Integer, Img<T>>> raiMap = new ConcurrentHashMap<>();

    // Source name
    public String sourceName;

    //final FinalInterval cacheBlockSize;

    public boolean useBioFormatsXYBlockSize;

    public boolean ignoreBioFormatsVoxelSizeMetaData;

    public boolean is3D;

    public boolean ignoreBioFormatsLocationMetaData;

    boolean positionIsImageCenter;

    int maxCacheSize;

    public int[] cellDimensions;

    final Unit<Length> targetUnit;

    // Inner VoxelDimensions, taken from BioFormats
    VoxelDimensions voxelsDimensions;

    // Channel index of the current source
    public int cChannel;

    // Serie index of the current source
    public int cSerie;

    Length positionReferenceFrameLength;
    Length voxSizeReferenceFrameLength;

    AffineTransform3D positionPreTransform;
    AffineTransform3D positionPostTransform;
    AffineTransform3D voxSizePreTransform;
    AffineTransform3D voxSizePostTransform;

    boolean[] axesOfImageFlip;

    final ReaderPool readerPool;

    ReadOnlyCachedCellImgOptions cacheOptions; // can't be final because of the Exception...
    private int numMipMapLevels = -1;

    /**
     * Bio Format source constructor
     * @param readerPool bio format reader pool (flatten should be set to false to allow for multiresolution handling)
     * @param image_index image index within source
     * @param channel_index channel index within source
     * @param swZC switch or not z and c
     */
    public BioFormatsBdvSource(ReaderPool readerPool,
                               int image_index,
                               int channel_index,
                               boolean swZC,
                               FinalInterval cacheBlockSize,
                               ReadOnlyCachedCellImgOptions cacheOptions,
                               boolean useBioFormatsXYBlockSize,
                               boolean ignoreBioFormatsLocationMetaData,
                               boolean ignoreBioFormatsVoxelSizeMetaData,
                               boolean positionIsImageCenter,
                               Length positionReferenceFrameLength,
                               Length voxSizeReferenceFrameLength,
                               Unit<Length> u,
                               AffineTransform3D positionPreTransform,
                               AffineTransform3D positionPostTransform,
                               AffineTransform3D voxSizePreTransform,
                               AffineTransform3D voxSizePostTransform,
                               boolean[] axesFlip)
    {
        this.readerPool = readerPool;
        this.targetUnit = u;
        this.ignoreBioFormatsLocationMetaData = ignoreBioFormatsLocationMetaData;
        this.ignoreBioFormatsVoxelSizeMetaData = ignoreBioFormatsVoxelSizeMetaData;
        this.useBioFormatsXYBlockSize = useBioFormatsXYBlockSize;
        this.switchZandC = swZC;

        try {
            IFormatReader reader = readerPool.acquire();
            reader.setSeries(cSerie);
            this.numberOfTimePoints = reader.getSizeT();

            this.cSerie = image_index;
            this.cChannel = channel_index;
            this.positionIsImageCenter = positionIsImageCenter;
            this.positionReferenceFrameLength = positionReferenceFrameLength;
            this.voxSizeReferenceFrameLength = voxSizeReferenceFrameLength;
            this.positionPostTransform = positionPostTransform;
            this.positionPreTransform = positionPreTransform;
            this.voxSizePostTransform = voxSizePostTransform;
            this.voxSizePreTransform = voxSizePreTransform;
            this.axesOfImageFlip = axesFlip;

            // MetaData
            final IMetadata omeMeta = (IMetadata) reader.getMetadataStore();

            // SourceName
            if (omeMeta.getChannelName(image_index, channel_index) != null) {
                if (omeMeta.getChannelName(image_index, channel_index).equals("null")) {
                    this.sourceName = omeMeta.getImageName(image_index);
                } else {
                    this.sourceName = omeMeta.getImageName(image_index) + "_ch_" + omeMeta.getChannelName(image_index, channel_index);
                }
            } else {
                this.sourceName = omeMeta.getImageName(image_index);
            }

            if (this.sourceName == null) {
                this.sourceName = "null";
            }

            setRootTransform(omeMeta, image_index);

            if (reader.getSizeZ() > 1) {
                is3D = true;
            } else {
                is3D = false;
            }

            voxelsDimensions = BioFormatsMetaDataHelper.getSeriesVoxelDimensions(
                    omeMeta, image_index, u, voxSizeReferenceFrameLength
            );

            cellDimensions = new int[]{
                    useBioFormatsXYBlockSize ? reader.getOptimalTileWidth() : (int) cacheBlockSize.dimension(0),
                    useBioFormatsXYBlockSize ? reader.getOptimalTileHeight() : (int) cacheBlockSize.dimension(1),
                    (!is3D) ? 1 : (int) cacheBlockSize.dimension(2)};

            this.cacheOptions = cacheOptions.cellDimensions( cellDimensions );

            readerPool.recycle(reader);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void setRootTransform(IMetadata omeMeta, int image_index) {
        if (ignoreBioFormatsLocationMetaData) {
            rootTransform.identity();
        } else {
            rootTransform.set(BioFormatsMetaDataHelper.getSeriesRootTransform(omeMeta,
                    image_index,targetUnit,
                    positionPreTransform,
                    positionPostTransform,
                    positionReferenceFrameLength,
                    positionIsImageCenter,
                    voxSizePreTransform,
                    voxSizePostTransform,
                    voxSizeReferenceFrameLength,
                    axesOfImageFlip));
        }
    }

    public ReaderPool getReaderPool() {
        return this.readerPool;
    }

    /**
     * Returns true if the BioFormat reader specifies a higher number of sources
     * @param t
     * @return
     */
    @Override
    public boolean isPresent(int t) {
        return t<numberOfTimePoints;
    }

    /**
     * The core function of the source which is implemented in subclasses
     * @see BioFormatsBdvSourceRGB24bits
     * @see BioFormatsBdvSourceUnsignedByte
     * @see BioFormatsBdvSourceUnsignedShort
     * @param t // timepoint
     * @param level // resolution level
     * @return
     */
    abstract public RandomAccessibleInterval<T> createSource(int t, int level);

    /**
     * Returns stored RAI of requested timepoint and resolution level
     * @param t
     * @param level
     * @return
     */
    @Override
    public RandomAccessibleInterval<T> getSource(int t, int level) {
        level = levelCropper(level);
        if (raiMap.containsKey(t)) {
            if (raiMap.get(t).containsKey(level)) {
                return raiMap.get(t).get(level);
            }
        }
        return createSource(t,level);
    }

    /**
     * Extends to zero out of bounds strategy
     * @param t
     * @param level
     * @param method
     * @return
     */
    @Override
    public RealRandomAccessible<T> getInterpolatedSource(int t, int level, Interpolation method) {
        level = levelCropper(level);
        final T zero = getType();
        zero.setZero();
        ExtendedRandomAccessibleInterval<T, RandomAccessibleInterval< T >>
                eView = Views.extendZero(getSource( t, level ));
        RealRandomAccessible< T > realRandomAccessible = Views.interpolate( eView, interpolators.get(method) );
        return realRandomAccessible;
    }

    public int levelCropper(int level) {
        return level;//this.getNumMipmapLevels()-1;
    }

    /**
     * Finds transform based on the root transform (highest resolution transform)
     * And assuming all resolution level cover the same area of imaging
     * @param t
     * @param level
     * @param transform
     */
    @Override
    public void getSourceTransform(int t, int level, AffineTransform3D transform) {
        // Ignoring t parameters : assuming all transforms are identical over time
        // TODO How is the pyramid in 3D ?
        level = levelCropper(level);
        if (!transforms.contains(level)) {
            if (level==0) {
                transforms.put(0, this.rootTransform);
            } else {
                AffineTransform3D tr = new AffineTransform3D();
                tr.set(rootTransform);

                // Apply ratio in numbers of pixel
                long nPixXLvl0 = 0;
                long nPixYLvl0 = 0;
                long nPixZLvl0 = 0;
                try {
                    IFormatReader reader = readerPool.acquire();
                    reader.setSeries(cSerie);
                    reader.setResolution(0);
                    nPixXLvl0 = reader.getSizeX();
                    nPixYLvl0 = reader.getSizeY();
                    nPixZLvl0 = reader.getSizeZ();
                    readerPool.recycle(reader);
                } catch (Exception e) {
                    e.printStackTrace();
                }
                long nPixXCurrentLvl = this.getSource(t,level).dimension(0);

                long nPixYCurrentLvl = this.getSource(t,level).dimension(1);

                long nPixZCurrentLvl = this.getSource(t,level).dimension(2);

                tr.translate(-pXmm, -pYmm, -pZmm);

                tr.set(tr.get(0,0)*((double)nPixXLvl0/(double)nPixXCurrentLvl),0,0);
                tr.set(tr.get(1,1)*((double)nPixYLvl0/(double)nPixYCurrentLvl),1,1);
                tr.set(tr.get(2,2)*((double)nPixZLvl0/(double)nPixZCurrentLvl),2,2);

                tr.translate(pXmm, pYmm, pZmm);
                transforms.put(level, tr);
            }
        }

        transform.set(transforms.get(level));
    }

    @Override
    abstract public T getType();

    @Override
    public String getName() {
        if (getType() instanceof Volatile) {
            return this.sourceName+" (Volatile)";
        } else {
            return this.sourceName;
        }
    }

    public void setName(String newName) {
        this.sourceName = newName;
    }

    @Override
    public VoxelDimensions getVoxelDimensions() {
        return voxelsDimensions;
    }

    @Override
    public int getNumMipmapLevels() {
        if (numMipMapLevels == -1)
        {
            try
            {
                IFormatReader reader = readerPool.acquire();
                reader.setSeries( cSerie );
                numMipMapLevels = reader.getResolutionCount();
                readerPool.recycle( reader );
            } catch ( Exception e )
            {
                e.printStackTrace();
            }
        }
        return numMipMapLevels;
    }

    final public static String CONCRETE = "CONCRETE";
    final public static String VOLATILE = "VOLATILE";

    static public Type getBioformatsBdvSourceType(IFormatReader reader, int image_index) throws UnsupportedOperationException {
        final IMetadata omeMeta = (IMetadata) reader.getMetadataStore();
        reader.setSeries(image_index);
        if (reader.isRGB()) {
            if (omeMeta.getPixelsType(image_index)== PixelType.UINT8) {
                return new ARGBType();
            } else {
                throw new UnsupportedOperationException("Unhandled 16 bits RGB images");
            }
        } else {
            PixelType pt = omeMeta.getPixelsType(image_index);
            if  (pt == PixelType.UINT8) {return new UnsignedByteType();}
            if  (pt == PixelType.UINT16) {return new UnsignedShortType();}
            if  (pt == PixelType.UINT32) {return new UnsignedIntType();}
            if  (pt == PixelType.FLOAT) {return new FloatType();}
        }
        throw new UnsupportedOperationException("Unhandled pixel type for serie "+image_index+": "+omeMeta.getPixelsType(image_index));
    }

    static public Class<? extends BioFormatsBdvSource> getBioformatsBdvSourceClass(IFormatReader reader, int image_index) throws UnsupportedOperationException {
        final IMetadata omeMeta = (IMetadata) reader.getMetadataStore();
        reader.setSeries(image_index);
        if (reader.isRGB()) {
            if (omeMeta.getPixelsType(image_index)== PixelType.UINT8) {
                return BioFormatsBdvSourceRGB24bits.class;
            } else {
                throw new UnsupportedOperationException("Unhandled 16 bits RGB images");
            }
        } else {
            PixelType pt = omeMeta.getPixelsType(image_index);
            if  (pt == PixelType.UINT8) {return BioFormatsBdvSourceUnsignedByte.class;}
            if  (pt == PixelType.UINT16) {return BioFormatsBdvSourceUnsignedShort.class;}
            if  (pt == PixelType.UINT32) {return BioFormatsBdvSourceUnsignedInt.class;}
            if  (pt == PixelType.FLOAT) {return BioFormatsBdvSourceFloat.class;}
        }
        throw new UnsupportedOperationException("Unhandled pixel type for serie "+image_index+": "+omeMeta.getPixelsType(image_index));
    }

    static public Volatile getVolatileOf(NumericType t) {
        if (t instanceof UnsignedShortType) return new VolatileUnsignedShortType();

        if (t instanceof UnsignedIntType) return new VolatileUnsignedIntType();

        if (t instanceof UnsignedByteType) return new VolatileUnsignedByteType();

        if (t instanceof FloatType) return new VolatileFloatType();

        if (t instanceof ARGBType) return new VolatileARGBType();
        return null;
    }

}
