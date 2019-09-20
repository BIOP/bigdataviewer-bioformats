package ch.epfl.biop.bdv.bioformats.export;

import loci.formats.meta.IMetadata;
import mpicbg.spim.data.sequence.VoxelDimensions;
import net.imglib2.Dimensions;
import net.imglib2.realtransform.AffineTransform3D;
import ome.units.UNITS;
import ome.units.quantity.Length;
import ome.units.unit.Unit;

public class BioFormatsToXmlUtils {

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
            dZmm=1;
        } else {
            try {
                dZmm = omeMeta.getPixelsPhysicalSizeZ(iSerie).value(u).doubleValue();
            }  catch(NullPointerException e2) {
                dZmm=1;
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
