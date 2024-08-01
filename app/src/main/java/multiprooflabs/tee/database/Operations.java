package multiprooflabs.tee.database;

import android.content.Context;
import android.util.Log;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import com.google.android.gms.common.util.IOUtils;


import static android.content.Context.MODE_PRIVATE;

import multiprooflabs.tee.logging.RustLogger;

public class Operations {
    public static final String CLASS_NAME = "Java" + Operations.class.getName();
    public static final String DATA_DIR = "/data/local/tmp";

    public static String readFile(String name) {
        File dataDirectory = new File(DATA_DIR);

        RustLogger.rustLog(CLASS_NAME + " reading " + name + " from " + dataDirectory);
        File fileToRead = new File(dataDirectory, name);

        //Read text from file
        String line;
        StringBuilder text = new StringBuilder();
        try(BufferedReader br = new BufferedReader(new FileReader(fileToRead))) {
            while ((line = br.readLine()) != null) {
                text.append(line);
            }
        } catch (IOException e) {
            RustLogger.rustLog(CLASS_NAME + " failed to read the file " + fileToRead + e.getMessage());
            return "";
        }

        return text.toString();
    }

    public static void writeBytes(Context context, String fileName, byte[] signedState) {
        try (FileOutputStream outputStream = context.openFileOutput(fileName, MODE_PRIVATE)){
            outputStream.write(signedState);
            outputStream.flush();
            outputStream.close();
            RustLogger.rustLog(CLASS_NAME + fileName + " written to disk");
        } catch (IOException e) {
            RustLogger.rustLog(CLASS_NAME + " failed to write " + fileName + " to disk:" + e.getMessage());
        }
    }

    public static byte[] readBytes(Context context, String fileName) {
        byte[] ba = {};
        try (FileInputStream inputStream = context.openFileInput(fileName)){
            ba = IOUtils.toByteArray(inputStream);
        } catch (IOException e) {
            RustLogger.rustLog(CLASS_NAME + " failed to get the signature from the file, ok if first run!");
        }
        return ba;
    }

    public static void copy(File src, File dst) throws IOException {
        try (InputStream in = new FileInputStream(src)) {
            try (OutputStream out = new FileOutputStream(dst)) {
                // Transfer bytes from in to out
                byte[] buf = new byte[1024];
                int len;
                while ((len = in.read(buf)) > 0) {
                    out.write(buf, 0, len);
                }
            }
        }
    }
}
