package hu.mrolcsi.android.filebrowser;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.os.Environment;
import android.os.Parcelable;
import android.support.annotation.NonNull;
import android.support.v4.app.DialogFragment;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.*;
import android.view.inputmethod.EditorInfo;
import android.widget.*;
import hu.mrolcsi.android.filebrowser.option.BrowseMode;
import hu.mrolcsi.android.filebrowser.option.Layout;
import hu.mrolcsi.android.filebrowser.option.SortMode;
import hu.mrolcsi.android.filebrowser.util.DividerItemDecoration;
import hu.mrolcsi.android.filebrowser.util.Error;
import hu.mrolcsi.android.filebrowser.util.Utils;
import org.lucasr.twowayview.ItemClickSupport;

import java.io.File;
import java.io.FileFilter;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Created with IntelliJ IDEA.
 * User: Matusinka Roland
 * Date: 2013.07.30.
 * Time: 11:34
 */

public class BrowserDialog extends DialogFragment {

    //region Publics
    /**
     * Browsing mode:
     * <ul>
     * <li>Open File: {@link hu.mrolcsi.android.filebrowser.option.BrowseMode#OPEN_FILE OPEN_FILE}</li>
     * <li>Select Directory: {@link hu.mrolcsi.android.filebrowser.option.BrowseMode#SELECT_DIR SELECT_DIR}</li>
     * <li>Save File: {@link hu.mrolcsi.android.filebrowser.option.BrowseMode#SAVE_FILE SAVE_FILE}</li>
     * </ul>
     * Default: open file
     *
     * @see hu.mrolcsi.android.filebrowser.option.BrowseMode
     */
    public static final String OPTION_BROWSE_MODE;
    /**
     * String:  Absolute path to starting directory (Default: root of EXTERNAL_STORAGE (SD-Card) or "/")
     */
    public static final String OPTION_START_PATH;
    /**
     * String:  File extensions to show separated with semicolons (;) (Default: empty (*.*))
     */
    public static final String OPTION_EXTENSION_FILTER;
    /**
     * Return value: Absolute path to selected file/directory
     */
    public static final String RESULT;
    /**
     * Sort Mode: (Directories always have priority before files)
     * <ul>
     * <li>By filename (ascending): {@link hu.mrolcsi.android.filebrowser.option.SortMode#BY_NAME_ASC BY_NAME_ASC}</li>
     * <li>By filename (descending): {@link hu.mrolcsi.android.filebrowser.option.SortMode#BY_NAME_DESC BY_NAME_DESC}</li>
     * <li>By extension (ascending): {@link hu.mrolcsi.android.filebrowser.option.SortMode#BY_EXTENSION_ASC BY_EXTENSION_ASC}</li>
     * <li>By extension (descending): {@link hu.mrolcsi.android.filebrowser.option.SortMode#BY_EXTENSION_DESC BY_EXTENSION_DESC}</li>
     * <li>By modification date (ascending): {@link hu.mrolcsi.android.filebrowser.option.SortMode#BY_DATE_ASC BY_DATE_ASC}</li>
     * <li>By modification date (descending): {@link hu.mrolcsi.android.filebrowser.option.SortMode#BY_DATE_DESC BY_DATE_DESC}</li>
     * <li>By size (ascending): {@link hu.mrolcsi.android.filebrowser.option.SortMode#BY_SIZE_ASC BY_SIZE_ASC}</li>
     * <li>By size (descending): {@link hu.mrolcsi.android.filebrowser.option.SortMode#BY_SIZE_DESC BY_SIZE_DESC}</li>
     * </ul>
     * Default: by filename (ascending)
     *
     * @see hu.mrolcsi.android.filebrowser.option.SortMode
     */
    public static final String OPTION_SORT_MODE;
    /**
     * String:  Default filename; only used when saving.
     *
     * @see hu.mrolcsi.android.filebrowser.option.BrowseMode
     */
    public static final String OPTION_DEFAULT_FILENAME;
    /**
     * Boolean: Should the specified start path be considered as Root?
     *
     * @see #OPTION_START_PATH
     */
    public static final String OPTION_START_IS_ROOT;
    /**
     * Starting layout (can be change at runtime)
     * <ul>
     * <li>List {@link hu.mrolcsi.android.filebrowser.option.Layout#LIST LIST}</li>
     * <li>Grid {@link hu.mrolcsi.android.filebrowser.option.Layout#GRID GRID}</li>
     * </ul>
     * Default: list
     *
     * @see hu.mrolcsi.android.filebrowser.option.Layout
     */
    public static final String OPTION_LAYOUT;
    public static final SortMode[] SORT_HASHES = new SortMode[]{
            SortMode.BY_NAME_ASC,
            SortMode.BY_NAME_DESC,
            SortMode.BY_EXTENSION_ASC,
            SortMode.BY_EXTENSION_DESC,
            SortMode.BY_DATE_ASC,
            SortMode.BY_DATE_DESC,
            SortMode.BY_SIZE_ASC,
            SortMode.BY_SIZE_DESC
    };
    //endregion

