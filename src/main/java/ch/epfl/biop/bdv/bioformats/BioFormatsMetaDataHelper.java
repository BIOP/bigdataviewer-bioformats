package ch.epfl.biop.bdv.bioformats;

import ch.epfl.biop.bdv.bioformats.bioformatssource.BioFormatsBdvSource;
import loci.formats.*;
import loci.formats.meta.IMetadata;
import mpicbg.spim.data.sequence.VoxelDimensions;
import net.imglib2.Dimensions;
import net.imglib2.realtransform.AffineTransform3D;
import net.imglib2.type.numeric.ARGBType;
import net.imglib2.type.numeric.NumericType;
import net.imglib2.type.volatiles.VolatileARGBType;
import net.imglib2.type.volatiles.VolatileNumericType;
import ome.units.UNITS;
import ome.units.quantity.Length;
import ome.units.unit.Unit;

import java.awt.*;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.logging.Logger;

import org.apache.commons.lang3.tuple.MutablePair;
import org.apache.commons.lang3.tuple.Pair;

/**
 *
 */

public class BioFormatsMetaDataHelper {

    private static final Logger LOGGER = Logger.getLogger( BioFormatsMetaDataHelper.class.getName() );

    public static int getChannelHashFromBFMeta(IMetadata m, int iSerie, int iChannel) {
        // Iso Channel = identical Wavelength and pixel type
        int hash = 1;
        if (m.getChannelEmissionWavelength(iSerie,iChannel)!=null) {
            hash*=m.getChannelEmissionWavelength(iSerie,iChannel).value(UNITS.NANOMETER).intValue();
        }
        if (m.getChannelName(iSerie,iChannel)!=null) {
            hash*=m.getChannelName(iSerie,iChannel).hashCode() % 17;
        }
        if (m.getPixelsType(iSerie)!=null) {
            hash*=m.getPixelsType(iSerie).getValue().hashCode() % 17;
        }
        return hash;
    }

    public static double[] getPos(IMetadata omeMeta, int iSerie, Unit u) {
        double pXmm = 0, pYmm = 0, pZmm = 0; // Location of pixel located at coordinates 0,0,0
        // First : try with Physical dimension
        try {
            if (omeMeta.getPlaneCount(iSerie)>0) {
                pXmm = omeMeta.getPlanePositionX(iSerie, 0).value(u).doubleValue();
                pYmm = omeMeta.getPlanePositionY(iSerie, 0).value(u).doubleValue();
            }
        } catch(NullPointerException e) { // Beurk
            System.out.println("NullPointerException Caught : no physical units");
            try {
                pXmm = omeMeta.getPlanePositionX(iSerie, 0).value().doubleValue();
                pYmm = omeMeta.getPlanePositionY(iSerie, 0).value().doubleValue();
                System.out.println("NullPointerException Caught : no plane position");
            } catch (NullPointerException e2) { // Beurk
                pXmm=0;
                pYmm=0;
            }
        }

        // In 3D if available
        if (omeMeta.getPixelsPhysicalSizeZ(iSerie)==null) {
            pZmm=0;
        } else {
            try {
                pZmm = omeMeta.getPlanePositionZ(iSerie, 0).value(u).doubleValue();
            }  catch(NullPointerException e2) { // Beurk
                pZmm=0;
            }
        }
        return new double[]{pXmm,pYmm,pZmm};
    }

    public static double[] getVoxSize(IMetadata omeMeta, int iSerie, Unit u) {
        double dXmm, dYmm, dZmm; // Voxel size

        try {
            dXmm = omeMeta.getPixelsPhysicalSizeX(iSerie).value(u).doubleValue();
            dYmm = omeMeta.getPixelsPhysicalSizeY(iSerie).value(u).doubleValue();
        } catch(NullPointerException e1) { // Beurk
            //System.out.print("NullPointerException Caught");
                dXmm=1;
                dYmm=1;
        }

        // In 3D if available
        if (omeMeta.getPixelsPhysicalSizeZ(iSerie)==null) {
            dZmm=1; // Assuming isotropic
        } else {
            try {
                dZmm = omeMeta.getPixelsPhysicalSizeZ(iSerie).value(u).doubleValue();
            }  catch(NullPointerException e2) {
                dZmm=1; // Assuming isotropic
            }
        }
        return new double[]{dXmm, dYmm, dZmm};
    }

