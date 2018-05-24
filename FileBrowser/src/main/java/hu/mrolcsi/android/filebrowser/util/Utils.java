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
import hu.mrolcsi.android.filebrowser.R;
import java.util.Arrays;
import java.util.HashSet;

/**
 * Created with IntelliJ IDEA.
 * User: Matusinka Roland
 * Date: 2013.03.25.
 * Time: 22:14
 */

public abstract class Utils {

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

  public static Drawable getTintedDrawable(Context context, int drawableId) {
    Drawable inDrawable;
    if (Build.VERSION.SDK_INT >= 22) {
      inDrawable = context.getResources().getDrawable(drawableId, context.getTheme());
    } else {
      //noinspection deprecation
      inDrawable = context.getResources().getDrawable(drawableId);
    }

    return getTintedDrawable(context, inDrawable);
  }

  public static Drawable getTintedDrawable(Context context, Drawable drawable) {
    if (drawable == null) {
      return null;
    }

    final Drawable outDrawable = DrawableCompat.wrap(drawable);
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

    @SuppressLint("InflateParams") final View contentView = LayoutInflater.from(context)
        .inflate(R.layout.browser_progress_dialog, null);
    ((TextView) contentView.findViewById(android.R.id.message)).setText(message);
    final ProgressBar progressBar = contentView.findViewById(android.R.id.progress);
    final Drawable indeterminateDrawable = progressBar.getIndeterminateDrawable().mutate();

    //get accent color from attrs
    context.getTheme().resolveAttribute(R.attr.colorAccent, tv, true);

    indeterminateDrawable.setColorFilter(tv.data, PorterDuff.Mode.SRC_IN);
    progressBar.setIndeterminateDrawable(indeterminateDrawable);

    builder.setView(contentView);

    return builder.show();
  }
}