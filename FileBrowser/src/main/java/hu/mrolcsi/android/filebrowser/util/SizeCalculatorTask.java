package hu.mrolcsi.android.filebrowser.util;

import android.os.AsyncTask;

import java.io.File;
import java.util.Stack;

/**
 * Created with IntelliJ IDEA.
 * User: Matusinka Roland
 * Date: 2015.08.06.
 * Time: 16:26
 */

public class SizeCalculatorTask extends AsyncTask<File, Long, Long> {

    @Override
    protected Long doInBackground(File... files) {
        long result = 0;

        // if input is a file (not directory) return its size
        if (files[0].isFile()) return files[0].length();

        // if it's a directory, calculate size recursively
        Stack<File> dirStack = new Stack<>();
        dirStack.push(files[0]);

        File dirCurrent;
        File[] fileList;

        while (!dirStack.isEmpty()) {

            if (isCancelled()) return 0l;

            dirCurrent = dirStack.pop();

            fileList = dirCurrent.listFiles();

            if (fileList != null) {
                for (File aFileList : fileList) {

                    if (aFileList.isDirectory())
                        dirStack.push(aFileList);
                    else {
                        result += aFileList.length();
                        //publishProgress(aFileList.length());
                    }
                }
            } else result = 0;
        }
        return result;
    }
}
