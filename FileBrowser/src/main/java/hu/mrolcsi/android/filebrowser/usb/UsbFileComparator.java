package hu.mrolcsi.android.filebrowser.usb;

import java.util.Comparator;

import com.github.mjdev.libaums.fs.UsbFile;
import hu.mrolcsi.android.filebrowser.util.Utils;

/**
 * Created by Matusinka Roland on 2016.04.14..
 */
public abstract class UsbFileComparator {

    public static class ByFileName implements Comparator<UsbFile> {
        @Override
        public int compare(UsbFile f1, UsbFile f2) {
            if (f1.isDirectory() && !f2.isDirectory()) return 1;
            if (!f1.isDirectory() && f2.isDirectory()) return -1;
            return f1.getName().compareToIgnoreCase(f2.getName());
        }
    }

    public static class ByExtension implements Comparator<UsbFile> {
        @Override
        public int compare(UsbFile f1, UsbFile f2) {
            if (f1.isDirectory() && f2.isDirectory()) {
                return f1.getName().compareToIgnoreCase(f2.getName());
            }

            if (f1.isDirectory() && !f2.isDirectory()) return -1;
            if (!f1.isDirectory() && f2.isDirectory()) return 1;

            String ext1 = Utils.getExtension(f1.getName());
            String ext2 = Utils.getExtension(f2.getName());
            if (ext1 == null) return -1;
            if (ext2 == null) return 1;
            return ext1.compareToIgnoreCase(ext2);
        }
    }

    public static class ByDate implements Comparator<UsbFile> {
        @Override
        public int compare(UsbFile f1, UsbFile f2) {
            long f1mod = f1.lastModified();
            long f2mod = f2.lastModified();
            return (int) (f1mod - f2mod);
        }
    }

    public static class BySize implements Comparator<UsbFile> {
        @Override
        public int compare(UsbFile f1, UsbFile f2) {
            if (f1 == null) return 1;
            if (f2 == null) return -1;

            long f1size = 0, f2size = 0;
            if (!f1.isDirectory() && !f2.isDirectory()) {
                f1size = f1.getLength();
                f2size = f2.getLength();
            }

            if (f1.isDirectory() && f2.isDirectory()) {
                f1size = Utils.dirSize(f1);
                f2size = Utils.dirSize(f2);
            }

            if (f1size < f2size) return -1;
            else if (f1size > f2size) return 1;
            else return 0;
        }
    }
}
