/*
 * Copyright 2018 Roland Matusinka <http://github.com/mrolcsi>
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package hu.mrolcsi.android.filebrowser.usb;

import android.annotation.TargetApi;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Parcelable;
import android.support.v7.app.AlertDialog;
import android.text.TextUtils;
import com.github.mjdev.libaums.UsbMassStorageDevice;
import com.github.mjdev.libaums.fs.FileSystem;
import com.github.mjdev.libaums.fs.UsbFile;
import com.github.mjdev.libaums.partition.Partition;
import hu.mrolcsi.android.filebrowser.BrowserDialog;
import hu.mrolcsi.android.filebrowser.BuildConfig;
import hu.mrolcsi.android.filebrowser.R;
import hu.mrolcsi.android.filebrowser.option.BrowseMode;
import hu.mrolcsi.android.filebrowser.option.SortMode;
import hu.mrolcsi.android.filebrowser.util.Error;
import hu.mrolcsi.android.filebrowser.util.FileUtils;
import hu.mrolcsi.android.filebrowser.util.Utils;
import hu.mrolcsi.android.filebrowser.util.itemclicksupport.ItemClickSupport;
import java.io.File;
import java.io.IOException;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Stack;

/**
 * Created by Matusinka Roland on 2016.04.13..
 */
@TargetApi(16)
public class UsbBrowserDialog extends BrowserDialog {

  private static final String ACTION_USB_PERMISSION = BuildConfig.APPLICATION_ID + ".USB_PERMISSION";

  private final Stack<UsbFile> mHistory = new Stack<>();

  private UsbMassStorageDevice mDevice;
  private AlertDialog mWaitingForUsbDialog;
  private OnDialogResultListener mOnDialogResultListener = new OnDialogResultListener() {
    @Override
    public void onPositiveResult(UsbFile file, FileSystem currentFs) {
    }

    @Override
    public void onNegativeResult() {
    }
  };
  private OnFileSelectedListener mOnFileSelectedListener = (file, fileSystem) -> {
    if (mOnDialogResultListener != null) {
      mOnDialogResultListener.onPositiveResult(file, fileSystem);
    }
  };
  private UsbFile mCurrentDir;
  private FileSystem mFileSystem;
  private String mStartPath = File.separator;
  private String mRootPath = File.separator;
  private String mPathToRestore;
  private final BroadcastReceiver mUsbReceiver = new BroadcastReceiver() {
    @Override
    public void onReceive(Context context, Intent intent) {

      String action = intent.getAction();
      switch (action) {
        case ACTION_USB_PERMISSION: {
          UsbDevice device = intent.getParcelableExtra(UsbManager.EXTRA_DEVICE);
          if (intent.getBooleanExtra(UsbManager.EXTRA_PERMISSION_GRANTED, false)) {
            if (device != null) {
              setupDevice();
            }
          }
          break;
        }
        case UsbManager.ACTION_USB_DEVICE_ATTACHED: {
          UsbDevice device = intent.getParcelableExtra(UsbManager.EXTRA_DEVICE);

          // determine if connected device is a mass storage device
          if (device != null) {
            discoverDevice(intent);
          }
          break;
        }
        case UsbManager.ACTION_USB_DEVICE_DETACHED: {
          UsbDevice device = intent.getParcelableExtra(UsbManager.EXTRA_DEVICE);

          // determine if connected device is a mass storage device
          if (device != null) {
            if (UsbBrowserDialog.this.mDevice != null) {
              UsbBrowserDialog.this.mDevice.close();
            }
            // check if there are other devices or set action bar title
            // to no device if not
            discoverDevice(intent);
          }
          break;
        }
      }
    }
  };

  @Override
  public void onCreate(Bundle savedInstanceState) {
    if (Build.VERSION.SDK_INT < 16) {
      throw new UnsupportedOperationException(
          "USB Mass Storage is not supported under API Level 16");
    }

    if (savedInstanceState != null) {
      mPathToRestore = savedInstanceState.getString("currentPath");
    }

    super.onCreate(savedInstanceState);
  }