    static {
        OPTION_START_IS_ROOT = "startIsRoot";
        OPTION_DEFAULT_FILENAME = "defaultFileName";
        OPTION_SORT_MODE = "sort";
        RESULT = "result";
        OPTION_EXTENSION_FILTER = "extensionFilter";
        OPTION_START_PATH = "startPath";
        OPTION_BROWSE_MODE = "browseMode";
        OPTION_LAYOUT = "layout";
    }

    //region Privates
    private BrowseMode mBrowseMode = BrowseMode.OPEN_FILE;
    private SortMode mSortMode = SortMode.BY_NAME_ASC;
    private String[] mExtensionFilter;
    private String mDefaultFileName;
    private String mCurrentExtension;
    private String mStartPath = Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED) ? Environment.getExternalStorageDirectory().getAbsolutePath() : "/";
    private String mRootPath = File.listRoots()[0].getAbsolutePath();
    private String mCurrentPath = mStartPath;
    private boolean mStartIsRoot = true;
    private Layout mActiveLayout = Layout.LIST;
    private int mItemLayoutID = R.layout.browser_listitem_layout;
    private boolean mOverwrite = false;
    private Map<String, Parcelable> mStates = new ConcurrentHashMap<>();

    private RecyclerView rvFileList;
    private ImageButton btnSave;
    private EditText etFilename;
    private LinearLayoutManager mLinearLayout;
    private GridLayoutManager mGridLayout;
    private ItemClickSupport mItemClickSupport;
    private DividerItemDecoration mListItemDecor;
    private Toolbar mToolbar;
    private OnDialogResultListener onDialogResultListener = new OnDialogResultListener() {
        @Override
        public void onPositiveResult(String path) {
        }

        @Override
        public void onNegativeResult() {
        }
    };
    //</editor-fold>


    public BrowserDialog() {
        super();
        setStyle(STYLE_NO_TITLE, 0);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        if (savedInstanceState != null) {
            mStartPath = savedInstanceState.getString(OPTION_START_PATH);
            mCurrentPath = savedInstanceState.getString("currentPath");
            mBrowseMode = (BrowseMode) savedInstanceState.getSerializable(OPTION_BROWSE_MODE);
            mSortMode = (SortMode) savedInstanceState.getSerializable(OPTION_SORT_MODE);
            mExtensionFilter = savedInstanceState.getStringArray(OPTION_EXTENSION_FILTER);
            mStartIsRoot = savedInstanceState.getBoolean(OPTION_START_IS_ROOT, true);
            mActiveLayout = (Layout) savedInstanceState.getSerializable(OPTION_LAYOUT);
            mItemLayoutID = savedInstanceState.getInt("itemLayoutID");
            mDefaultFileName = savedInstanceState.getString(OPTION_DEFAULT_FILENAME);
        } else {
            mCurrentPath = mStartPath;
        }
        super.onCreate(savedInstanceState);
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        final Dialog dialog = super.onCreateDialog(savedInstanceState);
        dialog.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE);
        return dialog;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.browser_layout_dialog, container, false);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {

        mToolbar = (Toolbar) view.findViewById(R.id.browser_toolbar);
        mToolbar.inflateMenu(R.menu.browser_menu);
        mToolbar.setTitle(R.string.browser_currentDirectory);
        mToolbar.setOnMenuItemClickListener(new Toolbar.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem menuItem) {
                final int id = menuItem.getItemId();

                if (id == R.id.browser_menuNewFolder) {
                    showNewFolderDialog();
                    return true;
                } else if (id == R.id.browser_menuSort) {
                    showSortDialog();
                    return true;
                } else if (id == R.id.browser_menuSwitchLayout) {
                    if (mActiveLayout == Layout.LIST) {
                        menuItem.setTitle(R.string.browser_menu_viewAsList);
                        menuItem.setIcon(R.drawable.browser_list_dark);
                        toGridView();
                    } else if (mActiveLayout == Layout.GRID) {
                        menuItem.setTitle(R.string.browser_menu_viewAsGrid);
                        menuItem.setIcon(R.drawable.browser_grid_dark);
                        toListView();
                    }
                    return true;
                }
                return false;
            }
        });

        rvFileList = (RecyclerView) view.findViewById(R.id.browser_recyclerView);
        mListItemDecor = new DividerItemDecoration(getActivity(), DividerItemDecoration.VERTICAL_LIST);
        mLinearLayout = new LinearLayoutManager(getActivity(), LinearLayoutManager.VERTICAL, false);
        mGridLayout = new GridLayoutManager(getActivity(), getResources().getInteger(R.integer.browser_columnCount), LinearLayoutManager.VERTICAL, false);
        mItemClickSupport = ItemClickSupport.addTo(rvFileList);
        setListListeners();

        switch (mActiveLayout) {
            default:
            case LIST:
                toListView();
                break;
            case GRID:
                toGridView();
                break;
        }

        if (mBrowseMode == BrowseMode.SAVE_FILE) {
            btnSave = (btnSave == null) ? (ImageButton) view.findViewById(R.id.browser_imageButtonSave) : btnSave;
            etFilename = (etFilename == null) ? (EditText) view.findViewById(R.id.browser_editTextFileName) : etFilename;

            etFilename.setOnEditorActionListener(new TextView.OnEditorActionListener() {
                @Override
                public boolean onEditorAction(TextView textView, int i, KeyEvent keyEvent) {
                    if (i == EditorInfo.IME_ACTION_DONE) {
                        sendResult();
                        return true;
                    }
                    return false;
                }
            });

            RelativeLayout rlSave = (RelativeLayout) view.findViewById(R.id.browser_rlSave);
            rlSave.setVisibility(View.VISIBLE);

            final Spinner spnExtension = (Spinner) view.findViewById(R.id.browser_spnExtension);
            if (mExtensionFilter != null) {
                final ArrayAdapter<String> extensionAdapter = new ArrayAdapter<>(getActivity(), android.R.layout.simple_spinner_item, mExtensionFilter);
                extensionAdapter.setDropDownViewResource(R.layout.browser_dropdown_item);
                spnExtension.setAdapter(extensionAdapter);
                spnExtension.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                    @Override
                    public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
                        mCurrentExtension = (String) spnExtension.getSelectedItem();
                    }

                    @Override
                    public void onNothingSelected(AdapterView<?> adapterView) {
                    }
                });
            } else spnExtension.setVisibility(View.GONE);

            if (mDefaultFileName != null) etFilename.setText(mDefaultFileName);

            btnSave.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    sendResult();
                }
            });
        }
    }

    private void sendResult() {
        final String filename = checkExtension(etFilename.getText().toString());
        String result = mCurrentPath + "/" + filename;

        if (!result.isEmpty() && Utils.isFilenameValid(result)) {
            File f = new File(result);
            if (f.exists()) {
                if (!mOverwrite) {
                    Toast.makeText(getActivity(), getString(R.string.browser_confirmOverwrite), Toast.LENGTH_SHORT).show();
                    mOverwrite = true;
                    //TODO: ellenőrizni
                } else {
                    onDialogResultListener.onPositiveResult(result);
                    dismiss();
                }
            } else {
                onDialogResultListener.onPositiveResult(result);
                dismiss();
            }
        } else {
            showErrorDialog(hu.mrolcsi.android.filebrowser.util.Error.INVALID_FILENAME);
        }
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        outState.putString("currentPath", mCurrentPath);
        outState.putSerializable(OPTION_BROWSE_MODE, mBrowseMode);
        outState.putSerializable(OPTION_SORT_MODE, mSortMode);
        outState.putStringArray(OPTION_EXTENSION_FILTER, mExtensionFilter);
        outState.putString(OPTION_START_PATH, mStartPath);
        outState.putBoolean(OPTION_START_IS_ROOT, mStartIsRoot);
        outState.putSerializable(OPTION_LAYOUT, mActiveLayout);
        outState.putInt("itemLayoutID", mItemLayoutID);
        outState.putString(OPTION_DEFAULT_FILENAME, mDefaultFileName);
        super.onSaveInstanceState(outState);
    }

    /**
     * Lista nézetbe váltás ViewFlipperen keresztül.
     */
    private void toListView() {
        mActiveLayout = Layout.LIST;
        mItemLayoutID = R.layout.browser_listitem_layout;
        rvFileList.addItemDecoration(mListItemDecor);
        rvFileList.setLayoutManager(mLinearLayout);
        loadList(new File(mCurrentPath));
    }

    /**
     * Grid nézetbe váltás ViewFlipperen keresztül.
     */
    private void toGridView() {
        mActiveLayout = Layout.GRID;
        rvFileList.setLayoutManager(mGridLayout);
        rvFileList.removeItemDecoration(mListItemDecor);
        mItemLayoutID = R.layout.browser_griditem_layout;
        loadList(new File(mCurrentPath));
    }

    /**
     * Dialógus megjelenítése a rendezési mód kiválasztásához.
     */
    private void showSortDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity())
                .setTitle(R.string.browser_menu_sortBy)
                .setIcon(R.drawable.browser_alphabetical_sorting_dark)
                .setItems(R.array.browser_sortOptions, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        mSortMode = SORT_HASHES[i];
                        loadList(new File(mCurrentPath));
                    }
                });
        AlertDialog ad = builder.create();
        ad.show();
    }

    /**
     * View váltás után listenerek újraregisztrálása.
     */
    private void setListListeners() {
        switch (mBrowseMode) {
            default:
            case OPEN_FILE:
                mItemClickSupport.setOnItemClickListener(new ItemClickSupport.OnItemClickListener() {
                    @Override
                    public void onItemClick(RecyclerView recyclerView, View view, int i, long l) {
                        FileHolder holder = (FileHolder) view.getTag();
                        if (holder.file.getAbsolutePath().equals(File.separator + getString(R.string.browser_upFolder))) {
                            loadList(new File(mCurrentPath).getParentFile());
                        } else {
                            if (holder.file.isDirectory()) loadList(holder.file);
                            if (holder.file.isFile()) {
                                onDialogResultListener.onPositiveResult(holder.file.getAbsolutePath());
                                dismiss();
                            }
                        }
                    }
                });
                mItemClickSupport.setOnItemLongClickListener(new ItemClickSupport.OnItemLongClickListener() {
                    @Override
                    public boolean onItemLongClick(RecyclerView recyclerView, View view, int i, long l) {
                        FileHolder holder = (FileHolder) view.getTag();
                        if (holder.file.isFile()) {
                            onDialogResultListener.onPositiveResult(holder.file.getAbsolutePath());
                            dismiss();
                        }
                        return holder.file.isFile();
                    }
                });
                break;
            case SELECT_DIR:
                mItemClickSupport.setOnItemClickListener(new ItemClickSupport.OnItemClickListener() {
                    @Override
                    public void onItemClick(RecyclerView recyclerView, View view, int i, long l) {
                        FileHolder holder = (FileHolder) view.getTag();
                        if (holder.file.getAbsolutePath().equals(File.separator + getString(R.string.browser_upFolder))) {
                            loadList(new File(mCurrentPath).getParentFile());
                        } else if (holder.file.isDirectory()) {
                            loadList(holder.file);
                        } else if (holder.file.getAbsolutePath().equals(File.separator + getString(R.string.browser_titleSelectDir))) {
                            onDialogResultListener.onPositiveResult(mCurrentPath);
                            dismiss();
                        }
                    }
                });
                mItemClickSupport.setOnItemLongClickListener(new ItemClickSupport.OnItemLongClickListener() {
                    @Override
                    public boolean onItemLongClick(RecyclerView recyclerView, View view, int i, long l) {
                        FileHolder holder = (FileHolder) view.getTag();
                        if (holder.file.isDirectory()) {
                            onDialogResultListener.onPositiveResult(holder.file.getAbsolutePath());
                            dismiss();
                        }
                        return true;
                    }
                });
                break;
            case SAVE_FILE:
                mItemClickSupport.setOnItemClickListener(new ItemClickSupport.OnItemClickListener() {
                    @Override
                    public void onItemClick(RecyclerView recyclerView, View view, int i, long l) {
                        FileHolder holder = (FileHolder) view.getTag();
                        if (holder.file.getAbsolutePath().equals(File.separator + getString(R.string.browser_upFolder))) {
                            loadList(new File(mCurrentPath).getParentFile());
                        } else {
                            if (holder.file.isFile()) etFilename.setText(holder.file.getName());
                            if (holder.file.isDirectory()) loadList(holder.file);
                        }
                    }
                });
                mItemClickSupport.setOnItemLongClickListener(new ItemClickSupport.OnItemLongClickListener() {
                    @Override
                    public boolean onItemLongClick(RecyclerView recyclerView, View view, int i, long l) {
                        FileHolder holder = (FileHolder) view.getTag();
                        if (!holder.file.isFile()) return false;
                        else {
                            showOverwriteDialog(holder.file.getAbsolutePath());
                            Toast.makeText(getActivity(), R.string.browser_confirmOverwrite, Toast.LENGTH_LONG).show();
                            return true;
                        }
                    }
                });

        }
    }

    /**
     * Fájlok listájának betöltése a ListView/GridView-ba.
     *
     * @param directory A betöltendő mappa.
     */
    private void loadList(final File directory) {
        if (!directory.canRead()) {
            showErrorDialog(Error.FOLDER_NOT_READABLE);
            return;
        }

        mStates.put(mCurrentPath, rvFileList.getLayoutManager().onSaveInstanceState());

        File[] filesToLoad;

        if (mExtensionFilter != null) filesToLoad = directory.listFiles(new FileFilter() {
            @Override
            public boolean accept(File file) {
                if (file.isFile()) {
                    String ext = Utils.getExtension(file.getName());
                    int i = 0;
                    int n = mExtensionFilter.length;
                    while (i < n && !mExtensionFilter[i].toLowerCase().equals(ext)) i++;
                    return i < n;
                } else return file.canRead();
            }
        });
        else filesToLoad = directory.listFiles(new FileFilter() {
            @Override
            public boolean accept(File file) {
                return file.canRead();
            }
        });

        mCurrentPath = directory.getAbsolutePath();
        mToolbar.setSubtitle(mCurrentPath);

        FileListAdapter fla;
        boolean isRoot = mStartIsRoot ? mCurrentPath.equals(mStartPath) || mCurrentPath.equals(mRootPath) : mCurrentPath.equals(mRootPath);

        Log.d(getClass().getName(), "root path = " + mRootPath);
        Log.d(getClass().getName(), "start path = " + mStartPath);
        Log.d(getClass().getName(), "current path = " + mCurrentPath);

        switch (mBrowseMode) {
            default:
            case SAVE_FILE:
            case OPEN_FILE:
                fla = new FileListAdapter(getActivity(), mItemLayoutID, filesToLoad, mBrowseMode, mSortMode, isRoot);
                break;
            case SELECT_DIR:
                FileFilter filter = new FileFilter() {
                    @Override
                    public boolean accept(File file) {
                        return file.isDirectory();
                    }
                };
                fla = new FileListAdapter(getActivity(), mItemLayoutID, directory.listFiles(filter), mBrowseMode, mSortMode, isRoot);
                break;
        }

        rvFileList.setAdapter(fla);

        //if (browseMode == MODE_SAVE_FILE) btnSave.setEnabled(directory.canWrite());
        Parcelable state = mStates.get(mCurrentPath);
        if (state != null)
            rvFileList.getLayoutManager().onRestoreInstanceState(state);

        File currentFile = new File(mCurrentPath);
        mToolbar.getMenu().findItem(R.id.browser_menuNewFolder).setVisible(currentFile.canWrite());
    }

    /**
     * Ha a mentéskor megadott névvel már létezik fájl, megerősítést kér a felülírásról.
     * Tényleges írás NEM történik.
     *
     * @param fileName fájlnév
     */
    private void showOverwriteDialog(final String fileName) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity())
                .setIcon(android.R.drawable.ic_dialog_alert)
                .setMessage(R.string.browser_fileExists_message)
                .setTitle(R.string.browser_fileExists_title)
                .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        onDialogResultListener.onPositiveResult(fileName);
                        dismiss();
                    }
                })
                .setNegativeButton(android.R.string.no, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        dialogInterface.dismiss();
                    }
                });
        AlertDialog ad = builder.create();
        ad.show();
    }

    /**
     * Új mappa létrehozása az aktuális mappában.
     * WRITE_EXTERNAL_STORAGE szükséges!
     */
    private void showNewFolderDialog() {
        @SuppressLint("InflateParams") final View view = getActivity().getLayoutInflater().inflate(R.layout.browser_dialog_newfolder, null);
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity())
                .setTitle(R.string.browser_menu_newFolder)
                .setIcon(R.drawable.browser_open_folder_dark).setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        EditText etFolderName = (EditText) view.findViewById(R.id.browser_etNewFolder);
                        if (Utils.isFilenameValid(etFolderName.getText().toString())) {
                            File newDir = new File(mCurrentPath + "/" + etFolderName.getText());
                            if (newDir.mkdir()) {
                                loadList(new File(mCurrentPath));
                            } else showErrorDialog(Error.CANT_CREATE_FOLDER);
                        } else showErrorDialog(Error.INVALID_FOLDERNAME);
                    }
                })
                .setNegativeButton(android.R.string.no, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        dialogInterface.dismiss();
                    }
                })
                .setView(view);
        AlertDialog ad = builder.create();
        ad.show();
    }

    /**
     * Hibaüzenet megjelenítése a felhasználónak.
     *
     * @param error a hiba oka
     */
    private void showErrorDialog(Error error) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setIcon(android.R.drawable.ic_dialog_alert);
        builder.setNeutralButton(android.R.string.ok, null);

        switch (error) {
            case CANT_CREATE_FOLDER:
                builder = new AlertDialog.Builder(getActivity())
                        .setMessage(R.string.browser_error_cantCreateFolder_message)
                        .setTitle(R.string.browser_error_cantCreateFolder_title);
                break;
            case FOLDER_NOT_READABLE:
                builder = new AlertDialog.Builder(getActivity())
                        .setMessage(R.string.browser_error_folderCantBeOpened_message)
                        .setTitle(R.string.browser_error_folderCantBeOpened_title);
                break;
            case INVALID_FILENAME:
//                builder = new AlertDialog.Builder(getActivity())
//                        .setIcon(android.R.drawable.ic_dialog_alert)
//                        .setMessage(R.string.browser_error_invalidFilename_message)
//                        .setTitle(R.string.browser_error_invalidFilename_title)
//                        .setNeutralButton(android.R.string.ok, new DialogInterface.OnClickListener() {
//                            @Override
//                            public void onClick(DialogInterface dialogInterface, int i) {
//                            }
//                        });
//                Toast.makeText(getActivity(), R.string.browser_error_invalidFilename_message, Toast.LENGTH_LONG).show();
                break;
            case INVALID_FOLDERNAME:
                builder = new AlertDialog.Builder(getActivity())
                        .setMessage(R.string.browser_error_invalidFolderName_message)
                        .setTitle(R.string.browser_error_invalidFolderName_title);
                break;
            default:
                break;
        }

        builder.show();
    }

    private String checkExtension(String input) {
        final int lastDot = input.lastIndexOf('.');
        String extension;
        if (lastDot >= 0) {
            extension = input.substring(lastDot);
        } else return input + "." + mCurrentExtension;

        if (Utils.contains(mExtensionFilter, extension)) {
            return input + "." + mCurrentExtension;
        } else return input;
    }

    public BrowserDialog setOnDialogResultListener(OnDialogResultListener listener) {
        this.onDialogResultListener = listener;
        return this;
    }

    @Override
    public void onCancel(DialogInterface dialog) {
        super.onCancel(dialog);
        onDialogResultListener.onNegativeResult();
    }

    @SuppressWarnings("UnusedDeclaration")
    public BrowseMode getBrowseMode() {
        return mBrowseMode;
    }

    @SuppressWarnings("UnusedDeclaration")
    public BrowserDialog setBrowseMode(BrowseMode browseMode) {
        this.mBrowseMode = browseMode;
        return this;
    }

    @SuppressWarnings("UnusedDeclaration")
    public SortMode getSortMode() {
        return mSortMode;
    }

    @SuppressWarnings("UnusedDeclaration")
    public BrowserDialog setSortMode(SortMode sortMode) {
        this.mSortMode = sortMode;
        return this;
    }

    @SuppressWarnings("UnusedDeclaration")
    public String[] getExtensionFilter() {
        return mExtensionFilter;
    }

    public BrowserDialog setExtensionFilter(String... extensions) {
        this.mExtensionFilter = extensions;
        return this;
    }

    @SuppressWarnings("UnusedDeclaration")
    public BrowserDialog setExtensionFilter(String extensionFilter) {
        this.mExtensionFilter = extensionFilter.split(";");
        return this;
    }

    @SuppressWarnings("UnusedDeclaration")
    public String getDefaultFileName() {
        return mDefaultFileName;
    }

    @SuppressWarnings("UnusedDeclaration")
    public BrowserDialog setDefaultFileName(String defaultFileName) {
        this.mDefaultFileName = defaultFileName;
        return this;
    }

    @SuppressWarnings("UnusedDeclaration")
    public String getStartPath() {
        return mStartPath;
    }

    @SuppressWarnings("UnusedDeclaration")
    public BrowserDialog setStartPath(String startPath) {
        this.mStartPath = startPath;
        return this;
    }

    public BrowserDialog setRootPath(String rootPath) {
        this.mRootPath = rootPath;
        return this;
    }

    @SuppressWarnings("UnusedDeclaration")
    public boolean isStartRoot() {
        return mStartIsRoot;
    }

    @SuppressWarnings("UnusedDeclaration")
    public BrowserDialog setStartIsRoot(boolean startIsRoot) {
        this.mStartIsRoot = startIsRoot;
        return this;
    }

    public interface OnDialogResultListener {
        /**
         * Visszatérés a kiválasztott fájl/mappa teljes elérési útjával.
         *
         * @param path A hívó Activityben felhasználható elérési út.
         */
        void onPositiveResult(String path);

        /**
         * Nem lett kiválasztva fájl/mappa.
         * A dialógus bezárult.
         */
        void onNegativeResult();
    }
}
