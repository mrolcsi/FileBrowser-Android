package hu.mrolcsi.android.filebrowser;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.os.Parcelable;
import android.support.v4.app.DialogFragment;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.view.*;
import android.view.inputmethod.EditorInfo;
import android.widget.*;
import hu.mrolcsi.android.filebrowser.option.BrowseMode;
import hu.mrolcsi.android.filebrowser.option.Layout;
import hu.mrolcsi.android.filebrowser.option.SortMode;
import hu.mrolcsi.android.filebrowser.util.DividerItemDecoration;
import hu.mrolcsi.android.filebrowser.util.Error;
import hu.mrolcsi.android.filebrowser.util.FileSorterTask;
import hu.mrolcsi.android.filebrowser.util.Utils;
import hu.mrolcsi.android.filebrowser.util.itemclicksupport.ItemClickSupport;

import java.io.File;
import java.io.FileFilter;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Created with IntelliJ IDEA.
 * User: Matusinka Roland
 * Date: 2013.07.30.
 * Time: 11:34
 */

@SuppressWarnings("unused")
public class BrowserDialog extends DialogFragment {

    //region Public stuff
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
    private int mIconStyle = R.style.browser_DarkIcons;
    private Layout mActiveLayout = Layout.LIST;
    private int mItemLayoutID = R.layout.browser_listitem_layout;
    private boolean mOverwrite = false;
    private Map<String, Parcelable> mStates = new ConcurrentHashMap<>();
    private AsyncTask<File, Integer, List<File>> mFileSorter;

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

    private MenuItem menuSortMode;
    private MenuItem menuSwitchLayout;
    //endregion

    @Override
    public void onCreate(Bundle savedInstanceState) {
        setStyle(STYLE_NO_TITLE, this.getTheme());
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


    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);

