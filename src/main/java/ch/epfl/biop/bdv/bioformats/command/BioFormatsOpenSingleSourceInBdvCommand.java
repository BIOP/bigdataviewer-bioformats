package ch.epfl.biop.bdv.bioformats.command;

import bdv.util.BdvFunctions;
import bdv.util.BdvHandle;
import bdv.util.BdvOptions;
import bdv.util.BdvStackSource;
import bdv.util.volatiles.SharedQueue;
import bdv.viewer.Source;
import ch.epfl.biop.bdv.bioformats.BioFormatsHelper;
import ch.epfl.biop.bdv.bioformats.Units;
import ch.epfl.biop.bdv.bioformats.bioformatssource.*;
import loci.formats.*;
import loci.formats.meta.IMetadata;
import net.imglib2.FinalInterval;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.type.numeric.ARGBType;
import net.imglib2.type.numeric.RealType;
import net.imglib2.type.numeric.integer.UnsignedByteType;
import net.imglib2.type.numeric.integer.UnsignedIntType;
import net.imglib2.type.numeric.integer.UnsignedShortType;
import net.imglib2.type.numeric.real.FloatType;
import net.imglib2.type.volatiles.*;
import net.imglib2.util.Util;
import net.imglib2.view.Views;
import ome.units.UNITS;
import ome.units.quantity.Length;
import ome.units.unit.Unit;
import org.scijava.ItemIO;
import org.scijava.command.Command;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;

import java.awt.*;
import java.io.File;
import java.util.logging.Logger;

@Plugin(type = Command.class,menuPath = "BDV_SciJava>Open>Open single source with BioFormats in Bdv")
public class BioFormatsOpenSingleSourceInBdvCommand implements Command {
    private static final Logger LOGGER = Logger.getLogger( BioFormatsOpenSingleSourceInBdvCommand.class.getName() );

    @Parameter(label = "Image File")
    public File inputFile;

    @Parameter(label = "Open in new BigDataViewer window")
    public boolean createNewWindow;

    // ItemIO.BOTH required because it can be modified in case of appending new data to BDV (-> requires INPUT), or created (-> requires OUTPUT)
    @Parameter(label = "BigDataViewer Frame", type = ItemIO.BOTH, required = false)
    public BdvHandle bdv_h;

    @Parameter(type = ItemIO.OUTPUT)
    public Source bdvSrc;

    @Parameter(type = ItemIO.OUTPUT)
    public VolatileBdvSource<?, ?> vSrc;

    @Parameter(label="Display type ()", choices = {"Volatile","Standard","No Show"})
    public String appendMode = "Volatile";

    @Parameter
    public int sourceIndex;

    @Parameter
    public int channelIndex;

    @Parameter
    public boolean switchZandC = false;

    @Parameter
    public boolean autoscale = true;

    @Parameter
    public boolean keepBdv3d = false;

    @Parameter
    public boolean letBioFormatDecideCacheBlockXY = true;

    @Parameter
    public boolean ignoreMetadata = true;

    @Parameter
    public int cacheBlockSizeX = 512;

    @Parameter
    public int cacheBlockSizeY = 512;

    @Parameter
    public int cacheBlockSizeZ = 32;

    @Parameter( choices = { Units.MILLIMETERS, Units.MICRONS } )
    public String unit = Units.MICRONS;

