package hu.mrolcsi.android.filebrowser.testapp;

import android.Manifest.permission;
import android.content.pm.PackageManager;
import android.os.Build.VERSION;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.Fragment;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Toast;
import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {

  private static final int REQUEST_PERMISSIONS = 50;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_main);
    if (savedInstanceState == null) {
      getSupportFragmentManager().beginTransaction()
          .replace(R.id.container, new TestFragment())
          .commit();
    }
  }

  @Override
  protected void onStart() {
    super.onStart();

    if (VERSION.SDK_INT >= 23) {
      // request permissions before launching app
      final String[] neededPermissions = {
          permission.WRITE_EXTERNAL_STORAGE,
      };

      List<String> permissionsList = new ArrayList<>();
      for (String permission : neededPermissions) {
        if (ContextCompat.checkSelfPermission(this, permission)
            != PackageManager.PERMISSION_GRANTED) {
          permissionsList.add(permission);
        }
      }

      if (permissionsList.size() > 0) {
        ActivityCompat.requestPermissions(
            this, permissionsList.toArray(new String[permissionsList.size()]),
            REQUEST_PERMISSIONS);
      }
    }
  }

  @Override
  public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
      @NonNull int[] grantResults) {
    if (requestCode == REQUEST_PERMISSIONS) {
      // check if all permissions have been granted
      int i = 0;
      while (i < grantResults.length && grantResults[i] == PackageManager.PERMISSION_GRANTED) {
        i++;
      }
      if (grantResults.length > 0 && i >= grantResults.length) {
        // all permissions granted
        Toast.makeText(this, "Permission granted.", Toast.LENGTH_SHORT).show();
      }
    }
  }

  @Override
  public boolean onCreateOptionsMenu(Menu menu) {
    // Inflate the menu; this adds items to the action bar if it is present.
    getMenuInflater().inflate(R.menu.menu_main, menu);
    return true;
  }

  @Override
  public boolean onOptionsItemSelected(MenuItem item) {
    // Handle action bar item clicks here. The action bar will
    // automatically handle clicks on the Home/Up button, so long
    // as you specify a parent activity in AndroidManifest.xml.
    int id = item.getItemId();

    //noinspection SimplifiableIfStatement
    if (id == R.id.action_settings) {
      return true;
    }

    return super.onOptionsItemSelected(item);
  }

  public void swapFragment(Fragment fragment) {
    getSupportFragmentManager().beginTransaction()
        .replace(R.id.container, fragment)
        .commit();
  }
}
