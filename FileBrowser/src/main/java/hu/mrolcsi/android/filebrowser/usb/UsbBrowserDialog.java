package hu.mrolcsi.android.filebrowser.usb;

import android.annotation.TargetApi;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Parcelable;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.RecyclerView;
import android.view.View;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Stack;

import com.github.mjdev.libaums.UsbMassStorageDevice;
import com.github.mjdev.libaums.fs.FileSystem;
import com.github.mjdev.libaums.fs.UsbFile;
import com.github.mjdev.libaums.partition.Partition;
import hu.mrolcsi.android.filebrowser.BrowserDialog;
import hu.mrolcsi.android.filebrowser.BuildConfig;
import hu.mrolcsi.android.filebrowser.R;
import hu.mrolcsi.android.filebrowser.option.BrowseMode;
import hu.mrolcsi.android.filebrowser.util.Error;
import hu.mrolcsi.android.filebrowser.util.Utils;
import hu.mrolcsi.android.filebrowser.util.itemclicksupport.ItemClickSupport;

/**
 * Created by Matusinka Roland on 2016.04.13..
 */
@TargetApi(16)
public class UsbBrowserDialog extends BrowserDialog {

    private static final String TAG = "UsbBrowserDialog";
    private static final String ACTION_USB_PERMISSION = BuildConfig.APPLICATION_ID + ".USB_PERMISSION";
    private final Stack<UsbFile> mHistory = new Stack<>();
    private UsbMassStorageDevice mDevice;
    private AlertDialog mWaitingForUsbDialog;
    private OnDialogResultListener mOnDialogResultListener;
    private UsbFile mCurrentDir;
    private String mPathToRestore;
    private final BroadcastReceiver mUsbReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {

            String action = intent.getAction();
            if (ACTION_USB_PERMISSION.equals(action)) {

                UsbDevice device = intent.getParcelableExtra(UsbManager.EXTRA_DEVICE);
                if (intent.getBooleanExtra(UsbManager.EXTRA_PERMISSION_GRANTED, false)) {

                    if (device != null) {
                        setupDevice();
                    }
                }

            } else if (UsbManager.ACTION_USB_DEVICE_ATTACHED.equals(action)) {
                UsbDevice device = intent.getParcelableExtra(UsbManager.EXTRA_DEVICE);

                // determine if connected device is a mass storage device
                if (device != null) {
                    discoverDevice(intent);
                }
            } else if (UsbManager.ACTION_USB_DEVICE_DETACHED.equals(action)) {
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
            }
        }
    };

    @Override
    public void onCreate(Bundle savedInstanceState) {
        if (Build.VERSION.SDK_INT < 16) {
            throw new UnsupportedOperationException("USB Mass Storage is not supported under API Level 16");
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
                builder.setMessage("Please connect a USB Mass Storage device.");
                builder.setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        dismiss();
                    }
                });
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
            PendingIntent permissionIntent = PendingIntent.getBroadcast(getContext(), 0, new Intent(ACTION_USB_PERMISSION), 0);
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

            FileSystem fs = partitions.get(0).getFileSystem();
            mCurrentDir = fs.getRootDirectory();
            mHistory.clear();
            mHistory.push(fs.getRootDirectory());   //root is always first

            if (mPathToRestore != null) {
                final String[] paths = mPathToRestore.substring(1, mPathToRestore.length() - 1).split(File.separator);
                for (String dir : paths) {
                    mCurrentDir = mCurrentDir.search(dir);
                    mHistory.push(mCurrentDir);
                }
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
        ItemClickSupport.OnItemClickListener onItemClickListener = new ItemClickSupport.OnItemClickListener() {
            @Override
            public void onItemClick(RecyclerView parent, View view, int position, long id) {
                UsbFileListAdapter.UsbFileHolder holder = (UsbFileListAdapter.UsbFileHolder) view.getTag();

                if (holder.usbFile.getName().equals(getString(R.string.browser_upFolder))) {
                    //go up one dir
                    try {
                        mHistory.pop();
                        loadList(holder.usbFile.getParent());
                    } catch (IOException e) {
                        showErrorDialog(Error.FOLDER_NOT_READABLE);
                    }
                } else if (mBrowseMode == BrowseMode.SELECT_DIR && holder.usbFile.getName().equals(getString(R.string.browser_titleSelectDir))) {
                    mOnDialogResultListener.onPositiveResult(holder.usbFile);
                    dismiss();
                } else {
                    if (holder.usbFile.isDirectory()) {
                        try {
                            mHistory.push(holder.usbFile);
                            loadList(holder.usbFile);
                        } catch (IOException e) {
                            showErrorDialog(Error.FOLDER_NOT_READABLE);
                        }
                    } else {
                        if (mBrowseMode == BrowseMode.SAVE_FILE) {
                            etFilename.setText(holder.usbFile.getName());
                        } else {
                            mOnDialogResultListener.onPositiveResult(holder.usbFile);
                            dismiss();
                        }
                    }
                }
            }
        };

        ItemClickSupport.OnItemLongClickListener onItemLongClickListener = new ItemClickSupport.OnItemLongClickListener() {
            @Override
            public boolean onItemLongClick(RecyclerView parent, View view, int position, long id) {
                return false;
            }
        };

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
            if (mExtensionFilter != null) {
                for (UsbFile file : usbFiles) {
                    if (file.isDirectory()) continue;

                    String ext = Utils.getExtension(file.getName());
                    int i = 0;
                    int n = mExtensionFilter.length;
                    while (i < n && !mExtensionFilter[i].toLowerCase().equals(ext))
                        i++;
                    if (i < n) {
                        filesToLoad.add(file);
                    }
                }
            } else {
                filesToLoad = Arrays.asList(usbFiles);
            }
        }

        mCurrentDir = directory;

        mFileSorter = new UsbFileSorterTask(mSortMode) {
            private AlertDialog pd;

            @Override
            protected void onPreExecute() {
                super.onPreExecute();

                pd = Utils.showProgressDialog(getContext(), getString(R.string.browser_loadingFileList));
                pd.setCanceledOnTouchOutside(false);
                pd.setOnCancelListener(new DialogInterface.OnCancelListener() {
                    @Override
                    public void onCancel(DialogInterface dialogInterface) {
                        mFileSorter.cancel(true);
                        mFileSorter = null;
                    }
                });
                pd.show();
            }

            @Override
            protected void onPostExecute(List<UsbFile> files) {
                super.onPostExecute(files);

                pd.dismiss();

                mToolbar.setSubtitle(getCurrentPath());

                rvFileList.setAdapter(new UsbFileListAdapter(getContext(), mItemLayoutID, mBrowseMode, mSortMode, directory, files, mCurrentDir.isRoot()));

                Parcelable state = mStates.get(mCurrentDir.getName());
                if (state != null)
                    rvFileList.getLayoutManager().onRestoreInstanceState(state);

                mToolbar.getMenu().findItem(R.id.browser_menuNewFolder).setVisible(true);
            }

            @Override
            protected void onCancelled() {
                super.onCancelled();

                pd.dismiss();
            }
        }.execute(filesToLoad.toArray(new UsbFile[filesToLoad.size()]));
    }

    @Override
    protected void saveFile(boolean overwrite) {
        final String filename = checkExtension(etFilename.getText().toString());

        if (!filename.isEmpty() && Utils.isFilenameValid(filename)) {
            UsbFile existingFile;
            try {
                existingFile = mCurrentDir.search(filename);
                if (existingFile != null) {
                    if (overwrite) {
                        mOnDialogResultListener.onPositiveResult(existingFile);
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
                    mOnDialogResultListener.onPositiveResult(newFile);
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
        if (Utils.isFilenameValid(folderName)) {
            try {
                final UsbFile newDir = mCurrentDir.createDirectory(folderName);
                mHistory.push(newDir);
                loadList(newDir);
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
                .setIcon(Utils.tintDrawable(getContext(), R.drawable.browser_alphabetical_sorting_asc))
                .setItems(R.array.browser_sortOptions, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        mSortMode = SORT_HASHES[i];

                        setupSortMode();

                        try {
                            loadList(mCurrentDir);
                        } catch (IOException e) {
                            showErrorDialog(Error.FOLDER_NOT_READABLE);
                        }
                    }
                });
        AlertDialog ad = builder.create();
        ad.show();
    }

    public UsbBrowserDialog setOnDialogResultListener(OnDialogResultListener listener) {
        mOnDialogResultListener = listener;
        return this;
    }

    public interface OnDialogResultListener {
        void onPositiveResult(UsbFile file);

        void onNegativeResult();
    }
}
