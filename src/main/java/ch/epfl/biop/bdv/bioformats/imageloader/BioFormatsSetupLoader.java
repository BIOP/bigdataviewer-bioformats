/*-
 * #%L
 * Commands and function for opening, conversion and easy use of bioformats format into BigDataViewer
 * %%
 * Copyright (C) 2019 - 2022 Nicolas Chiaruttini, BIOP, EPFL
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
package ch.epfl.biop.bdv.bioformats.imageloader;

import bdv.AbstractViewerSetupImgLoader;
import bdv.viewer.Source;
import ch.epfl.biop.bdv.bioformats.bioformatssource.BioFormatsBdvOpener;
import ch.epfl.biop.bdv.bioformats.bioformatssource.BioFormatsBdvSource;
import ch.epfl.biop.bdv.bioformats.bioformatssource.ReaderPool;
import mpicbg.spim.data.generic.sequence.ImgLoaderHint;
import mpicbg.spim.data.sequence.MultiResolutionSetupImgLoader;
import mpicbg.spim.data.sequence.VoxelDimensions;
import net.imglib2.*;
import net.imglib2.converter.Converter;
import net.imglib2.converter.Converters;
import net.imglib2.realtransform.AffineTransform3D;
import net.imglib2.type.numeric.ARGBType;
import net.imglib2.type.numeric.NumericType;
import net.imglib2.type.numeric.integer.AbstractIntegerType;
import net.imglib2.type.numeric.real.FloatType;

import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Function;

public class BioFormatsSetupLoader<T extends NumericType<T>,V extends Volatile<T> & NumericType<V>> extends AbstractViewerSetupImgLoader<T, V> implements MultiResolutionSetupImgLoader< T > {

    public Source<T> concreteSource;

    public Source<V> volatileSource;

    int[] cellDimensions;

    Function<RandomAccessibleInterval<T>, RandomAccessibleInterval<FloatType>> cvtRaiToFloatRai;

    final Converter<T,FloatType> cvt;

    Consumer<String> errlog = s -> System.err.println(BioFormatsSetupLoader.class+" error:"+s);

    public ReaderPool getReaderPool() {
        return ((BioFormatsBdvSource) concreteSource).getReaderPool();
    }

    final BioFormatsBdvOpener opener;

    public BioFormatsBdvOpener getOpener() { return opener; }

    final public int iSerie,iChannel;

    public BioFormatsSetupLoader(BioFormatsBdvOpener opener,
                                 int sourceIndex,
                                 int channelIndex,
                                 T t,
                                 V v) {
        super(t, v);

        this.opener = opener;
        iSerie = sourceIndex;
        iChannel = channelIndex;

        Map<String, Source> sources = opener.getConcreteAndVolatileSources(sourceIndex, channelIndex);

        concreteSource = (Source<T>) sources.get(BioFormatsBdvSource.CONCRETE);
        volatileSource = (Source<V>) sources.get(BioFormatsBdvSource.VOLATILE);

        cellDimensions = ((BioFormatsBdvSource) concreteSource).cellDimensions;

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
        return volatileSource.getSource(timepointId,level);
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
                concreteSource.getSource(timepointId,level).dimensions(dimensions);
            }

            @Override
            public long dimension(int d) {
                return concreteSource.getSource(timepointId,level).dimension(d);
            }

            @Override
            public int numDimensions() {
                return concreteSource.getSource(timepointId,level).numDimensions();
            }
        };
    }

    @Override
    public RandomAccessibleInterval<T> getImage(int timepointId, int level, ImgLoaderHint... hints) {
        return concreteSource.getSource(timepointId,level);
    }

    @Override
    public double[][] getMipmapResolutions() {
        // Needs to compute mipmap resolutions... pfou
        int numMipmapLevels = concreteSource.getNumMipmapLevels();
        double [][] mmResolutions = new double[ numMipmapLevels ][3];
        mmResolutions[0][0]=1;
        mmResolutions[0][1]=1;
        mmResolutions[0][2]=1;

        RandomAccessibleInterval srcL0 = concreteSource.getSource(0,0);
        for ( int iLevel = 1; iLevel< numMipmapLevels; iLevel++) {
            RandomAccessibleInterval srcLi = concreteSource.getSource(0,iLevel);
            mmResolutions[iLevel][0] = (double)srcL0.dimension(0)/(double)srcLi.dimension(0);
            mmResolutions[iLevel][1] = (double)srcL0.dimension(1)/(double)srcLi.dimension(1);
            mmResolutions[iLevel][2] = (double)srcL0.dimension(2)/(double)srcLi.dimension(2);
        }
        return mmResolutions;
    }

    @Override
    public AffineTransform3D[] getMipmapTransforms() {
        AffineTransform3D[] ats = new AffineTransform3D[concreteSource.getNumMipmapLevels()];

        for (int iLevel = 0; iLevel< concreteSource.getNumMipmapLevels(); iLevel++) {
            AffineTransform3D at = new AffineTransform3D();
            concreteSource.getSourceTransform(0,iLevel,at);
            ats[iLevel] = at;
        }

        return ats;
    }

    @Override
    public int numMipmapLevels() {
        return concreteSource.getNumMipmapLevels();
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
        return concreteSource.getVoxelDimensions();
    }

}
