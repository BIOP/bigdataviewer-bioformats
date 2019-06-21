package ch.epfl.biop.bdv.bioformats.export;

import net.imglib2.Cursor;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.type.numeric.ARGBType;
import net.imglib2.type.numeric.integer.UnsignedByteType;
import net.imglib2.view.Views;

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
}
