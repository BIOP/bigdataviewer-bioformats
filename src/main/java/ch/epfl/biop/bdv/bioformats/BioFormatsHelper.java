package ch.epfl.biop.bdv.bioformats;

import loci.formats.IFormatReader;
import loci.formats.meta.IMetadata;
import mpicbg.spim.data.sequence.VoxelDimensions;
import net.imglib2.Dimensions;
import net.imglib2.realtransform.AffineTransform3D;
import ome.units.UNITS;
import ome.units.quantity.Length;
import ome.units.unit.Unit;
import ome.xml.model.enums.PixelType;

import java.util.function.Consumer;
import java.util.logging.Logger;

/**
 * Inspired from https://github.com/qupath/qupath-bioformats-extension/blob/master/src/main/java/qupath/lib/images/servers/BioFormatsImageServer.java
 */

public class BioFormatsHelper {


    private static final Logger LOGGER = Logger.getLogger( BioFormatsHelper.class.getName() );

    /**
     * Manager to help keep multithreading under control.
     */

    public int nDimensions;

    final public boolean is8bits;

    final public boolean is16bits;

    final public boolean is32bits;

    final public boolean isFloat32bits;

    final public boolean is24bitsRGB;

    public BioFormatsHelper(IFormatReader reader, int image_index) throws Exception {

        final IMetadata omeMeta = (IMetadata) reader.getMetadataStore();
        reader.setSeries(image_index);
        is24bitsRGB = (reader.isRGB());
        is8bits = (omeMeta.getPixelsType(image_index) == PixelType.UINT8)&&(!is24bitsRGB);
        is16bits = (omeMeta.getPixelsType(image_index) == PixelType.UINT16)&&(!is24bitsRGB);
        is32bits = (omeMeta.getPixelsType(image_index) == PixelType.UINT32)&&(!is24bitsRGB);
        isFloat32bits = (omeMeta.getPixelsType(image_index) == PixelType.FLOAT)&&(!is24bitsRGB);

        Consumer<String> log = (s) -> {};// LOGGER.info(s);

        log.accept("getchannelsamples="+omeMeta.getChannelSamplesPerPixel(image_index, 0));
        log.accept("getchannelcount="+omeMeta.getChannelCount(image_index));
        log.accept("reader.getImageCount()="+reader.getImageCount());
        log.accept("reader.isRGB()="+reader.isRGB());
        log.accept("reader.getOptimalTileHeight()="+reader.getOptimalTileHeight());
        log.accept("reader.getOptimalTileWidth()="+reader.getOptimalTileWidth());
        log.accept("reader.isInterleaved()="+reader.isInterleaved());
    }

    public String toString() {
        String str = "Img \n";
        str+="nDimensions = "+nDimensions;
        return str;
    }


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
        double dXmm = 1, dYmm = 1, dZmm = 1; // Voxel size

        try {
            dXmm = omeMeta.getPixelsPhysicalSizeX(iSerie).value(u).doubleValue();
            dYmm = omeMeta.getPixelsPhysicalSizeY(iSerie).value(u).doubleValue();
        } catch(NullPointerException e1) { // Beurk
            //System.out.print("NullPointerException Caught");
            try {
                dXmm = omeMeta.getPixelsSizeX(iSerie).getNumberValue().doubleValue();
                dYmm = omeMeta.getPixelsSizeY(iSerie).getNumberValue().doubleValue();
            } catch(NullPointerException e2) { // Beurk
                //System.out.print("NullPointerException Caught");
                dXmm=1;
                dYmm=1;
            }
        }

        // In 3D if available
        if (omeMeta.getPixelsPhysicalSizeZ(iSerie)==null) {
            dZmm=dXmm; // Assuming isotropic
        } else {
            try {
                dZmm = omeMeta.getPixelsPhysicalSizeZ(iSerie).value(u).doubleValue();
            }  catch(NullPointerException e2) {
                dZmm=dXmm; // Assuming isotropic
            }
        }
        return new double[]{dXmm, dYmm, dZmm};
    }

    public static AffineTransform3D getRootTransform(IMetadata omeMeta, int iSerie, Unit u) {

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
}
