package hu.mrolcsi.android.filebrowser.util;

import java.io.File;
import java.util.Comparator;

/**
 * Created with IntelliJ IDEA.
 * User: Matusinka Roland
 * Date: 2015.08.06.
 * Time: 15:30
 */

public abstract class FileComparator {
    static class byFileName implements Comparator<File> {
        @Override
        public int compare(File f1, File f2) {
            return f1.getName().compareToIgnoreCase(f2.getName());
        }
    }

    static class byExtension implements Comparator<File> {

        @Override
        public int compare(File file, File file2) {
            String ext1 = Utils.getExtension(file.getName());
            String ext2 = Utils.getExtension(file2.getName());
            if (ext1 == null) return -1;
            if (ext2 == null) return 1;
            return ext1.compareToIgnoreCase(ext2);
        }
    }

    static class byDate implements Comparator<File> {

        @Override
        public int compare(File f1, File f2) {
            long f1mod = f1.lastModified();
            long f2mod = f2.lastModified();
            if (f1mod < f2mod) return -1;
            else if (f1mod > f2mod) return 1;
            else return 0;
        }
    }

    static class bySize implements Comparator<File> {

        @Override
        public int compare(File f1, File f2) {
            if (f1 == null) return 1;
            if (f2 == null) return -1;
            long f1size = 0, f2size = 0;
            if (f1.isFile() && f2.isFile()) {
                f1size = f1.length();
                f2size = f2.length();
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