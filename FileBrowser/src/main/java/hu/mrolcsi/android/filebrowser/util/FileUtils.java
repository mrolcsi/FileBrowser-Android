package hu.mrolcsi.android.filebrowser.util;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Stack;

import com.github.mjdev.libaums.fs.UsbFile;

/**
 * Created by Matusinka Roland on 2016.04.20..
 */
public abstract class FileUtils {
    private static final String[] RESERVED_CHARS = {"|", "\\", "?", "*", "<", "\"", ":", ">"};

    public static String getExtension(String fileName) {
        String ext = null;
        int i = fileName.lastIndexOf('.');
        if (i > 0 && i < fileName.length() - 1)
            ext = fileName.substring(i + 1).toLowerCase();
        return ext;
    }

    public static String getNameWithoutExtension(String fileName) {
        int i = fileName.lastIndexOf('.');

        try {
            return fileName.substring(0, i);
        } catch (IndexOutOfBoundsException e) {
            return fileName;
        }
    }

    public static boolean isFilenameValid(String filename) {
        for (String reservedChar : RESERVED_CHARS) {
            if (filename.contains(reservedChar)) return false;
        }
        return true;
    }

    public static String getFriendlySize(File inputFile) {
        long rawSize = 0;
        if (inputFile.isFile()) {
            rawSize = inputFile.length();
        } else if (inputFile.isDirectory()) {
            rawSize = dirSize(inputFile);
        }
        return getFriendlySize(rawSize);
    }

    public static String getFriendlySize(long rawSize) {
        if (rawSize > 1000000000)
            return String.format(Locale.getDefault(), "%.2f GB", (float) rawSize / 1000000000);
        else if (rawSize > 1000000)
            return String.format(Locale.getDefault(), "%.2f MB", (float) rawSize / 1000000);
        else if (rawSize > 1000)
            return String.format(Locale.getDefault(), "%.2f kB", (float) rawSize / 1000);
        else return String.format(Locale.getDefault(), "%d B", rawSize);
    }

    /**
     * Rekurzívan megadja egy mappa méretét byte-ban.
     *
     * @param dir Kinduló mappa.
     * @return A mappa mérete byte-ban.
     * @see <a href="http://stackoverflow.com/questions/4040912/how-can-i-get-the-size-of-a-folder-on-sd-card-in-android">forrás</a>
     */
    public static long dirSize(File dir) {

        if (Cache.getInstance().sizeCache.containsKey(dir.getAbsolutePath()))
            return Cache.getInstance().sizeCache.get(dir.getAbsolutePath());

        long result = 0;

        Stack<File> dirlist = new Stack<>();
        dirlist.clear();

        dirlist.push(dir);

        while (!dirlist.isEmpty()) {
            File dirCurrent = dirlist.pop();

            File[] fileList = dirCurrent.listFiles();

            if (fileList != null) {
                for (File aFileList : fileList) {

                    if (aFileList.isDirectory())
                        dirlist.push(aFileList);
                    else
                        result += aFileList.length();
                }
            } else result = 0;
        }

        Cache.getInstance().sizeCache.put(dir.getAbsolutePath(), result);

        return result;
    }

    public static long dirSize(UsbFile dir) {
        if (Cache.getInstance().sizeCache.containsKey(dir.getName()))
            return Cache.getInstance().sizeCache.get(dir.getName());

        long result = 0;

        Stack<UsbFile> dirlist = new Stack<>();
        dirlist.clear();

        dirlist.push(dir);

        while (!dirlist.isEmpty()) {
            UsbFile dirCurrent = dirlist.pop();

            try {
                UsbFile[] fileList = dirCurrent.listFiles();

                if (fileList != null) {
                    for (UsbFile file : fileList) {

                        if (file.isDirectory())
                            dirlist.push(file);
                        else
                            result += file.getLength();
                    }
                } else result = 0;
            } catch (IOException e) {
                //skip
            }
        }

        Cache.getInstance().sizeCache.put(dir.getName(), result);

        return result;
    }

    public static String getAbsolutePath(UsbFile file) {
        UsbFile child = file;

        List<String> parents = new ArrayList<>();

        while (child.getParent() != null) {
            parents.add(0, child.getName());
            child = child.getParent();
        }

        StringBuilder sb = new StringBuilder();

        for (String name : parents) {
            sb.append("/");
            sb.append(name);
        }

        return sb.toString();
    }
}