        getActivity().getTheme().applyStyle(mIconStyle, true);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.browser_layout_dialog, container, false);
    }

    @Override
    public void onViewCreated(final View view, Bundle savedInstanceState) {

        mToolbar = (Toolbar) view.findViewById(R.id.browser_toolbar);
        setupToolbar();

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
            btnSave.setImageResource(Utils.getStyledResource(getActivity(), R.attr.browser_save, R.drawable.browser_save_dark));
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

    private void setupToolbar() {
        mToolbar.inflateMenu(R.menu.browser_menu);
        mToolbar.setTitle(R.string.browser_currentDirectory);

        mToolbar.getMenu().findItem(R.id.browser_menuNewFolder).setIcon(Utils.getStyledResource(getActivity(), R.attr.browser_open_folder, R.drawable.browser_open_folder_dark));

        menuSortMode = mToolbar.getMenu().findItem(R.id.browser_menuSort);

        setupSortMode();

        menuSwitchLayout = mToolbar.getMenu().findItem(R.id.browser_menuSwitchLayout);
        menuSwitchLayout.setIcon(Utils.getStyledResource(getActivity(), R.attr.browser_grid, R.drawable.browser_grid_dark));

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
                        menuItem.setIcon(Utils.getStyledResource(getActivity(), R.attr.browser_list, R.drawable.browser_list_dark));
                        toGridView();
                    } else if (mActiveLayout == Layout.GRID) {
                        menuItem.setTitle(R.string.browser_menu_viewAsGrid);
                        menuItem.setIcon(Utils.getStyledResource(getActivity(), R.attr.browser_grid, R.drawable.browser_grid_dark));
                        toListView();
                    }
                    return true;
                }
                return false;
            }
        });
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
                    //TODO: check
                } else {
                    onDialogResultListener.onPositiveResult(result);
                    dismiss();
                }
            } else {
                onDialogResultListener.onPositiveResult(result);
                dismiss();
            }
        } else {
            showErrorDialog(Error.INVALID_FILENAME);
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

    private void toListView() {
        mActiveLayout = Layout.LIST;
        mItemLayoutID = R.layout.browser_listitem_layout;
        rvFileList.addItemDecoration(mListItemDecor);
        rvFileList.setLayoutManager(mLinearLayout);
        loadList(new File(mCurrentPath));

        menuSwitchLayout.setTitle(R.string.browser_menu_viewAsGrid);
        menuSwitchLayout.setIcon(Utils.getStyledResource(getActivity(), R.attr.browser_grid, R.drawable.browser_grid_dark));
    }

    private void toGridView() {
        mActiveLayout = Layout.GRID;
        rvFileList.setLayoutManager(mGridLayout);
        rvFileList.removeItemDecoration(mListItemDecor);
        mItemLayoutID = R.layout.browser_griditem_layout;
        loadList(new File(mCurrentPath));

        menuSwitchLayout.setTitle(R.string.browser_menu_viewAsList);
        menuSwitchLayout.setIcon(Utils.getStyledResource(getActivity(), R.attr.browser_list, R.drawable.browser_list_dark));
    }

    private void showSortDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity())
                .setTitle(R.string.browser_menu_sortBy)
                .setIcon(Utils.getStyledResource(getActivity(), R.attr.browser_alphabetical_sorting, R.drawable.browser_alphabetical_sorting_dark))
                .setItems(R.array.browser_sortOptions, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        mSortMode = SORT_HASHES[i];

                        setupSortMode();

                        loadList(new File(mCurrentPath));
                    }
                });
        AlertDialog ad = builder.create();
        ad.show();
    }

    private void setupSortMode() {
        final String[] sortOptions = getResources().getStringArray(R.array.browser_sortOptions);

        switch (mSortMode) {
            case BY_NAME_ASC:
                menuSortMode.setTitle(sortOptions[0]);
                menuSortMode.setIcon(Utils.getStyledResource(getActivity(), R.attr.browser_alphabetical_sorting, R.drawable.browser_alphabetical_sorting_dark));
                break;
            case BY_NAME_DESC:
                menuSortMode.setTitle(sortOptions[1]);
                menuSortMode.setIcon(Utils.getStyledResource(getActivity(), R.attr.browser_alphabetical_sorting_2, R.drawable.browser_alphabetical_sorting_2_dark));
                break;
            case BY_EXTENSION_ASC:
                menuSortMode.setTitle(sortOptions[2]);
                menuSortMode.setIcon(Utils.getStyledResource(getActivity(), R.attr.browser_alphabetical_sorting, R.drawable.browser_alphabetical_sorting_dark));
                break;
            case BY_EXTENSION_DESC:
                menuSortMode.setTitle(sortOptions[3]);
                menuSortMode.setIcon(Utils.getStyledResource(getActivity(), R.attr.browser_alphabetical_sorting_2, R.drawable.browser_alphabetical_sorting_2_dark));
                break;
            case BY_DATE_ASC:
                menuSortMode.setTitle(sortOptions[4]);
                menuSortMode.setIcon(Utils.getStyledResource(getActivity(), R.attr.browser_numerical_sorting, R.drawable.browser_numerical_sorting_dark));
                break;
            case BY_DATE_DESC:
                menuSortMode.setTitle(sortOptions[5]);
                menuSortMode.setIcon(Utils.getStyledResource(getActivity(), R.attr.browser_reversed_numerical_sorting, R.drawable.browser_reversed_numerical_sorting_dark));
                break;
            case BY_SIZE_ASC:
                menuSortMode.setTitle(sortOptions[6]);
                menuSortMode.setIcon(Utils.getStyledResource(getActivity(), R.attr.browser_numerical_sorting, R.drawable.browser_numerical_sorting_dark));
                break;
            case BY_SIZE_DESC:
                menuSortMode.setTitle(sortOptions[7]);
                menuSortMode.setIcon(Utils.getStyledResource(getActivity(), R.attr.browser_reversed_numerical_sorting, R.drawable.browser_reversed_numerical_sorting_dark));
                break;
        }
    }

    private void setListListeners() {

        ItemClickSupport.OnItemClickListener onItemClickListener = new ItemClickSupport.OnItemClickListener() {
            @Override
            public void onItemClick(RecyclerView parent, View view, int position, long id) {
                FileHolder holder = (FileHolder) view.getTag();
                if (holder.file.getAbsolutePath().equals(File.separator + getString(R.string.browser_upFolder))) {
                    loadList(new File(mCurrentPath).getParentFile());
                } else if (mBrowseMode == BrowseMode.SELECT_DIR && holder.file.getAbsolutePath().equals(File.separator + getString(R.string.browser_titleSelectDir))) {
                    onDialogResultListener.onPositiveResult(holder.file.getParent());
                    dismiss();
                } else {
                    if (holder.file.isDirectory()) loadList(holder.file);
                    if (holder.file.isFile()) {
                        if (mBrowseMode == BrowseMode.SAVE_FILE) {
                            etFilename.setText(holder.file.getName());
                        } else {
                            onDialogResultListener.onPositiveResult(holder.file.getAbsolutePath());
                            dismiss();
                        }
                    }
                }
            }
        };

        ItemClickSupport.OnItemLongClickListener onItemLongClickListener = new ItemClickSupport.OnItemLongClickListener() {
            @Override
            public boolean onItemLongClick(RecyclerView parent, View view, int position, long id) {
                FileHolder holder = (FileHolder) view.getTag();
                if (mBrowseMode == BrowseMode.OPEN_FILE && holder.file.isFile() || mBrowseMode == BrowseMode.SELECT_DIR && holder.file.isDirectory()) {
                    onDialogResultListener.onPositiveResult(holder.file.getAbsolutePath());
                    dismiss();
                    return true;
                }
                if (mBrowseMode == BrowseMode.SAVE_FILE && holder.file.isFile()) {
                    showOverwriteDialog(holder.file.getAbsolutePath());
                    return true;
                }
                return false;
            }
        };

        mItemClickSupport.setOnItemClickListener(onItemClickListener);
        mItemClickSupport.setOnItemLongClickListener(onItemLongClickListener);
    }

    private void loadList(final File directory) {
        if (!directory.canRead()) {
            showErrorDialog(Error.FOLDER_NOT_READABLE);
            return;
        }

        mStates.put(mCurrentPath, rvFileList.getLayoutManager().onSaveInstanceState());

        if (mFileSorter != null) {
            mFileSorter.cancel(true);
            mFileSorter = null;
        }

        File[] filesToLoad;

        if (mBrowseMode == BrowseMode.OPEN_FILE || mBrowseMode == BrowseMode.SAVE_FILE) {
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
        } else filesToLoad = directory.listFiles(new FileFilter() {
            @Override
            public boolean accept(File file) {
                return file.isDirectory() && file.canRead();
            }
        });

        mCurrentPath = directory.getAbsolutePath();

        mFileSorter = new FileSorterTask(mSortMode) {

            long startTime;
            private ProgressDialog pd;

            @Override
            protected void onPreExecute() {
                super.onPreExecute();

                pd = new ProgressDialog(getActivity());
                pd.setMessage(getString(R.string.browser_loadingFileList));
                //pd.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
                //pd.setIndeterminate(false);
                pd.setCanceledOnTouchOutside(false);
                pd.setOnCancelListener(new DialogInterface.OnCancelListener() {
                    @Override
                    public void onCancel(DialogInterface dialogInterface) {
                        mFileSorter.cancel(true);
                        mFileSorter = null;
                    }
                });

                startTime = System.currentTimeMillis();
            }

            @Override
            protected void onProgressUpdate(Integer... values) {
                super.onProgressUpdate(values);
                pd.setProgress(values[0]);
                pd.setMax(values[1]);

                //if (System.currentTimeMillis() - startTime > 3000)
                pd.show();
            }

            @Override
            protected void onPostExecute(List<File> files) {
                super.onPostExecute(files);

                pd.dismiss();

                mToolbar.setSubtitle(mCurrentPath);

                boolean isRoot = mStartIsRoot ? mCurrentPath.equals(mStartPath) || mCurrentPath.equals(mRootPath) : mCurrentPath.equals(mRootPath);

                rvFileList.setAdapter(new FileListAdapter(getActivity(), mItemLayoutID, files, mBrowseMode, mSortMode, isRoot));

                Parcelable state = mStates.get(mCurrentPath);
                if (state != null)
                    rvFileList.getLayoutManager().onRestoreInstanceState(state);

                File currentFile = new File(mCurrentPath);
                mToolbar.getMenu().findItem(R.id.browser_menuNewFolder).setVisible(currentFile.canWrite());

            }
        }.execute(filesToLoad);

    }

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

    private void showNewFolderDialog() {
        @SuppressLint("InflateParams") final View view = getActivity().getLayoutInflater().inflate(R.layout.browser_dialog_newfolder, null);
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity())
                .setTitle(R.string.browser_menu_newFolder)
                .setIcon(Utils.getStyledResource(getActivity(), R.attr.browser_open_folder, R.drawable.browser_open_folder_dark))
                .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
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


    public BrowseMode getBrowseMode() {
        return mBrowseMode;
    }


    public BrowserDialog setBrowseMode(BrowseMode browseMode) {
        this.mBrowseMode = browseMode;
        return this;
    }


    public SortMode getSortMode() {
        return mSortMode;
    }


    public BrowserDialog setSortMode(SortMode sortMode) {
        this.mSortMode = sortMode;
        return this;
    }


    public String[] getExtensionFilter() {
        return mExtensionFilter;
    }

    public BrowserDialog setExtensionFilter(String... extensions) {
        this.mExtensionFilter = extensions;
        return this;
    }

    public BrowserDialog setExtensionFilter(String extensionFilter) {
        this.mExtensionFilter = extensionFilter.split(";");
        return this;
    }

    public String getDefaultFileName() {
        return mDefaultFileName;
    }


    public BrowserDialog setDefaultFileName(String defaultFileName) {
        this.mDefaultFileName = defaultFileName;
        return this;
    }


    public String getStartPath() {
        return mStartPath;
    }


    public BrowserDialog setStartPath(String startPath) {
        this.mStartPath = startPath;
        return this;
    }

    public BrowserDialog setRootPath(String rootPath) {
        this.mRootPath = rootPath;
        return this;
    }


    public boolean isStartRoot() {
        return mStartIsRoot;
    }


    public BrowserDialog setStartIsRoot(boolean startIsRoot) {
        this.mStartIsRoot = startIsRoot;
        return this;
    }

    public BrowserDialog setTheme(int styleResourceId) {
        this.mIconStyle = styleResourceId;
        return this;
    }

    public BrowserDialog setLayout(Layout layout) {
        this.mActiveLayout = layout;
        return this;
    }

    public interface OnDialogResultListener {

        void onPositiveResult(String path);

        void onNegativeResult();
    }
}
