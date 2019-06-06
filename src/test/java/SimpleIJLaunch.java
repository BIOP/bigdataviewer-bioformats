import net.imagej.ImageJ;
import org.junit.Test;

public class SimpleIJLaunch {

    static public void main(String... args) {
        // Arrange
        // create the ImageJ application context with all available services
        final ImageJ ij = new ImageJ();
        ij.ui().showUI();
    }
}
