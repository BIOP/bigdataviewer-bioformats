package ch.epfl.biop.bdv.bioformats.export.ometiff;

import bdv.util.BdvHandle;
import bdv.viewer.Source;
import loci.common.DebugTools;
import loci.common.services.ServiceFactory;
import loci.formats.FormatTools;
import loci.formats.IFormatWriter;
import loci.formats.ImageWriter;
import loci.formats.ome.OMEPyramidStore;
import loci.formats.services.OMEXMLService;
import loci.formats.tiff.IFD;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.Volatile;
import net.imglib2.type.numeric.ARGBType;
import net.imglib2.type.numeric.NumericType;
import net.imglib2.type.numeric.integer.UnsignedByteType;
import net.imglib2.type.numeric.integer.UnsignedIntType;
import net.imglib2.type.numeric.integer.UnsignedShortType;
import net.imglib2.type.numeric.real.FloatType;
import net.imglib2.view.Views;
import ome.xml.model.enums.DimensionOrder;
import ome.xml.model.enums.PixelType;
import ome.xml.model.primitives.PositiveInteger;
import org.scijava.command.Command;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import static ch.epfl.biop.bdv.scijava.command.Info.ScijavaBdvRootMenu;

/**
 * Inspired from https://github.com/ome/bio-formats-examples/blob/master/src/main/java/FileExport.java
 *
 * JUST NOT WORKING AT ALL WITH TILES
 */

@Plugin(type = Command.class,menuPath = ScijavaBdvRootMenu+"Export>Save Sources as OMETIFF (SciJava)")
public class BioFormatsExportBdvToOmeTiff implements Command{

    private static final Logger LOGGER = Logger.getLogger( BioFormatsExportBdvToOmeTiff.class.getName() );

    @Parameter(label="Sources to save ('2,3-5'), starts at 0")
    String index_srcs_to_save;

    @Parameter(label = "BigDataViewer Frame")
    public BdvHandle bdv_h;

    /** The file format writer. */
    private IFormatWriter writer;

    /** The name of the output file. */
    @Parameter(label = "output file, ome tiff format")
    public File outputFile;

    @Parameter(label = "compute pyramid")
    public boolean computePyramid;

    @Parameter
    int tileSizeX = 512;

    @Parameter
    int tileSizeY = 512;

    @Parameter(label = "pyramid scale factor (XY only)")
    public int scale = 4;

    @Parameter(label = "number of resolutions")
    public int resolutions = 4;

    @Parameter(label = "time point")
    public int timePoint = 4;