    public static AffineTransform3D getRootTransform(IMetadata omeMeta, int iSerie, Unit u, boolean isCenterConvention) {

        AffineTransform3D rootTransform = new AffineTransform3D();

        double[] p = getPos(omeMeta, iSerie, u);
        double[] d = getVoxSize(omeMeta, iSerie, u);

        // Sets AffineTransform of the highest resolution image from the pyramid
        rootTransform.identity();
        rootTransform.set(
                d[0],0   ,0   ,0,
                0   ,d[1],0   ,0,
                0   ,0   ,d[2],0,
                0   ,0   ,0   ,1
        );
        if (isCenterConvention) {
            Dimensions dims = getDimensions(omeMeta, iSerie, u);
            p[0]-=(dims.dimension(0)/2d)*d[0];
            p[1]-=(dims.dimension(1)/2d)*d[1];
            p[2]-=(dims.dimension(2)/2d)*d[2];
        }
        rootTransform.translate(p[0], p[1], p[2]);
        return rootTransform;
    }

    public static VoxelDimensions getVoxelDimensions(IMetadata omeMeta, int iSerie, Unit u) {
        // Always 3 to allow for big stitcher compatibility
        //int numDimensions = 2 + (omeMeta.getPixelsSizeZ(iSerie).getNumberValue().intValue()>1?1:0);
        int numDimensions = 3;

        double[] d = getVoxSize(omeMeta, iSerie, u);

        VoxelDimensions voxelDimensions;

        {
            assert numDimensions == 3;
            voxelDimensions = new VoxelDimensions() {

                final Unit<Length> targetUnit = u;

                double[] dims = {d[0],d[1],d[2]};

                @Override
                public String unit() {
                    return targetUnit.getSymbol();
                }

                @Override
                public void dimensions(double[] doubles) {
                    doubles[0] = dims[0];
                    doubles[1] = dims[1];
                    //if (numDimensions==3)
                    doubles[2] = dims[2];
                }

                @Override
                public double dimension(int i) {
                    return dims[i];
                }

                @Override
                public int numDimensions() {
                    return numDimensions;
                }
            };
        }
        return voxelDimensions;
    }

    public static Dimensions getDimensions(IMetadata omeMeta, int iSerie, Unit u) {
        // Always set 3d to allow for Big Stitcher compatibility
        //int numDimensions = 2 + (omeMeta.getPixelsSizeZ(iSerie).getNumberValue().intValue()>1?1:0);
        int numDimensions = 3;

        int sX = omeMeta.getPixelsSizeX(iSerie).getNumberValue().intValue();
        int sY = omeMeta.getPixelsSizeY(iSerie).getNumberValue().intValue();
        int sZ = omeMeta.getPixelsSizeZ(iSerie).getNumberValue().intValue();

        long[] dims = new long[3];
        dims[0] = sX;

        dims[1] = sY;

        dims[2] = sZ;

        Dimensions dimensions = new Dimensions() {
            @Override
            public void dimensions(long[] dimensions) {
                dimensions[0] = dims[0];
                dimensions[1] = dims[1];
                dimensions[2] = dims[2];
                //if (numDimensions==3) dimensions[2] = dims[2];
            }

            @Override
            public long dimension(int d) {
                return dims[d];
            }

            @Override
            public int numDimensions() {
                return numDimensions;
            }
        };

        return dimensions;
    }

