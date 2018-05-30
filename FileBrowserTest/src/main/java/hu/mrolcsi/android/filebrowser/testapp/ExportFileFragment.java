package hu.mrolcsi.android.filebrowser.testapp;

import android.os.AsyncTask;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AlertDialog.Builder;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import com.github.mjdev.libaums.fs.FileSystem;
import com.github.mjdev.libaums.fs.UsbFile;
import com.github.mjdev.libaums.fs.UsbFileStreamFactory;
import hu.mrolcsi.android.filebrowser.BrowserDialog;
import hu.mrolcsi.android.filebrowser.option.BrowseMode;
import hu.mrolcsi.android.filebrowser.usb.UsbBrowserDialog;
import hu.mrolcsi.android.filebrowser.util.FileUtils;
import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.ref.WeakReference;
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
  private UsbFile mDestDir;
  private FileSystem mUsbFs;

  @Nullable
  @Override
  public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container,
      @Nullable Bundle savedInstanceState) {
    return inflater.inflate(R.layout.fragment_export, container, false);
  }

  @Override
  public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {

    tvSource = view.findViewById(R.id.tvSource);
    tvDest = view.findViewById(R.id.tvDest);

    pbProgressBar = view.findViewById(R.id.pbProgressBar);

    btnSource = view.findViewById(R.id.btnSource);
    btnDest = view.findViewById(R.id.btnDest);
    btnCopy = view.findViewById(R.id.btnCopy);

    initListeners();
  }

  private void initListeners() {

    btnSource.setOnClickListener(view -> {
      final BrowserDialog dialog = new BrowserDialog();
      dialog.setShowHiddenFiles(true);
      dialog.setOnFileSelectedListener(pathToFile -> {
        mSourceFile = new File(pathToFile);
        tvSource.setText(pathToFile);
      });
      dialog.show(getChildFragmentManager(), "SourceBrowser");
    });

    btnDest.setOnClickListener(view -> {
      final UsbBrowserDialog dialog = new UsbBrowserDialog();
      dialog.setBrowseMode(BrowseMode.SAVE_FILE);
      dialog.setOnFileSelectedListener((file, fileSystem) -> {
        mUsbFs = fileSystem;
        mDestDir = file;
        tvDest.setText(FileUtils.getAbsolutePath(file));
      });
      dialog.show(getChildFragmentManager(), "DestinationBrowser");
    });

    btnCopy.setOnClickListener(view -> new CopyToUsbTask(this).execute());
  }

  private static class CopyToUsbTask extends AsyncTask<Void, Long, Void> {

    private WeakReference<ExportFileFragment> wrFragment;

    public CopyToUsbTask(ExportFileFragment fragment) {
      this.wrFragment = new WeakReference<>(fragment);
    }

    @Override
    protected Void doInBackground(Void... voids) {
      try {
        final InputStream in = new BufferedInputStream(new FileInputStream(wrFragment.get().mSourceFile));
        final OutputStream out = UsbFileStreamFactory.createBufferedOutputStream(
            wrFragment.get().mDestDir.createFile(wrFragment.get().mSourceFile.getName()), wrFragment.get().mUsbFs);

        int bufferSize = wrFragment.get().mUsbFs.getChunkSize();

        byte[] buffer = new byte[bufferSize];
        long copied = 0;
        long total = wrFragment.get().mSourceFile.length();
        int len;

        while ((len = in.read(buffer)) != -1) {
          out.write(buffer, 0, len);
          copied += len;
          publishProgress(copied, total);
        }

        out.close();
        in.close();

      } catch (IOException e) {
        Log.e(getClass().getSimpleName(), Log.getStackTraceString(e));
      }

      return null;
    }

    @Override
    protected void onProgressUpdate(Long... values) {
      final int progress = values[0].intValue();
      final int max = values[1].intValue();

      Log.d(getClass().getSimpleName(), String.format(Locale.getDefault(), "Progress=%1$d Max=%2$d", progress, max));

      wrFragment.get().pbProgressBar.setMax(max);
      wrFragment.get().pbProgressBar.setProgress(progress);
    }

    @Override
    protected void onPostExecute(Void aVoid) {
      AlertDialog.Builder builder = new Builder(wrFragment.get().getContext());
      builder.setMessage("Finished copying.");
      builder.setPositiveButton(android.R.string.ok, null);
      builder.show();
    }
  }
}
