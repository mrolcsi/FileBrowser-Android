package hu.mrolcsi.android.filebrowser.testapp;

import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AlertDialog.Builder;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import com.github.mjdev.libaums.fs.FileSystem;
import com.github.mjdev.libaums.fs.UsbFile;
import com.github.mjdev.libaums.fs.UsbFileStreamFactory;
import hu.mrolcsi.android.filebrowser.BrowserDialog;
import hu.mrolcsi.android.filebrowser.BrowserDialog.OnDialogResultListener;
import hu.mrolcsi.android.filebrowser.option.BrowseMode;
import hu.mrolcsi.android.filebrowser.usb.UsbBrowserDialog;
import hu.mrolcsi.android.filebrowser.util.FileUtils;
import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Locale;

/**
 * Created by rmatusinka on 2017.05.16..
 */

public class ExportFileFragment extends Fragment {

  private static final int REQUEST_PERMISSIONS = 50;

  private TextView tvSource;
  private TextView tvDest;

  private ProgressBar pbProgressBar;

  private Button btnSource;
  private Button btnDest;
  private Button btnCopy;

  private File mSourceFile;
  private UsbFile mDestFile;
  private FileSystem mUsbFs;

  @Nullable
  @Override
  public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container,
      @Nullable Bundle savedInstanceState) {
    return inflater.inflate(R.layout.fragment_export, container, false);
  }

  @Override
  public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {

    tvSource = (TextView) view.findViewById(R.id.tvSource);
    tvDest = (TextView) view.findViewById(R.id.tvDest);

    pbProgressBar = (ProgressBar) view.findViewById(R.id.pbProgressBar);

    btnSource = (Button) view.findViewById(R.id.btnSource);
    btnDest = (Button) view.findViewById(R.id.btnDest);
    btnCopy = (Button) view.findViewById(R.id.btnCopy);

    initListeners();
  }

  private void initListeners() {

    btnSource.setOnClickListener(new OnClickListener() {
      @Override
      public void onClick(View view) {
        final BrowserDialog dialog = new BrowserDialog();
        dialog.setStartPath(Environment.getExternalStorageDirectory().getAbsolutePath());
        dialog.setStartIsRoot(true);
        dialog.setOnDialogResultListener(new OnDialogResultListener() {
          @Override
          public void onPositiveResult(String path) {
            mSourceFile = new File(path);
            tvSource.setText(path);
          }

          @Override
          public void onNegativeResult() {
            if (mSourceFile == null) {
              tvSource.setText("<nothing selected>");
            }
          }
        });
        dialog.show(getChildFragmentManager(), "SourceBrowser");
      }
    });

    btnDest.setOnClickListener(new OnClickListener() {
      @Override
      public void onClick(View view) {
        final UsbBrowserDialog dialog = new UsbBrowserDialog();
        dialog.setBrowseMode(BrowseMode.SAVE_FILE);
        dialog.setStartIsRoot(true);
        dialog.setStartPath("a/b/c");
        dialog.setStartIsRoot(true);
        dialog.setOnDialogResultListener(new UsbBrowserDialog.OnDialogResultListener() {
          @Override
          public void onPositiveResult(UsbFile file, FileSystem currentFs) {
            mUsbFs = currentFs;
            mDestFile = file;
            tvDest.setText(FileUtils.getAbsolutePath(file));
          }

          @Override
          public void onNegativeResult() {
            if (mDestFile == null) {
              tvDest.setText("<nothing selected>");
            }
          }
        });
        dialog.show(getChildFragmentManager(), "DestinationBrowser");
      }
    });

    btnCopy.setOnClickListener(new OnClickListener() {
      @Override
      public void onClick(View view) {
        new CopyToUsbTask().execute();
      }
    });

  }

  private class CopyToUsbTask extends AsyncTask<Void, Long, Void> {

    @Override
    protected Void doInBackground(Void... voids) {
      try {
        final InputStream in = new BufferedInputStream(new FileInputStream(mSourceFile));
        final OutputStream out = UsbFileStreamFactory.createBufferedOutputStream(mDestFile, mUsbFs);

        int bufferSize = mUsbFs.getChunkSize();

        byte[] buffer = new byte[bufferSize];
        long copied = 0;
        long total = mSourceFile.length();
        int len;

        while ((len = in.read(buffer)) != -1) {
          out.write(buffer, 0, len);
          copied += len;
          publishProgress(copied, total);
        }

        out.close();
        in.close();

      } catch (IOException e) {
        e.printStackTrace();
      }

      return null;
    }

    @Override
    protected void onProgressUpdate(Long... values) {
      final int progress = values[0].intValue();
      final int max = values[1].intValue();

      Log.d("UsbCopy", String.format(Locale.getDefault(), "Progress=%1$d Max=%2$d", progress, max));

      pbProgressBar.setMax(max);
      pbProgressBar.setProgress(progress);
    }

    @Override
    protected void onPostExecute(Void aVoid) {
      AlertDialog.Builder builder = new Builder(getContext());
      builder.setMessage("Finished copying.");
      builder.setPositiveButton(android.R.string.ok, null);
      builder.show();
    }
  }
}