  @Override
  public void onStart() {
    super.onStart();

    IntentFilter filter = new IntentFilter(ACTION_USB_PERMISSION);
    filter.addAction(UsbManager.ACTION_USB_DEVICE_ATTACHED);
    filter.addAction(UsbManager.ACTION_USB_DEVICE_DETACHED);
    getContext().registerReceiver(mUsbReceiver, filter);
    discoverDevice(getActivity().getIntent());
  }

  @Override
  public void onStop() {
    super.onStop();

    getContext().unregisterReceiver(mUsbReceiver);
  }

  /**
   * Searches for connected mass storage devices, and initializes them if it
   * could find some.
   */
  private void discoverDevice(Intent intent) {
    UsbManager usbManager = (UsbManager) getContext().getSystemService(Context.USB_SERVICE);
    UsbMassStorageDevice[] devices = UsbMassStorageDevice.getMassStorageDevices(getContext());

    if (devices.length == 0) {
      if (mWaitingForUsbDialog == null) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
        builder.setCancelable(false);
        builder.setMessage(R.string.browser_pleaseConnectUsb);
        builder.setNegativeButton(android.R.string.cancel, (dialogInterface, i) -> dismiss());
        mWaitingForUsbDialog = builder.create();
      }

      rvFileList.setAdapter(null);
      mWaitingForUsbDialog.show();
      return;
    }

    // we only use the first device
    mDevice = devices[0];

    UsbDevice usbDevice = intent.getParcelableExtra(UsbManager.EXTRA_DEVICE);

