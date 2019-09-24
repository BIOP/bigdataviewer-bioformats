package ch.epfl.biop.bdv.bioformats.export.ometiff;

import net.imglib2.Cursor;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.type.numeric.ARGBType;
import net.imglib2.type.numeric.integer.UnsignedByteType;
import net.imglib2.type.numeric.integer.UnsignedShortType;
import net.imglib2.view.Views;

import java.nio.ByteBuffer;

/**
 * Limited to 4Gb....
 */

public class SourceToByteArray {

    static public byte[] raiARGBToByteArray(RandomAccessibleInterval<ARGBType> rai) {
        return null;
    }

    static public byte[] raiUnsignedByteTypeToByteArray(RandomAccessibleInterval<UnsignedByteType> rai, UnsignedByteType t) {
        Cursor<UnsignedByteType> c = Views.flatIterable(rai).cursor();
        long nBytes = rai.dimension(0)*rai.dimension(1)*rai.dimension(2);

        if (nBytes>Integer.MAX_VALUE) {
            System.err.println("Too many bytes during export!");
            return null;
        }

        byte[] out = new byte[(int) nBytes];

        for (int i=0;i<nBytes;i++) {
            out[i]=c.next().getByte();
        }
        return out;
    }

    static public byte[] raiUnsignedShortTypeToByteArray(RandomAccessibleInterval<UnsignedShortType> rai, UnsignedShortType t) {
        Cursor<UnsignedShortType> c = Views.flatIterable(rai).cursor();
        int nBytesPerPixel = 2;
        long nBytes = rai.dimension(0)*rai.dimension(1)*rai.dimension(2)*nBytesPerPixel;

        if (nBytes>Integer.MAX_VALUE) {
            System.err.println("Too many bytes during export!");
            return null;
        }

        byte[] out = new byte[(int) nBytes];

        for (int i=0;i<nBytes;i+=nBytesPerPixel) {
            int v = c.next().get();
            //int v = ((bytes[idxPx + 1] & 0xff) << 8) | (bytes[idxPx] & 0xff);
            out[i]  =(byte)(v);
            out[i+1]=(byte)(v >>> 8);
        }
        return out;
    }
}
