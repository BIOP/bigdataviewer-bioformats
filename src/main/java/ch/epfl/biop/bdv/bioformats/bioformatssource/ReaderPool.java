package ch.epfl.biop.bdv.bioformats.bioformatssource;
import loci.formats.IFormatReader;
import java.util.function.Supplier;

/**
 * Created with IntelliJ IDEA.
 * User: dbtsai
 * Date: 2/24/13
 * Time: 1:21 PM
 */

public class ReaderPool extends ResourcePool<IFormatReader> {

    Supplier<IFormatReader> readerSupplier;

    public ReaderPool(int size, Boolean dynamicCreation, Supplier<IFormatReader> readerSupplier) {
        super(size, dynamicCreation);
        createPool();
        this.readerSupplier = readerSupplier;
    }

    @Override
    protected IFormatReader createObject() {
        return readerSupplier.get();
    }
}