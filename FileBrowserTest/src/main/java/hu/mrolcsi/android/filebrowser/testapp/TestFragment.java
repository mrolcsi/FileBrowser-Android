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
  private BrowserDialog.OnDialogResultListener onDialogResultListener;
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

    Button btnOpenFile = (Button) view.findViewById(R.id.buttonOpen);
    Button btnSelectDir = (Button) view.findViewById(R.id.buttonSelectDir);
    Button btnSaveFile = (Button) view.findViewById(R.id.buttonSaveFile);
    Button btnOpenFileFiltered = (Button) view.findViewById(R.id.buttonOpenWithFilter);
    Button btnOpenFileInFragment = (Button) view.findViewById(R.id.buttonOpenAsFragment);
    Button btnOpenUsb = (Button) view.findViewById(R.id.buttonOpenUsb);
    tvPath = (TextView) view.findViewById(R.id.textViewPath);

    onDialogResultListener = new BrowserDialog.OnDialogResultListener() {
      @Override
      public void onPositiveResult(String path) {
        tvPath.setText(path);
        ((MainActivity) getActivity()).swapFragment(TestFragment.this);
      }

      @Override
      public void onNegativeResult() {
        tvPath.setText(android.R.string.cancel);
        ((MainActivity) getActivity()).swapFragment(TestFragment.this);
      }
    };

    btnOpenFile.setOnClickListener(new View.OnClickListener() {
      @Override
      public void onClick(View view) {
        BrowserDialog dialog = new BrowserDialog()
            .setBrowseMode(BrowseMode.OPEN_FILE)
            .setSortMode(SortMode.BY_EXTENSION_ASC)
            .setStartIsRoot(false)
            .setRootPath(Environment.getExternalStorageDirectory().getAbsolutePath())
            .setDialogTitle("Select file for testing")
            .setOnDialogResultListener(onDialogResultListener);
        dialog.show(getChildFragmentManager(), dialog.toString());
      }
    });

    btnSelectDir.setOnClickListener(new View.OnClickListener() {
      @Override
      public void onClick(View view) {
        BrowserDialog dialog = new BrowserDialog()
            .setBrowseMode(BrowseMode.SELECT_DIR)
            .setOnDialogResultListener(onDialogResultListener);
        dialog.show(getChildFragmentManager(), dialog.toString());
      }
    });

    btnSaveFile.setOnClickListener(new View.OnClickListener() {
      @Override
      public void onClick(View view) {
        BrowserDialog dialog = new BrowserDialog()
            .setBrowseMode(BrowseMode.SAVE_FILE)
            .setDefaultFileName("test_file.txt")
            .setExtensionFilter("txt", "xml", "csv")
            .setRootPath(Environment.getExternalStorageDirectory().getAbsolutePath())
            .setStartPath(Environment.getExternalStorageDirectory().getAbsolutePath())
            .setStartIsRoot(false)
            .setOnDialogResultListener(onDialogResultListener);
        dialog.show(getChildFragmentManager(), dialog.toString());
      }
    });

    btnOpenFileFiltered.setOnClickListener(new View.OnClickListener() {
      @Override
      public void onClick(View view) {
        BrowserDialog dialog = new BrowserDialog()
            .setStartPath(Environment.getExternalStorageDirectory().getAbsolutePath())
            .setExtensionFilter("mp3;wav")
            .setStartIsRoot(false)
            .setSortMode(SortMode.BY_EXTENSION_ASC)
            .setBrowseMode(BrowseMode.SAVE_FILE)
            .setOnDialogResultListener(onDialogResultListener);
        dialog.show(getChildFragmentManager(), dialog.toString());
      }
    });

    btnOpenFileInFragment.setOnClickListener(new View.OnClickListener() {
      @Override
      public void onClick(View view) {
        BrowserDialog dialog = new BrowserDialog()
            .setStartPath(Environment.getExternalStorageDirectory().getAbsolutePath())
            .setExtensionFilter("mp3;wav")
            .setStartIsRoot(false)
            .setSortMode(SortMode.BY_DATE_DESC)
            .setBrowseMode(BrowseMode.SAVE_FILE)
            .setLayout(Layout.GRID)
            .setOnDialogResultListener(onDialogResultListener);
        ((MainActivity) getActivity()).swapFragment(dialog);
      }
    });

    btnOpenUsb.setOnClickListener(new View.OnClickListener() {
      @Override
      public void onClick(View view) {
        ((MainActivity) getActivity()).swapFragment(new ExportFileFragment());
      }
    });
  }
}