    public static ArrayList<Pair<Integer, ArrayList<Integer>>> getListOfSeriesAndChannels(String dataLocation, String code) {

        IFormatReader readerIdx = new ImageReader();

        readerIdx.setFlattenedResolutions(false);
        Memoizer memo = new Memoizer( readerIdx );

        final IMetadata omeMetaOmeXml = MetadataTools.createOMEXMLMetadata();
        memo.setMetadataStore(omeMetaOmeXml);

        try {
            memo.setId( dataLocation );
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
        final IFormatReader reader = memo;
        ArrayList<Pair<Integer, ArrayList<Integer>>>
                listOfSources =
                commaSeparatedListToArrayOfArray(
                        code,
                        idxSeries ->(idxSeries>=0)?idxSeries:reader.getSeriesCount()+idxSeries, // apparently -1 is necessary -> I don't really understand
                        (idxSeries, idxChannel) ->
                                (idxChannel>=0)?idxChannel:omeMetaOmeXml.getChannelCount(idxSeries)+idxChannel
                );
        System.out.println("Number of series: "+reader.getSeriesCount());
        for (int i=0;i<reader.getSeriesCount();i++) {
            System.out.println("Number of channels series: "+i+" : "+omeMetaOmeXml.getChannelCount(i));
        }
        return listOfSources;
    }

    /**
     * BiFunction necessary to be able to find index of negative values
     */
    static public ArrayList<Pair<Integer, ArrayList<Integer>>> commaSeparatedListToArrayOfArray(String expression, Function<Integer, Integer> fbounds, BiFunction<Integer, Integer, Integer> f) {
        String[] splitIndexes = expression.split(";");

        ArrayList<Pair<Integer, ArrayList<Integer>>> arrayOfArrayOfIndexes = new ArrayList<>();

        for (String str : splitIndexes) {
            str.trim();
            String seriesIdentifier = str;
            String channelIdentifier = "*";
            if (str.contains(".")) {
                String[] boundIndex = str.split("\\.");
                if (boundIndex.length==2) {
                    seriesIdentifier = boundIndex[0];
                    channelIdentifier = boundIndex[1];
                } else {
                    LOGGER.warning("Number format problem with expression:"+str+" - Expression ignored");
                    break;
                }
            }
            // TODO Need to split by comma
            // No sub array specifier -> equivalent to * in subchannel
            try {
                if (seriesIdentifier.trim().equals("*")) {
                    int maxIndex = fbounds.apply(-1);
                    System.out.println("maxIndex="+maxIndex);
                    for (int index = 0; index <=maxIndex; index++) {
                        MutablePair<Integer, ArrayList<Integer>> current = new MutablePair<>();
                        final int idxCp = index;
                        current.setLeft(idxCp);
                        current.setRight(
                                expressionToArray(channelIdentifier, i -> f.apply(idxCp,i))
                        );
                        arrayOfArrayOfIndexes.add(current);
                    }
                } else {
                    int indexMin, indexMax;

                    if (seriesIdentifier.trim().contains(":")) {
                        String[] boundIndex = seriesIdentifier.split(":");
                        assert boundIndex.length==2;
                        indexMin = fbounds.apply(Integer.valueOf(boundIndex[0].trim()));
                        indexMax = fbounds.apply(Integer.valueOf(boundIndex[1].trim()));
                    } else {
                        indexMin = fbounds.apply(Integer.valueOf(seriesIdentifier.trim()));
                        indexMax = indexMin;
                    }
                    if (indexMax>=indexMin) {
                        for (int index=indexMin;index<=indexMax;index++) {
                            MutablePair<Integer, ArrayList<Integer>> current = new MutablePair<>();
                            final int idxCp = index;
                            current.setLeft(index);
                            current.setRight(
                                    expressionToArray(channelIdentifier, i -> f.apply(idxCp,i))
                            );
                            arrayOfArrayOfIndexes.add(current);
                        }
                    } else {
                        for (int index=indexMax;index>=indexMin;index--) {
                            MutablePair<Integer, ArrayList<Integer>> current = new MutablePair<>();
                            final int idxCp = index;
                            current.setLeft(index);
                            current.setRight(
                                    expressionToArray(channelIdentifier, i -> f.apply(idxCp,i))
                            );
                            arrayOfArrayOfIndexes.add(current);
                        }
                    }

                }
            } catch (NumberFormatException e) {
                LOGGER.warning("Number format problem with expression:"+str+" - Expression ignored");
            }

        }
        return arrayOfArrayOfIndexes;
    }

    /**
     * Convert a comma separated list of indexes into an arraylist of integer
     *
     * For instance 1,2,5:7,10:12,14 returns an ArrayList containing
     * [1,2,5,6,7,10,11,12,14]
     *
     * Invalid format are ignored and an error message is displayed
     *
     * @param expression
     * @return list of indexes in ArrayList
     */
    static public ArrayList<Integer> expressionToArray(String expression, Function<Integer, Integer> fbounds) {
        String[] splitIndexes = expression.split(",");
        ArrayList<Integer> arrayOfIndexes = new ArrayList<>();
        for (String str : splitIndexes) {
            str.trim();
            if (str.contains(":")) {
                // Array of source, like 2:5 = 2,3,4,5
                String[] boundIndex = str.split(":");
                if (boundIndex.length==2) {
                    try {
                        int b1 = fbounds.apply(Integer.valueOf(boundIndex[0].trim()));
                        int b2 = fbounds.apply(Integer.valueOf(boundIndex[1].trim()));
                        if (b1<b2) {
                            for (int index = b1; index <= b2; index++) {
                                arrayOfIndexes.add(index);
                            }
                        }  else {
                            for (int index = b2; index >= b1; index--) {
                                arrayOfIndexes.add(index);
                            }
                        }
                    } catch (NumberFormatException e) {
                        LOGGER.warning("Number format problem with expression:"+str+" - Expression ignored");
                    }
                } else {
                    LOGGER.warning("Cannot parse expression "+str+" to pattern 'begin-end' (2-5) for instance, omitted");
                }
            } else {
                // Single source
                try {
                    if (str.trim().equals("*")) {
                        int maxIndex = fbounds.apply(-1);
                        for (int index = 0; index <=maxIndex; index++) {
                            arrayOfIndexes.add(index);
                        }
                    } else {
                        int index = fbounds.apply(Integer.valueOf(str.trim()));
                        arrayOfIndexes.add(index);
                    }
                } catch (NumberFormatException e) {
                    LOGGER.warning("Number format problem with expression:"+str+" - Expression ignored");
                }
            }
        }
        return arrayOfIndexes;
    }

    public static ARGBType getSourceColor(BioFormatsBdvSource src) {
        // Get color based on emission wavelength
        IFormatReader reader = src.getReader();

        final IMetadata omeMetaIdxOmeXml = MetadataTools.createOMEXMLMetadata();
        reader.setMetadataStore(omeMetaIdxOmeXml);
        ARGBType color = null;
        if ((src.getType() instanceof ARGBType)||(src.getType() instanceof VolatileARGBType)) {

        } else {
            if ((src.getType() instanceof NumericType)||(src.getType() instanceof VolatileNumericType)) {

                ome.xml.model.primitives.Color c = omeMetaIdxOmeXml.getChannelColor(src.cSerie, src.cChannel);
                if (c != null) {
                    color = new ARGBType(ARGBType.rgba(c.getRed(), c.getGreen(), c.getBlue(), 255));
                } else {
                    if (omeMetaIdxOmeXml.getChannelEmissionWavelength(src.cSerie, src.cChannel) != null) {
                        int emission = omeMetaIdxOmeXml.getChannelEmissionWavelength(src.cSerie, src.cChannel)
                                .value(UNITS.NANOMETER)
                                .intValue();
                        Color cAwt = getColorFromWavelength(emission);
                        color = new ARGBType(ARGBType.rgba(cAwt.getRed(), cAwt.getGreen(), cAwt.getBlue(), 255));
                    }
                }
            }
        }
        return color;
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

    /**
     * Look into Fields of BioFormats UNITS class that matches the input string
     * Return the corresponding Unit Field
     * Case insensitive
     * @param unit
     * @return
     */
    public static Unit getUnitFromString(String unit) {
        Field[] bfUnits = UNITS.class.getFields();
        for (Field f:bfUnits) {
            if (f.getType().equals(Unit.class)) {
                if (f.getName().toUpperCase().equals(unit.trim().toUpperCase())) {
                    try {
                        // Field found
                        Unit u = (Unit) f.get(null); // Field is assumed to be static
                        return u;
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }
        }
        // Field not found
        return null;
    }


}
