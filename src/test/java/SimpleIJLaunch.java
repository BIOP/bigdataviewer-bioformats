import bdv.util.BdvHandle;
import net.imagej.ImageJ;

public class SimpleIJLaunch {

    // TODO:
    //
    // autoset per filename
    // z problem : expand z thickness
    // force 2d
    // check ignoremetadata for reference frame
    // reference frame for voxel ?

    static public void main(String... args) {
        // Arrange
        // create the ImageJ application context with all available services
        final ImageJ ij = new ImageJ();
        ij.ui().showUI();
    }
}
