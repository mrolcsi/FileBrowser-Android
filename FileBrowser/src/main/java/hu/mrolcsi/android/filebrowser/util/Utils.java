package hu.mrolcsi.android.filebrowser.util;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.support.v4.graphics.drawable.DrawableCompat;
import android.support.v7.app.AlertDialog;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Stack;

import com.github.mjdev.libaums.fs.UsbFile;
import hu.mrolcsi.android.filebrowser.R;

/**
 * Created with IntelliJ IDEA.
 * User: Matusinka Roland
 * Date: 2013.03.25.
 * Time: 22:14
 */

public abstract class Utils {

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

    public static String getFriendlySize(File inputFile) {
        long rawSize = 0;
        if (inputFile.isFile()) {
            rawSize = inputFile.length();
        } else if (inputFile.isDirectory()) {
            rawSize = dirSize(inputFile);
        }
        if (rawSize > 1000000000)
            return String.format("%.2f GB", (float) rawSize / 1000000000);
        else if (rawSize > 1000000)
            return String.format("%.2f MB", (float) rawSize / 1000000);
        else if (rawSize > 1000)
            return String.format("%.2f kB", (float) rawSize / 1000);
        else return String.format("%d B", rawSize);
    }

    public static String getFriendlySize(long rawSize) {
        if (rawSize > 1000000000)
            return String.format("%.2f GB", (float) rawSize / 1000000000);
        else if (rawSize > 1000000)
            return String.format("%.2f MB", (float) rawSize / 1000000);
        else if (rawSize > 1000)
            return String.format("%.2f kB", (float) rawSize / 1000);
        else return String.format("%d B", rawSize);
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

    public static boolean isFilenameValid(String filename) {
        for (String reservedChar : RESERVED_CHARS) {
            if (filename.contains(reservedChar)) return false;
        }
        return true;
    }

    // http://stackoverflow.com/questions/1128723/in-java-how-can-i-test-if-an-array-contains-a-certain-value
    public static <T> boolean contains(final T[] array, final T v) {
//        if (v == null) {
//            for (final T e : array)
//                if (e == null)
//                    return true;
//        } else {
//            for (final T e : array)
//                if (e == v || v.equals(e))
//                    return true;
//        }
//
//        return false;

        final HashSet<T> set = new HashSet<>(Arrays.asList(array));
        return set.contains(v);
    }

    public static Drawable tintDrawable(Context context, int drawableId) {
        if (Build.VERSION.SDK_INT >= 22) {
            return context.getResources().getDrawable(drawableId, context.getTheme());
        }

        @SuppressWarnings("deprecation") final Drawable inDrawable = context.getResources().getDrawable(drawableId);
        if (inDrawable == null) {
            return null;
        }

        final Drawable outDrawable = DrawableCompat.wrap(inDrawable);
        DrawableCompat.setTintMode(outDrawable, PorterDuff.Mode.SRC_IN);

        final TypedValue value = new TypedValue();
        context.getTheme().resolveAttribute(R.attr.colorAccent, value, true);
        final int tintColor = value.data;

        DrawableCompat.setTint(outDrawable, tintColor);

        return outDrawable;
    }

    public static AlertDialog showProgressDialog(final Context context, final CharSequence message) {

        //get dialog theme from attrs
        final TypedValue tv = new TypedValue();
        context.getTheme().resolveAttribute(R.attr.alertDialogTheme, tv, true);

        final AlertDialog.Builder builder = new AlertDialog.Builder(context, tv.resourceId)
                .setCancelable(false);

        @SuppressLint("InflateParams") final View contentView = LayoutInflater.from(context).inflate(R.layout.browser_progress_dialog, null);
        ((TextView) contentView.findViewById(android.R.id.message)).setText(message);
        final ProgressBar progressBar = (ProgressBar) contentView.findViewById(android.R.id.progress);
        final Drawable indeterminateDrawable = progressBar.getIndeterminateDrawable().mutate();

        //get accent color from attrs
        context.getTheme().resolveAttribute(R.attr.colorAccent, tv, true);

        indeterminateDrawable.setColorFilter(tv.data, PorterDuff.Mode.SRC_IN);
        progressBar.setIndeterminateDrawable(indeterminateDrawable);

        builder.setView(contentView);

        return builder.show();
    }
}