    @Override
    public void run() {
        //DebugTools.enableLogging("INFO");
        DebugTools.setRootLevel("OFF");
        ArrayList<Integer> idx_src = expressionToArray(index_srcs_to_save, i -> {
            if (i>=0) {
                return i;
            } else {
                return bdv_h.getViewerPanel().getState().getSources().size()+i;
            }});

        List<Source<?>> srcs = idx_src
                .stream()
                .map(idx -> bdv_h.getViewerPanel().getState().getSources().get(idx).getSpimSource())
                .collect(Collectors.toList());

        try {
            ServiceFactory factory = new ServiceFactory();
            OMEXMLService service = factory.getInstance(OMEXMLService.class);
            OMEPyramidStore meta = (OMEPyramidStore) service.createOMEXMLMetadata();
            meta.createRoot();

            // Let's generate the metadata first
            int iImage = 0;
            Source<?> src = srcs.get(iImage);
            if (src.getName()!=null) {
                meta.setImageName(src.getName(), iImage);
            } else {
                meta.setImageName("No Name", iImage);
            }
            meta.setImageID("Image:"+iImage, iImage);
            meta.setPixelsID("Pixels:"+iImage, iImage);

            // specify that the pixel data is stored in big-endian format
            // change 'TRUE' to 'FALSE' to specify little-endian format
            meta.setPixelsBinDataBigEndian(Boolean.TRUE, iImage, 0);

            // specify that the images are stored in ZCT order
            meta.setPixelsDimensionOrder(DimensionOrder.XYZCT, iImage);

            long sizeX = src.getSource(timePoint,0).dimension(0);
            long sizeY = src.getSource(timePoint,0).dimension(1);
            long sizeZ = src.getSource(timePoint,0).dimension(2);

            int type;
            boolean isRGB = false;
            String pt;
            NumericType nt;
            // specify that the pixel type of the images
            if (src.getType() instanceof Volatile) {
                System.err.println("Volatile unsupported");
                cleanup();
                return;
            } else if (src.getType() instanceof UnsignedByteType) {
                nt = new UnsignedByteType();
                type = FormatTools.UINT8;
                pt = FormatTools.getPixelTypeString(type);
                meta.setPixelsType(PixelType.fromString(pt), iImage);
            } else if (src.getType() instanceof UnsignedShortType) {
                nt = new UnsignedShortType();
                type = FormatTools.UINT16;
                pt = FormatTools.getPixelTypeString(type);
                meta.setPixelsType(PixelType.fromString(pt), iImage);
            } else if (src.getType() instanceof UnsignedIntType) {
                nt = new UnsignedIntType();
                type = FormatTools.UINT32;
                pt = FormatTools.getPixelTypeString(type);
                meta.setPixelsType(PixelType.fromString(pt), iImage);
            } else if (src.getType() instanceof FloatType) {
                nt = new FloatType();
                type = FormatTools.FLOAT;
                pt = FormatTools.getPixelTypeString(type);
                meta.setPixelsType(PixelType.fromString(pt), iImage);
            } else if (src.getType() instanceof ARGBType) {
                nt = new ARGBType();
                type = FormatTools.UINT8;
                pt = FormatTools.getPixelTypeString(type); isRGB=true;
                meta.setPixelsType(PixelType.fromString(pt), iImage);
            } else {
                System.err.println("Pixel type unsupported");
                cleanup();
                return;
            }

            assert sizeX<Integer.MAX_VALUE;
            assert sizeY<Integer.MAX_VALUE;

            // specify the dimensions of the images
            meta.setPixelsSizeX(new PositiveInteger((int)sizeX), iImage);
            meta.setPixelsSizeY(new PositiveInteger((int)sizeY), iImage);
            meta.setPixelsSizeZ(new PositiveInteger((int)sizeZ), iImage);
            meta.setPixelsSizeC(new PositiveInteger(1), iImage);
            meta.setPixelsSizeT(new PositiveInteger(1), iImage);

            // define each channel and specify the number of samples in the channel
            // the number of samples is 3 for RGB images and 1 otherwise
            meta.setChannelID("Channel:"+iImage+":0", iImage, 0);
            meta.setChannelSamplesPerPixel(new PositiveInteger(isRGB?3:1), iImage, 0);

            for (int i=0;i<resolutions;i++) {
                int divScale = (int) Math.pow(scale,i);
                System.out.println("divscale="+divScale);
                System.out.println(new PositiveInteger((int) (sizeX/divScale)).getNumberValue().doubleValue());
                System.out.println(new PositiveInteger((int) (sizeY/divScale)).getNumberValue().doubleValue());
                meta.setResolutionSizeX(new PositiveInteger((int) (sizeX/divScale)),iImage,i);
                meta.setResolutionSizeY(new PositiveInteger((int) (sizeY/divScale)),iImage,i);
            }

            writer = new ImageWriter();//new PyramidOMETiffWriter();
            writer.setMetadataRetrieve(meta);
            writer.setTileSizeX(tileSizeX);
            writer.setTileSizeY(tileSizeY);
            writer.setId(outputFile.getAbsolutePath());

            iImage = 0;
            src = srcs.get(iImage);

            // ----------------

            writer.setSeries(iImage);
            IFD ifd = new IFD();
            ifd.put(IFD.TILE_WIDTH, tileSizeX);
            ifd.put(IFD.TILE_LENGTH, tileSizeY);
            writer.setResolution(0);
                int width = (int) sizeX;
                int height = (int) sizeY;

                // Determined the number of tiles to read and write
                int nXTiles = width / tileSizeX;
                int nYTiles = height / tileSizeY;
                if (nXTiles * tileSizeX != width) nXTiles++;
                if (nYTiles * tileSizeY != height) nYTiles++;

                for (int y=0; y<nYTiles; y++) {
                    for (int x=0; x<nXTiles; x++) {

                        System.out.println("X = "+y+"/"+nYTiles);
                        System.out.println("X = "+x+"/"+nXTiles);
                        // The x and y coordinates for the current tile
                        int tileX = x * tileSizeX;
                        int tileY = y * tileSizeY;

                        /* overlapped-tiling-example-start */
                        // If the last tile row or column overlaps the image size then only a partial tile
                        // is read or written. The tile size used is adjusted to account for any overlap.
                        int effTileSizeX = (tileX + tileSizeX) < width ? tileSizeX : width - tileX;
                        int effTileSizeY = (tileY + tileSizeY) < height ? tileSizeY : height - tileY;

                        // Read tiles from the input file and write them to the output OME-Tiff
                        //buf = reader.openBytes(image, tileX, tileY, effTileSizeX, effTileSizeY);

                        byte[] arrayToSave;
                        if (nt instanceof UnsignedByteType) {
                            RandomAccessibleInterval rai = src.getSource(timePoint, 0);
                            rai = Views.interval(rai, new long[] { x*tileSizeX, y*tileSizeY, 0 },
                                    new long[]{ x*tileSizeX+effTileSizeX, y*tileSizeY+effTileSizeY,0 } );
                            arrayToSave = SourceToByteArray.raiUnsignedByteTypeToByteArray(rai, new UnsignedByteType());
                        } else if (nt instanceof UnsignedShortType) {
                            RandomAccessibleInterval rai = src.getSource(timePoint, 0);
                            rai = Views.interval(rai, new long[] { x*tileSizeX, y*tileSizeY, 0 },
                                    new long[]{ x*tileSizeX+effTileSizeX, y*tileSizeY+effTileSizeY, 0 } );
                            arrayToSave = SourceToByteArray.raiUnsignedShortTypeToByteArray(rai, new UnsignedShortType());
                        } else {
                            System.err.println("Pixel type unsupported");
                            cleanup();
                            return;
                        }
                        System.out.println("arrayToSave length = "+arrayToSave.length);

                        writer.saveBytes(iImage, arrayToSave, tileX, tileY, effTileSizeX, effTileSizeY);
                        /* overlapped-tiling-example-end */
                    }
                }

            // ----------------

            //writer.saveBytes(iImage,arrayToSave);
            /*
            IImageScaler scaler = new SimpleImageScaler();

            for (int i=1;i<resolutions;i++) {
                writer.setResolution(i);
                //int divScale = (int) Math.pow(scale,i);
                int x = meta.getResolutionSizeX(iImage,i).getValue();
                int y = meta.getResolutionSizeY(iImage,i).getValue();
                System.out.println("i="+i+"; x="+x+"y="+y);
                byte[] downsample = scaler.downsample(arrayToSave,
                        (int) sizeX, (int) sizeY, Math.pow(scale,i),
                        FormatTools.getBytesPerPixel(pt),
                        false,
                        false,
                        1,
                        false);
                System.out.println("downsample.length="+downsample.length);
                writer.saveBytes(iImage, downsample);
            }
            */
            cleanup();
            System.out.println("Done");
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    /** Close the file writer. */
    private void cleanup() {
        try {
            writer.close();
        }
        catch (IOException e) {
            System.err.println("Failed to close file writer.");
            e.printStackTrace();
        }
    }

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
}