    if (usbDevice != null && usbManager.hasPermission(usbDevice)) {
      // requesting permission is not needed in this case
      setupDevice();
    } else {
      // first request permission from user to communicate with the
      // underlying
      // UsbDevice
      PendingIntent permissionIntent = PendingIntent
          .getBroadcast(getContext(), 0, new Intent(ACTION_USB_PERMISSION), 0);
      usbManager.requestPermission(mDevice.getUsbDevice(), permissionIntent);
    }
  }

  /**
   * Sets the device up and shows the contents of the root directory.
   */
  private void setupDevice() {
    try {
      if (mWaitingForUsbDialog != null) {
        mWaitingForUsbDialog.dismiss();
      }
      mDevice.init();

      // we always use the first partition of the device
      final List<Partition> partitions = mDevice.getPartitions();
      if (partitions.size() < 1) {
        showErrorDialog(Error.USB_ERROR, getString(R.string.browser_error_usbError_onlyFat32partitions));
        dismiss();
        return;
      }

      mFileSystem = partitions.get(0).getFileSystem();
      mHistory.clear();

      if (mPathToRestore != null) {
        final String[] paths = mPathToRestore.substring(1, mPathToRestore.length() - 1).split(File.separator);
        for (String dir : paths) {
          mCurrentDir = mCurrentDir.search(dir);
          mHistory.push(mCurrentDir);
        }
      } else if (getStartPath() != null) {
        if (mStartPath.startsWith("/")) {
          mStartPath = mStartPath.substring(1, mStartPath.length());
        }
        mCurrentDir = mFileSystem.getRootDirectory().search(getStartPath());
        if (mCurrentDir == null) {
          // create directory
          mCurrentDir = mFileSystem.getRootDirectory();
          mHistory.push(mCurrentDir);
          final String[] paths = getStartPath().split(File.separator);
          for (String path : paths) {
            if (!TextUtils.isEmpty(path)) {
              mCurrentDir = mCurrentDir.createDirectory(path);
              mHistory.push(mCurrentDir);
            }
          }
        } else {
          // build history from start path
          List<UsbFile> parents = new ArrayList<>();
          UsbFile dir = mCurrentDir;
          while (dir != null) {
            parents.add(dir);
            dir = dir.getParent();
          }
          Collections.reverse(parents);
          for (UsbFile parent : parents) {
            mHistory.push(parent);
          }
        }
      } else {
        mCurrentDir = mFileSystem.getRootDirectory();
        mHistory.push(mCurrentDir);
      }

      loadList(mCurrentDir);
    } catch (IOException e) {
      showErrorDialog(Error.USB_ERROR);
    }
  }

  @Override
  public void onSaveInstanceState(Bundle outState) {
    super.onSaveInstanceState(outState);
    outState.putString("currentPath", getCurrentPath());
  }

  @Override
  protected void setListListeners() {
    ItemClickSupport.OnItemClickListener onItemClickListener = (parent, view, position, id) -> {
      UsbFileListAdapter.UsbFileHolder holder = (UsbFileListAdapter.UsbFileHolder) view.getTag();

      if (holder.usbFile.getName().equals(getString(R.string.browser_upFolder))) {
        //go up one dir
        try {
          mHistory.pop();
          loadList(holder.usbFile.getParent());
        } catch (IOException e) {
          showErrorDialog(Error.FOLDER_NOT_READABLE);
        }
      } else if (mBrowseMode == BrowseMode.SELECT_DIR
          && holder.usbFile.getName().equals(getString(R.string.browser_titleSelectDir))) {
        mOnFileSelectedListener.onFileSelected(holder.usbFile.getParent(), mFileSystem);
        dismiss();
      } else {
        if (holder.usbFile.isDirectory()) {
          if (mLocked && mBrowseMode == BrowseMode.SELECT_DIR) {
            mOnFileSelectedListener.onFileSelected(holder.usbFile, mFileSystem);
            dismiss();
          } else {
            try {
              mHistory.push(holder.usbFile);
              loadList(holder.usbFile);
            } catch (IOException e) {
              showErrorDialog(Error.FOLDER_NOT_READABLE);
            }
          }
        } else if (!holder.usbFile.isDirectory() && holder.usbFile.getLength() != -1) {
          if (mBrowseMode == BrowseMode.SAVE_FILE) {
            etFilename.setText(holder.usbFile.getName());
          } else {
            mOnFileSelectedListener.onFileSelected(holder.usbFile, mFileSystem);
            dismiss();
          }
        }
      }
    };

    ItemClickSupport.OnItemLongClickListener onItemLongClickListener = (parent, view, position, id) -> false;

    mItemClickSupport.setOnItemClickListener(onItemClickListener);
    mItemClickSupport.setOnItemLongClickListener(onItemLongClickListener);
  }

  @Override
  protected void loadList(File directory) {
    // do nothing
  }

  protected void toListView() {
    super.toListView();
    if (mCurrentDir != null) {
      try {
        loadList(mCurrentDir);
      } catch (IOException e) {
        showErrorDialog(Error.FOLDER_NOT_READABLE);
      }
    }
  }

  protected void toGridView() {
    super.toGridView();
    if (mCurrentDir != null) {
      try {
        loadList(mCurrentDir);
      } catch (IOException e) {
        showErrorDialog(Error.FOLDER_NOT_READABLE);
      }
    }
  }

  protected void showHiddenFiles() {
    mShowHiddenFiles = true;
    try {
      loadList(mCurrentDir);
      menuShowHiddenFiles.setTitle(R.string.browser_menu_dontShowHiddenFiles);
      menuShowHiddenFiles.setIcon(Utils.getTintedDrawable(getContext(), R.drawable.browser_hide));
    } catch (IOException e) {
      showErrorDialog(Error.FOLDER_NOT_READABLE);
    }
  }

  protected void dontShowHiddenFiles() {
    mShowHiddenFiles = false;
    try {
      loadList(mCurrentDir);
      menuShowHiddenFiles.setTitle(R.string.browser_menu_showHiddenFiles);
      menuShowHiddenFiles.setIcon(Utils.getTintedDrawable(getContext(), R.drawable.browser_show));
    } catch (IOException e) {
      showErrorDialog(Error.FOLDER_NOT_READABLE);
    }

  }

  private void loadList(final UsbFile directory) throws IOException {
    //assume it's readable

    mStates.put(mCurrentDir.getName(), rvFileList.getLayoutManager().onSaveInstanceState());

    if (mFileSorter != null) {
      mFileSorter.cancel(true);
      mFileSorter = null;
    }

    List<UsbFile> filesToLoad = new ArrayList<>();

    if (mBrowseMode == BrowseMode.OPEN_FILE || mBrowseMode == BrowseMode.SAVE_FILE) {
      final UsbFile[] usbFiles = directory.listFiles();
      for (UsbFile file : usbFiles) {
        if (file.getName().startsWith(".") && !mShowHiddenFiles) {
          // skip hidden file
          continue;
        }

        if (file.isDirectory()) {
          filesToLoad.add(file);
          continue;
        }

        if (mExtensionFilter != null) {
          String ext = FileUtils.getExtension(file.getName());
          int i = 0;
          int n = mExtensionFilter.length;
          while (i < n && !mExtensionFilter[i].toLowerCase(Locale.getDefault()).equals(ext)) {
            i++;
          }
          if (i < n) {
            filesToLoad.add(file);
          }
        } else {
          filesToLoad.add(file);
        }
      }
    } else {
      final UsbFile[] usbFiles = directory.listFiles();
      for (UsbFile file : usbFiles) {
        if (file.isDirectory()) {
          filesToLoad.add(file);
        }
      }
    }

    mCurrentDir = directory;

    mFileSorter = new InnerUsbFileSorterTask(this, mSortMode)
        .execute(filesToLoad.toArray(new UsbFile[filesToLoad.size()]));
  }

  @Override
  protected void saveFile(boolean overwrite) {
    final String filename = checkExtension(etFilename.getText().toString());

    if (!filename.isEmpty() && FileUtils.isFilenameValid(filename)) {
      UsbFile existingFile;
      try {
        existingFile = mCurrentDir.search(filename);
        if (existingFile != null) {
          if (overwrite) {
            mOnFileSelectedListener.onFileSelected(existingFile, mFileSystem);
            dismiss();
          } else {
            showOverwriteDialog(filename);
          }
        }
      } catch (IOException e) {
        showErrorDialog(Error.FOLDER_NOT_READABLE);
        return;
      }

      if (existingFile == null) {
        try {
          final UsbFile newFile = mCurrentDir.createFile(filename);
          mOnFileSelectedListener.onFileSelected(newFile, mFileSystem);
          dismiss();
        } catch (IOException e) {
          showErrorDialog(Error.CANT_CREATE_FILE);
        }
      }
    } else {
      showErrorDialog(Error.INVALID_FILENAME);
    }
  }

  @Override
  protected void createFolder(String folderName) {
    if (FileUtils.isFilenameValid(folderName)) {
      try {
        final UsbFile newDir = mCurrentDir.createDirectory(folderName);
        if (mLocked) {
          // reload current directory to see newly created folder
          loadList(mCurrentDir);
        } else {
          mHistory.push(newDir);
          loadList(newDir);
        }
      } catch (IOException e) {
        showErrorDialog(Error.CANT_CREATE_FOLDER);
      }
    } else {
      showErrorDialog(Error.INVALID_FOLDERNAME);
    }
  }

  private String getCurrentPath() {
    StringBuilder sb = new StringBuilder();

    for (UsbFile usbFile : mHistory) {
      sb.append(usbFile.getName());
      sb.append(File.separator);
    }

    return sb.toString();
  }

  protected void showSortDialog() {
    AlertDialog.Builder builder = new AlertDialog.Builder(getContext())
        .setTitle(R.string.browser_menu_sortBy)
        .setIcon(Utils.getTintedDrawable(getContext(), R.drawable.browser_alphabetical_sorting_asc))
        .setItems(R.array.browser_sortOptions, (dialogInterface, i) -> {
          mSortMode = SORT_HASHES[i];

          setupSortMode();

          try {
            loadList(mCurrentDir);
          } catch (IOException e) {
            showErrorDialog(Error.FOLDER_NOT_READABLE);
          }
        });
    AlertDialog ad = builder.create();
    ad.show();
  }

  @Override
  public String getStartPath() {
    return mStartPath;
  }

  @Override
  public BrowserDialog setStartPath(String startPath) {
    mStartPath = startPath;
    return this;
  }

  @Override
  public String getRootPath() {
    return mRootPath;
  }

  @Override
  public BrowserDialog setRootPath(String rootPath) {
    mRootPath = rootPath;
    return this;
  }

  /**
   * Deprecated. Use {@link #setOnFileSelectedListener(OnFileSelectedListener)} instead.
   */
  @Deprecated
  public UsbBrowserDialog setOnDialogResultListener(OnDialogResultListener listener) {
    mOnDialogResultListener = listener;
    return this;
  }

  public UsbBrowserDialog setOnFileSelectedListener(OnFileSelectedListener listener) {
    mOnFileSelectedListener = listener;
    return this;
  }

  private static class InnerUsbFileSorterTask extends UsbFileSorterTask {

    private final WeakReference<UsbBrowserDialog> wrDialog;

    private AlertDialog pd;

    public InnerUsbFileSorterTask(UsbBrowserDialog dialog, SortMode sortMode) {
      super(sortMode);
      this.wrDialog = new WeakReference<>(dialog);
    }

    @Override
    protected void onPreExecute() {
      super.onPreExecute();

      pd = Utils
          .showProgressDialog(wrDialog.get().getContext(), wrDialog.get().getString(R.string.browser_loadingFileList));
      pd.setCanceledOnTouchOutside(false);
      pd.setOnCancelListener(dialogInterface -> {
        wrDialog.get().mFileSorter.cancel(true);
        wrDialog.get().mFileSorter = null;
      });
      pd.show();
    }

    @Override
    protected void onPostExecute(List<UsbFile> files) {
      super.onPostExecute(files);

      pd.dismiss();

      wrDialog.get().mToolbar.setSubtitle(wrDialog.get().getCurrentPath());

      final String currentDirPath = FileUtils.getAbsolutePath(wrDialog.get().mCurrentDir);
      boolean isRoot = currentDirPath.equals(wrDialog.get().mRootPath);

      wrDialog.get().rvFileList.setAdapter(new UsbFileListAdapter(
          wrDialog.get().getContext(),
          wrDialog.get().mItemLayoutId,
          wrDialog.get().mLocked ? BrowseMode.OPEN_FILE : wrDialog.get().mBrowseMode,
          wrDialog.get().mSortMode,
          wrDialog.get().mCurrentDir,
          files,
          isRoot));

      Parcelable state = wrDialog.get().mStates.get(wrDialog.get().mCurrentDir.getName());
      if (state != null) {
        wrDialog.get().rvFileList.getLayoutManager().onRestoreInstanceState(state);
      }

      wrDialog.get().mToolbar.getMenu().findItem(R.id.browser_menuNewFolder).setVisible(true);
    }

    @Override
    protected void onCancelled() {
      super.onCancelled();

      pd.dismiss();
    }
  }

  /**
   * Deprecated. Use {@link OnFileSelectedListener} instead.
   */
  @SuppressWarnings({"EmptyMethod", "unused"})
  @Deprecated
  public interface OnDialogResultListener {

    void onPositiveResult(UsbFile file, FileSystem currentFs);

    void onNegativeResult();
  }

  @FunctionalInterface
  public interface OnFileSelectedListener {

    void onFileSelected(UsbFile file, FileSystem fileSystem);
  }
}
