package explore;

import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.IOException;
import java.net.URL;

public class DatasetHelper {

    public static File urlToFile(URL url) {
        try {
            File temp = File.createTempFile("timg", ".jpg");
            temp.deleteOnExit();
            FileUtils.copyURLToFile(url, temp, 10000, 10000);
            return temp;
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    public static void main(String... args) {

    }
}
