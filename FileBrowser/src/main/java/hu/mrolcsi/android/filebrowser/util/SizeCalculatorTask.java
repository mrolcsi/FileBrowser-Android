package hu.mrolcsi.android.filebrowser.util;

import android.os.AsyncTask;

import java.io.File;

/**
 * Created with IntelliJ IDEA.
 * User: Matusinka Roland
 * Date: 2015.08.06.
 * Time: 16:26
 */

public class SizeCalculatorTask extends AsyncTask<File, Long, Long> {

    @Override
    protected Long doInBackground(File... files) {

        // if input is a file (not directory) return its size
        if (files[0].isFile()) return files[0].length();

        // if it's a directory, calculate size recursively
        return Utils.dirSize(files[0]);
    }
}
