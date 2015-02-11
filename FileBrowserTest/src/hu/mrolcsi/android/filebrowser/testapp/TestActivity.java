package hu.mrolcsi.android.filebrowser.testapp;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.os.Environment;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import hu.mrolcsi.android.filebrowser.BrowserActivity;
import hu.mrolcsi.android.filebrowser.BrowserDialog;

/**
 * Ez az activity nem a modul része, csupán tesztelési célokat szolgál.
 */
public class TestActivity extends Activity {

    private TextView tvPath;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);

        Button btnOpenFile = (Button) findViewById(R.id.buttonOpen);
        Button btnSelectDir = (Button) findViewById(R.id.buttonSelectDir);
        Button btnSaveFile = (Button) findViewById(R.id.buttonSaveFile);
        Button btnOpenFileFiltered = (Button) findViewById(R.id.buttonOpenWithFilter);
        Button btnDialog = (Button) findViewById(R.id.btnShowBrowserDialog);
        tvPath = (TextView) findViewById(R.id.textViewPath);

        final Intent browserIntent = new Intent(this, BrowserActivity.class);

        btnOpenFile.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                browserIntent.putExtra(BrowserActivity.OPTION_BROWSE_MODE, BrowserActivity.MODE_OPEN_FILE);
                browserIntent.putExtra(BrowserActivity.OPTION_SORT_MODE, BrowserActivity.SORT_BY_EXTENSION_ASC);
                browserIntent.putExtra(BrowserActivity.OPTION_START_IS_ROOT, false);
                startActivityForResult(browserIntent, 0);
            }
        });

        btnSelectDir.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                browserIntent.putExtra(BrowserActivity.OPTION_BROWSE_MODE, BrowserActivity.MODE_SELECT_DIR);
                startActivityForResult(browserIntent, 0);
            }
        });

        btnSaveFile.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                browserIntent.putExtra(BrowserActivity.OPTION_BROWSE_MODE, BrowserActivity.MODE_SAVE_FILE);
                browserIntent.putExtra(BrowserActivity.OPTION_DEFAULT_FILENAME, "test_file.txt");
                startActivityForResult(browserIntent, 0);
            }
        });

        btnOpenFileFiltered.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                //browserIntent.putExtra(BrowserActivity.OPTION_START_PATH, "/mnt/sdcard");
                //browserIntent.putExtra(BrowserActivity.OPTION_EXTENSION_FILTER, "mp3;wav");
                browserIntent.putExtra(BrowserActivity.OPTION_START_IS_ROOT, false);
                //browserIntent.putExtra(BrowserActivity.OPTION_LAYOUT, BrowserActivity.LAYOUT_GRID);
                browserIntent.putExtra(BrowserActivity.OPTION_SORT_MODE, BrowserActivity.SORT_BY_EXTENSION_ASC);
                browserIntent.putExtra(BrowserActivity.OPTION_BROWSE_MODE, BrowserActivity.MODE_SAVE_FILE);
                startActivityForResult(browserIntent, 0);
            }
        });

        final BrowserDialog dialog = new BrowserDialog();
        dialog.setBrowseMode(BrowserDialog.MODE_OPEN_FILE)
                .setDefaultFileName("screenshot.jpg")
                .setStartPath(Environment.getExternalStorageDirectory().getAbsolutePath())
                .setStartIsRoot(false);

        btnDialog.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                dialog.show(getFragmentManager(), "browser");
            }
        });

        dialog.setOnDialogResultListener(new BrowserDialog.OnDialogResultListener() {
            @Override
            public void onPositiveResult(String path) {
                tvPath.setText(path);
            }

            @Override
            public void onNegativeResult() {
                tvPath.setText("mégsézve");
            }
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        switch (requestCode) {
            case 0:
                tvPath.setText(resultCode == RESULT_OK ? data.getStringExtra(BrowserActivity.RESULT) : "Result_Canceled");
                break;
            default:
                break;
        }

    }
}



