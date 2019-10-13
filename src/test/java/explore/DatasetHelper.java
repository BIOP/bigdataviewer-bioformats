package explore;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;

import java.io.File;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;

public class DatasetHelper {

    // https://downloads.openmicroscopy.org/images/

    public static File cachedSampleDir = new File(System.getProperty("user.home"),"CachedSamples");

    final public static String JPG_RGB = "https://biop.epfl.ch/img/splash/physicsTemporal_byRGUIETcrop.jpg";
    final public static String OME_TIF = "https://downloads.openmicroscopy.org/images/Olympus-OIR/etienne/venus%20stack.ome.tif";
    public static void main(String... args) {
        System.out.println("Downloading all sample datasets.");
        ASyncDL(JPG_RGB);
        ASyncDL(OME_TIF);
    }

    public static void ASyncDL(String str) {
        new Thread(() -> urlToFile(str)).start();
    }

    public static File urlToFile(URL url) {
        try {
            File file_out = new File(cachedSampleDir,url.getFile());
            if (file_out.exists()) {
                return file_out;
            } else {
                System.out.println("Downloading and caching: "+url+" size = "+(int)(getFileSize(url)/1024)+" kb");
                FileUtils.copyURLToFile(url, file_out, 10000, 10000);
                System.out.println("Downloading and caching of "+url+" completed successfully ");
                if (FilenameUtils.getExtension(file_out.getAbsolutePath()).equals(".vsi")) {
                    // We need to download all the subfolders
                }
                return file_out;
            }
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    public static File urlToFile(String urlString) {
        URL url = null;
        try {
            url = new URL(urlString);
        } catch (MalformedURLException e) {
            e.printStackTrace();
        }
        return urlToFile(url);
    }

    // https://stackoverflow.com/questions/12800588/how-to-calculate-a-file-size-from-url-in-java
    private static int getFileSize(URL url) {
        URLConnection conn = null;
        try {
            conn = url.openConnection();
            if(conn instanceof HttpURLConnection) {
                ((HttpURLConnection)conn).setRequestMethod("HEAD");
            }
            conn.getInputStream();
            return conn.getContentLength();
        } catch (IOException e) {
            throw new RuntimeException(e);
        } finally {
            if(conn instanceof HttpURLConnection) {
                ((HttpURLConnection)conn).disconnect();
            }
        }
    }

}
