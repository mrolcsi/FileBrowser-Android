package hu.mrolcsi.android.filebrowser.testapp;

import android.content.Intent;
import android.os.Bundle;
import android.os.Environment;
import android.support.v4.app.FragmentActivity;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import hu.mrolcsi.android.filebrowser.BrowserDialog;
import hu.mrolcsi.android.filebrowser.option.BrowseMode;
import hu.mrolcsi.android.filebrowser.option.SortMode;

/**
 * Ez az activity nem a modul része, csupán tesztelési célokat szolgál.
 */
public class TestActivity extends FragmentActivity {

    private TextView tvPath;
    private BrowserDialog.OnDialogResultListener onDialogResultListener;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);

        Button btnOpenFile = (Button) findViewById(R.id.buttonOpen);
        Button btnSelectDir = (Button) findViewById(R.id.buttonSelectDir);
        Button btnSaveFile = (Button) findViewById(R.id.buttonSaveFile);
        Button btnOpenFileFiltered = (Button) findViewById(R.id.buttonOpenWithFilter);
        tvPath = (TextView) findViewById(R.id.textViewPath);

        onDialogResultListener = new BrowserDialog.OnDialogResultListener() {
            @Override
            public void onPositiveResult(String path) {
                tvPath.setText(path);
            }

            @Override
            public void onNegativeResult() {
                tvPath.setText(android.R.string.cancel);
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
                        .setOnDialogResultListener(onDialogResultListener);
                dialog.show(getSupportFragmentManager(), dialog.toString());
            }
        });

        btnSelectDir.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                BrowserDialog dialog = new BrowserDialog()
                        .setBrowseMode(BrowseMode.SELECT_DIR)
                        .setOnDialogResultListener(onDialogResultListener);
                dialog.show(getSupportFragmentManager(), dialog.toString());
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
                dialog.show(getSupportFragmentManager(), dialog.toString());
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
                dialog.show(getSupportFragmentManager(), dialog.toString());
                //browserIntent.putExtra(BrowserActivity.OPTION_LAYOUT, BrowserActivity.LAYOUT_GRID);
            }
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        switch (requestCode) {
            case 0:
                tvPath.setText(resultCode == RESULT_OK ? data.getStringExtra(BrowserDialog.RESULT) : "Result_Canceled");
                break;
            default:
                break;
        }

    }
}



