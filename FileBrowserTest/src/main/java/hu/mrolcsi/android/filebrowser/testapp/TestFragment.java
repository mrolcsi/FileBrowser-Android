package hu.mrolcsi.android.filebrowser.testapp;

import android.os.Bundle;
import android.os.Environment;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import hu.mrolcsi.android.filebrowser.BrowserDialog;
import hu.mrolcsi.android.filebrowser.option.BrowseMode;
import hu.mrolcsi.android.filebrowser.option.Layout;
import hu.mrolcsi.android.filebrowser.option.SortMode;

/**
 * Ez az activity nem a modul része, csupán tesztelési célokat szolgál.
 */
public class TestFragment extends Fragment {

  private TextView tvPath;
  private BrowserDialog.OnFileSelectedListener mOnFileSelectedListener;
  @SuppressWarnings("unused")
  private View mRootView;

  @Nullable
  @Override
  public View onCreateView(LayoutInflater inflater, ViewGroup container,
      Bundle savedInstanceState) {
    if (mRootView == null) {
      mRootView = inflater.inflate(R.layout.fragment_main, container, false);
    }
    return mRootView;
  }

  @Override
  public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
    super.onViewCreated(view, savedInstanceState);

    Button btnOpenFile = view.findViewById(R.id.buttonOpen);
    Button btnSelectDir = view.findViewById(R.id.buttonSelectDir);
    Button btnSaveFile = view.findViewById(R.id.buttonSaveFile);
    Button btnOpenFileFiltered = view.findViewById(R.id.buttonOpenWithFilter);
    Button btnOpenFileInFragment = view.findViewById(R.id.buttonOpenAsFragment);
    Button btnOpenUsb = view.findViewById(R.id.buttonOpenUsb);
    tvPath = view.findViewById(R.id.textViewPath);

    mOnFileSelectedListener = pathToFile -> {
      tvPath.setText(pathToFile);
      ((MainActivity) getActivity()).swapFragment(TestFragment.this);
    };

    btnOpenFile.setOnClickListener(v -> {
      BrowserDialog dialog = new BrowserDialog()
          .setBrowseMode(BrowseMode.OPEN_FILE)
          .setSortMode(SortMode.BY_EXTENSION_ASC)
          .setStartIsRoot(false)
          .setRootPath(Environment.getExternalStorageDirectory().getAbsolutePath())
          .setDialogTitle("Select file for testing")
          .setOnFileSelectedListener(mOnFileSelectedListener);
      dialog.show(getChildFragmentManager(), dialog.toString());
    });

    btnSelectDir.setOnClickListener(v -> {
      BrowserDialog dialog = new BrowserDialog()
          .setBrowseMode(BrowseMode.SELECT_DIR)
          .setOnFileSelectedListener(mOnFileSelectedListener);
      dialog.show(getChildFragmentManager(), dialog.toString());
    });

    btnSaveFile.setOnClickListener(v -> {
      BrowserDialog dialog = new BrowserDialog()
          .setBrowseMode(BrowseMode.SAVE_FILE)
          .setDefaultFileName("test_file.txt")
          .setExtensionFilter("txt", "xml", "csv")
          .setRootPath(Environment.getExternalStorageDirectory().getAbsolutePath())
          .setStartPath(Environment.getExternalStorageDirectory().getAbsolutePath())
          .setStartIsRoot(false)
          .setOnFileSelectedListener(mOnFileSelectedListener);
      dialog.show(getChildFragmentManager(), dialog.toString());
    });

    btnOpenFileFiltered.setOnClickListener(v -> {
      BrowserDialog dialog = new BrowserDialog()
          .setStartPath(Environment.getExternalStorageDirectory().getAbsolutePath())
          .setExtensionFilter("mp3;wav")
          .setStartIsRoot(false)
          .setSortMode(SortMode.BY_EXTENSION_ASC)
          .setBrowseMode(BrowseMode.SAVE_FILE)
          .setOnFileSelectedListener(mOnFileSelectedListener);
      dialog.show(getChildFragmentManager(), dialog.toString());
    });

    btnOpenFileInFragment.setOnClickListener(v -> {
      BrowserDialog dialog = new BrowserDialog()
          .setStartPath(Environment.getExternalStorageDirectory().getAbsolutePath())
          .setExtensionFilter("mp3;wav")
          .setStartIsRoot(false)
          .setSortMode(SortMode.BY_DATE_DESC)
          .setBrowseMode(BrowseMode.SAVE_FILE)
          .setLayout(Layout.GRID)
          .setOnFileSelectedListener(mOnFileSelectedListener);
      ((MainActivity) getActivity()).swapFragment(dialog);
    });

    btnOpenUsb.setOnClickListener(v -> ((MainActivity) getActivity()).swapFragment(new ExportFileFragment()));
  }
}



