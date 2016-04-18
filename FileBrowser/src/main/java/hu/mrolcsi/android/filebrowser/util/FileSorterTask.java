package hu.mrolcsi.android.filebrowser.util;

import android.os.AsyncTask;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import hu.mrolcsi.android.filebrowser.option.SortMode;

/**
 * Created with IntelliJ IDEA.
 * User: Matusinka Roland
 * Date: 2015.08.07.
 * Time: 11:31
 */

public class FileSorterTask extends AsyncTask<File, Integer, List<File>> {

    private final SortMode mSortMode;

    public FileSorterTask(SortMode sortMode) {
        mSortMode = sortMode;
    }

    private List<File> sortFiles(File[] input, Comparator<File> comparator) {
        if (input != null) {
            List<File> files = new ArrayList<>();
            List<File> dirs = new ArrayList<>();
            for (int i = 0; i < input.length; i++) {
                File f = input[i];
                if (isCancelled()) return null;
                if (f.isFile()) files.add(f);
                if (f.isDirectory()) dirs.add(f);
                publishProgress(i, input.length);
            }
            Collections.sort(dirs, comparator);
            Collections.sort(files, comparator);
            List<File> list = new ArrayList<>();
            list.addAll(dirs);
            list.addAll(files);
            return list;
        } else return null;
    }

    @Override
    protected List<File> doInBackground(File... inputData) {
        switch (mSortMode) {
            default:
            case BY_NAME_ASC:
                return sortFiles(inputData, new FileComparator.ByFileName());
            case BY_NAME_DESC:
                return sortFiles(inputData, Collections.reverseOrder(new FileComparator.ByFileName()));
            case BY_EXTENSION_ASC:
                return sortFiles(inputData, new FileComparator.ByExtension());
            case BY_EXTENSION_DESC:
                return sortFiles(inputData, Collections.reverseOrder(new FileComparator.ByExtension()));
            case BY_DATE_ASC:
                return sortFiles(inputData, new FileComparator.ByDate());
            case BY_DATE_DESC:
                return sortFiles(inputData, Collections.reverseOrder(new FileComparator.ByDate()));
            case BY_SIZE_ASC:
                return sortFiles(inputData, new FileComparator.BySize());
            case BY_SIZE_DESC:
                return sortFiles(inputData, Collections.reverseOrder(new FileComparator.BySize()));
        }
    }
}