    @Override
    public void run()
    {
        BdvOptions options = BdvOptions.options();

        if (createNewWindow) {
            bdv_h=null;
        }

        if (createNewWindow == false && bdv_h!=null) {
            options.addTo(bdv_h);
        }
        try {

            Unit< Length > unit = Units.getLengthUnit( this.unit );

            IFormatReader reader = new ImageReader();
            reader.setFlattenedResolutions(false);
            Memoizer memo = new Memoizer( reader );
            final IMetadata omeMetaIdxOmeXml = MetadataTools.createOMEXMLMetadata();
            memo.setMetadataStore(omeMetaIdxOmeXml);
            memo.setId( inputFile.getAbsolutePath() );
            final IFormatReader readerIdx = memo;

            bdvSrc = null;

            LOGGER.info("src idx = "+sourceIndex);
            LOGGER.info("ch idx = "+channelIndex);
            BioFormatsHelper h = new BioFormatsHelper(readerIdx, sourceIndex);
            vSrc = null;

            FinalInterval cacheBlockSize = new FinalInterval(new long[]
                           {(long)cacheBlockSizeX,
                            (long)cacheBlockSizeY,
                            (long)cacheBlockSizeZ});

            if (h.is24bitsRGB) {
                bdvSrc = new BioFormatsBdvRGBSource(readerIdx, sourceIndex, channelIndex, switchZandC, cacheBlockSize, letBioFormatDecideCacheBlockXY, ignoreMetadata, ignoreMetadata,unit);
                vSrc = new VolatileBdvSource<ARGBType, VolatileARGBType>(bdvSrc, new VolatileARGBType(), new SharedQueue(1));
            } else {
                if (h.is8bits)  {
                    bdvSrc = new BioFormatsBdvUnsignedByteSource(readerIdx, sourceIndex, channelIndex, switchZandC, cacheBlockSize, letBioFormatDecideCacheBlockXY, ignoreMetadata, ignoreMetadata, unit);
                    vSrc = new VolatileBdvSource<UnsignedByteType, VolatileUnsignedByteType>(bdvSrc, new VolatileUnsignedByteType(), new SharedQueue(1));
                }
                if (h.is16bits) {
                    bdvSrc = new BioFormatsBdvUnsignedShortSource(readerIdx, sourceIndex, channelIndex, switchZandC, cacheBlockSize, letBioFormatDecideCacheBlockXY, ignoreMetadata, ignoreMetadata, unit);
                    vSrc = new VolatileBdvSource<UnsignedShortType, VolatileUnsignedShortType>(bdvSrc, new VolatileUnsignedShortType(), new SharedQueue(1));
                }
                if (h.is32bits) {
                    bdvSrc = new BioFormatsBdvUnsignedIntSource(readerIdx, sourceIndex, channelIndex, switchZandC, cacheBlockSize, letBioFormatDecideCacheBlockXY, ignoreMetadata, ignoreMetadata, unit);
                    vSrc = new VolatileBdvSource<UnsignedIntType, VolatileUnsignedIntType>(bdvSrc, new VolatileUnsignedIntType(), new SharedQueue(1));
                }
                if (h.isFloat32bits) {
                    bdvSrc = new BioFormatsBdvFloatSource(readerIdx, sourceIndex, channelIndex, switchZandC, cacheBlockSize, letBioFormatDecideCacheBlockXY, ignoreMetadata, ignoreMetadata, unit);
                    vSrc = new VolatileBdvSource<FloatType, VolatileFloatType>(bdvSrc, new VolatileFloatType(), new SharedQueue(1));
                }
            }

            if (vSrc==null) {
                LOGGER.severe("Couldn't display source type. UnsignedShort, UnsignedByte, UnsignedInt, Float, and 24 bit RGB only are supported. ");
                return;
            }

            LOGGER.info("name=" + omeMetaIdxOmeXml.getChannelName(sourceIndex, channelIndex));

            BdvOptions opts = BdvOptions.options();
            if ((keepBdv3d==false)) opts = opts.is2D();
            if (bdv_h != null) opts = opts.addTo(bdv_h);
            BdvStackSource<?> bdvstack;
            switch (appendMode) {
                case "Volatile":
                    bdvstack = BdvFunctions.show(vSrc, opts);
                    break;
                case "Standard":
                    bdvstack = BdvFunctions.show(bdvSrc, opts);
                    break;
                case "No Show":
                    bdvstack=null;
                    break;
                default:
                    LOGGER.info("Invalid append mode: "+appendMode);
                    return;
            }
            if (bdvstack!=null) {
                if (!h.is24bitsRGB) {
                    ome.xml.model.primitives.Color c = omeMetaIdxOmeXml.getChannelColor(sourceIndex, channelIndex);
                    if (c != null) {
                        bdvstack.setColor(new ARGBType(ARGBType.rgba(c.getRed(), c.getGreen(), c.getBlue(), 255)));
                    } else {
                        if (omeMetaIdxOmeXml.getChannelEmissionWavelength(sourceIndex, channelIndex) != null) {
                            int emission = omeMetaIdxOmeXml.getChannelEmissionWavelength(sourceIndex, channelIndex).value(UNITS.NANOMETER).intValue();
                            LOGGER.info("emission=" + emission + " nm");
                            Color cAwt = getColorFromWavelength(emission);
                            bdvstack.setColor(new ARGBType(ARGBType.rgba(cAwt.getRed(), cAwt.getGreen(), cAwt.getBlue(), 255)));
                        }
                    }
                }

                if ((!h.is24bitsRGB) && (autoscale)) {
                    // autoscale attempt based on min max of last pyramid -> no scaling of RGB image
                    RandomAccessibleInterval<RealType> rai = bdvSrc.getSource(0, bdvSrc.getNumMipmapLevels() - 1);
                    RealType vMax = Util.getTypeFromInterval(rai);
                    if (rai.max(0) * rai.max(1) * rai.max(2) > (long) (1024 * 1024)) {
                        LOGGER.info("Image too big, autoscale is quick and dirty...");
                        rai = Views.interval(rai, new FinalInterval(rai.max(0) / 5, rai.max(1) / 5, 1));
                    }
                    for (RealType px : Views.flatIterable(rai)) {
                        if (px.compareTo(vMax) > 0) {
                            vMax.setReal(px.getRealDouble());
                        }
                    }
                    // TODO understand why min do not work
                    bdvstack.setDisplayRange(0, vMax.getRealDouble());
                }

                bdv_h = bdvstack.getBdvHandle();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    static private double Gamma = 0.80;
    static private double IntensityMax = 255;

    /** Taken from Earl F. Glynn's web page:
     * <a href="http://www.efg2.com/Lab/ScienceAndEngineering/Spectra.htm">Spectra Lab Report</a>
     * */
    public static int[] waveLengthToRGB(double Wavelength){
        double factor;
        double Red,Green,Blue;

        if((Wavelength >= 380) && (Wavelength<440)){
            Red = -(Wavelength - 440) / (440 - 380);
            Green = 0.0;
            Blue = 1.0;
        }else if((Wavelength >= 440) && (Wavelength<490)){
            Red = 0.0;
            Green = (Wavelength - 440) / (490 - 440);
            Blue = 1.0;
        }else if((Wavelength >= 490) && (Wavelength<510)){
            Red = 0.0;
            Green = 1.0;
            Blue = -(Wavelength - 510) / (510 - 490);
        }else if((Wavelength >= 510) && (Wavelength<580)){
            Red = (Wavelength - 510) / (580 - 510);
            Green = 1.0;
            Blue = 0.0;
        }else if((Wavelength >= 580) && (Wavelength<645)){
            Red = 1.0;
            Green = -(Wavelength - 645) / (645 - 580);
            Blue = 0.0;
        }else if((Wavelength >= 645) && (Wavelength<781)){
            Red = 1.0;
            Green = 0.0;
            Blue = 0.0;
        }else{
            Red = 0.0;
            Green = 0.0;
            Blue = 0.0;
        };

        // Let the intensity fall off near the vision limits

        if((Wavelength >= 380) && (Wavelength<420)){
            factor = 0.3 + 0.7*(Wavelength - 380) / (420 - 380);
        }else if((Wavelength >= 420) && (Wavelength<701)){
            factor = 1.0;
        }else if((Wavelength >= 701) && (Wavelength<781)){
            factor = 0.3 + 0.7*(780 - Wavelength) / (780 - 700);
        }else{
            factor = 0.0;
        };


        int[] rgb = new int[3];

        // Don't want 0^x = 1 for x <> 0
        rgb[0] = Red==0.0 ? 0 : (int) Math.round(IntensityMax * Math.pow(Red * factor, Gamma));
        rgb[1] = Green==0.0 ? 0 : (int) Math.round(IntensityMax * Math.pow(Green * factor, Gamma));
        rgb[2] = Blue==0.0 ? 0 : (int) Math.round(IntensityMax * Math.pow(Blue * factor, Gamma));

        return rgb;
    }


    public static Color getColorFromWavelength(int wv) {
        //https://stackoverflow.com/questions/1472514/convert-light-frequency-to-rgb
        int[] res = waveLengthToRGB(wv);
        return new Color(res[0], res[1], res[2]);
    }

}
