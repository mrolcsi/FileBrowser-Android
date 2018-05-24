package hu.mrolcsi.android.filebrowser;

import android.annotation.SuppressLint;
import android.content.DialogInterface;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.os.Parcelable;
import android.support.v4.app.DialogFragment;
import android.support.v7.app.AlertDialog;
import android.support.v7.view.menu.MenuBuilder;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RelativeLayout;
import android.widget.Spinner;
import android.widget.TextView;
import hu.mrolcsi.android.filebrowser.option.BrowseMode;
import hu.mrolcsi.android.filebrowser.option.Layout;
import hu.mrolcsi.android.filebrowser.option.SortMode;
import hu.mrolcsi.android.filebrowser.util.DividerItemDecoration;
import hu.mrolcsi.android.filebrowser.util.Error;
import hu.mrolcsi.android.filebrowser.util.FileSorterTask;
import hu.mrolcsi.android.filebrowser.util.FileUtils;
import hu.mrolcsi.android.filebrowser.util.Utils;
import hu.mrolcsi.android.filebrowser.util.itemclicksupport.ItemClickSupport;
import java.io.File;
import java.lang.reflect.Method;
import java.util.List;
import java.util.Locale;
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
  public static final String TAG = "BrowserDialog";

  /**
   * Browsing mode: <ul> <li>Open File: {@link hu.mrolcsi.android.filebrowser.option.BrowseMode#OPEN_FILE
   * OPEN_FILE}</li> <li>Select Directory: {@link hu.mrolcsi.android.filebrowser.option.BrowseMode#SELECT_DIR
   * SELECT_DIR}</li> <li>Save File: {@link hu.mrolcsi.android.filebrowser.option.BrowseMode#SAVE_FILE
   * SAVE_FILE}</li> </ul> Default: open file
   *
   * @see hu.mrolcsi.android.filebrowser.option.BrowseMode
   */
  private static final String OPTION_BROWSE_MODE;
  /**
   * String:  Absolute path to starting directory (Default: root of EXTERNAL_STORAGE (SD-Card) or
   * "/")
   */
  private static final String OPTION_START_PATH;
  /**
   * String:  File extensions to show separated with semicolons (;) (Default: empty (*.*))
   */
  private static final String OPTION_EXTENSION_FILTER;
  /**
   * Return value: Absolute path to selected file/directory
   */
  private static final String RESULT;
  /**
   * Sort Mode: (Directories always have priority before files) <ul> <li>By filename (ascending): {@link
   * hu.mrolcsi.android.filebrowser.option.SortMode#BY_NAME_ASC BY_NAME_ASC}</li> <li>By filename (descending): {@link
   * hu.mrolcsi.android.filebrowser.option.SortMode#BY_NAME_DESC BY_NAME_DESC}</li> <li>By extension (ascending): {@link
   * hu.mrolcsi.android.filebrowser.option.SortMode#BY_EXTENSION_ASC BY_EXTENSION_ASC}</li> <li>By extension
   * (descending): {@link hu.mrolcsi.android.filebrowser.option.SortMode#BY_EXTENSION_DESC BY_EXTENSION_DESC}</li>
   * <li>By modification date (ascending): {@link hu.mrolcsi.android.filebrowser.option.SortMode#BY_DATE_ASC
   * BY_DATE_ASC}</li> <li>By modification date (descending): {@link hu.mrolcsi.android.filebrowser.option.SortMode#BY_DATE_DESC
   * BY_DATE_DESC}</li> <li>By size (ascending): {@link hu.mrolcsi.android.filebrowser.option.SortMode#BY_SIZE_ASC
   * BY_SIZE_ASC}</li> <li>By size (descending): {@link hu.mrolcsi.android.filebrowser.option.SortMode#BY_SIZE_DESC
   * BY_SIZE_DESC}</li> </ul> Default: by filename (ascending)
   *
   * @see hu.mrolcsi.android.filebrowser.option.SortMode
   */
  private static final String OPTION_SORT_MODE;
  /**
   * String:  Default filename; only used when saving.
   *
   * @see hu.mrolcsi.android.filebrowser.option.BrowseMode
   */
  private static final String OPTION_DEFAULT_FILENAME;
  /**
   * Boolean: Should the specified start path be considered as Root?
   *
   * @see #OPTION_START_PATH
   */
  private static final String OPTION_START_IS_ROOT;
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
  private static final String OPTION_LAYOUT;
  /**
   * An optional title for the browser dialog.
   */
  private static final String OPTION_DIALOG_TITLE;
  /**
   * Boolean: should hidden files be shown when browsing?
   *
   * Hidden files are files starting with '.' (dot) or files which have the hidden file system
   * attribute set.
   */
  private static final String OPTION_SHOW_HIDDEN_FILES;

  /**
   * Boolean: can the user change directory?
   */
  private static final String OPTION_LOCKED_FOLDER;

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
    OPTION_DIALOG_TITLE = "dialogTitle";
    OPTION_SHOW_HIDDEN_FILES = "showHiddenFiles";
    OPTION_LOCKED_FOLDER = "lockedFolder";
  }

  //region Privates

  protected BrowseMode mBrowseMode = BrowseMode.OPEN_FILE;
  protected final Map<String, Parcelable> mStates = new ConcurrentHashMap<>();
  protected SortMode mSortMode = SortMode.BY_NAME_ASC;
  protected String[] mExtensionFilter;
  private String mStartPath = Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED) ?
      Environment.getExternalStorageDirectory().getAbsolutePath() : "/";
  private String mRootPath = File.listRoots()[0].getAbsolutePath();
  private boolean mStartIsRoot = true;
  protected int mItemLayoutID = R.layout.browser_listitem_layout;
  protected AsyncTask mFileSorter;
  protected RecyclerView rvFileList;
  protected EditText etFilename;
  protected ItemClickSupport mItemClickSupport;
  protected Toolbar mToolbar;
  private String mDefaultFileName;
  private String mDialogTitle;
  private String mCurrentExtension;
  private String mCurrentPath = mStartPath;
  private Layout mActiveLayout = Layout.LIST;
  protected boolean mShowHiddenFiles = false;
  protected boolean mLocked = false;
  private Button btnSave;
  private LinearLayoutManager mLinearLayout;
  private GridLayoutManager mGridLayout;
  private OnDialogResultListener onDialogResultListener = new OnDialogResultListener() {
    @Override
    public void onPositiveResult(String path) {
    }

    @Override
    public void onNegativeResult() {
    }
  };

  private MenuItem menuNewFolder;
  private MenuItem menuSortMode;
  private MenuItem menuSwitchLayout;
  protected MenuItem menuShowHiddenFiles;

  //endregion

  @Override
  public void onCreate(Bundle savedInstanceState) {
    setStyle(STYLE_NO_TITLE, getTheme());
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
      mDialogTitle = savedInstanceState.getString(OPTION_DIALOG_TITLE);
      mShowHiddenFiles = savedInstanceState.getBoolean(OPTION_SHOW_HIDDEN_FILES, false);
      mLocked = savedInstanceState.getBoolean(OPTION_LOCKED_FOLDER, false);
    } else {
      mCurrentPath = mStartPath;
    }
    super.onCreate(savedInstanceState);
  }

  @Override
  public View onCreateView(LayoutInflater inflater, ViewGroup container,
      Bundle savedInstanceState) {
    return inflater.inflate(R.layout.browser_layout_dialog, container, false);
  }

  @Override
  public void onViewCreated(final View view, Bundle savedInstanceState) {

    mToolbar = view.findViewById(R.id.browser_toolbar);
    setupToolbar();

    rvFileList = view.findViewById(R.id.browser_recyclerView);
    DividerItemDecoration listItemDecor = new DividerItemDecoration(getContext(), DividerItemDecoration.VERTICAL_LIST);
    mLinearLayout = new LinearLayoutManager(getContext(), LinearLayoutManager.VERTICAL, false);
    mGridLayout = new GridLayoutManager(getContext(),
        getResources().getInteger(R.integer.browser_columnCount), LinearLayoutManager.VERTICAL,
        false);
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
      if (btnSave == null) {
        btnSave = view.findViewById(R.id.browser_imageButtonSave);
        //btnSave.setImageDrawable(Utils.tintDrawable(getContext(), R.drawable.browser_save));
      }

      if (etFilename == null) {
        etFilename = view.findViewById(R.id.browser_editTextFileName);
        etFilename.setOnEditorActionListener((textView, i, keyEvent) -> {
          if (i == EditorInfo.IME_ACTION_DONE) {
            saveFile(false);
            return true;
          }
          return false;
        });
      }

      RelativeLayout rlSave = view.findViewById(R.id.browser_rlSave);
      rlSave.setVisibility(View.VISIBLE);

      final Spinner spnExtension = view.findViewById(R.id.browser_spnExtension);
      if (mExtensionFilter != null) {
        final ArrayAdapter<String> extensionAdapter = new ArrayAdapter<>(getContext(),
            android.R.layout.simple_spinner_item, mExtensionFilter);
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
      } else {
        spnExtension.setVisibility(View.GONE);
      }

      if (mDefaultFileName != null) {
        etFilename.setText(mDefaultFileName);
      }

      btnSave.setOnClickListener(v -> saveFile(false));
    }
  }

  @Override
  public void onSaveInstanceState(Bundle outState) {
    super.onSaveInstanceState(outState);
    outState.putString("currentPath", mCurrentPath);
    outState.putSerializable(OPTION_BROWSE_MODE, mBrowseMode);
    outState.putSerializable(OPTION_SORT_MODE, mSortMode);
    outState.putStringArray(OPTION_EXTENSION_FILTER, mExtensionFilter);
    outState.putString(OPTION_START_PATH, mStartPath);
    outState.putBoolean(OPTION_START_IS_ROOT, mStartIsRoot);
    outState.putSerializable(OPTION_LAYOUT, mActiveLayout);
    outState.putInt("itemLayoutID", mItemLayoutID);
    outState.putString(OPTION_DEFAULT_FILENAME, mDefaultFileName);
    outState.putString(OPTION_DIALOG_TITLE, mDialogTitle);
    outState.putBoolean(OPTION_SHOW_HIDDEN_FILES, mShowHiddenFiles);
    outState.putBoolean(OPTION_LOCKED_FOLDER, mLocked);
  }

  private void setupToolbar() {
    mToolbar.inflateMenu(R.menu.browser_menu);
    if (mDialogTitle != null) {
      mToolbar.setTitle(mDialogTitle);
    } else {
      switch (mBrowseMode) {
        case OPEN_FILE:
          mToolbar.setTitle(R.string.browser_titleOpenFile);
          break;
        case SELECT_DIR:
          mToolbar.setTitle(R.string.browser_titleSelectDir);
          break;
        case SAVE_FILE:
          mToolbar.setTitle(R.string.browser_titleSaveFile);
          break;
      }
    }

    mToolbar.setOverflowIcon(Utils.getTintedDrawable(getContext(), mToolbar.getOverflowIcon()));

    final Menu menu = mToolbar.getMenu();

    if (menu.getClass().equals(MenuBuilder.class)) {
      try {
        @SuppressLint("PrivateApi")
        Method m = menu.getClass().getDeclaredMethod("setOptionalIconsVisible", Boolean.TYPE);
        m.setAccessible(true);
        m.invoke(menu, true);
      } catch (NoSuchMethodException e) {
        Log.e(TAG, "onMenuOpened", e);
      } catch (Exception e) {
        throw new RuntimeException(e);
      }
    }

    menuNewFolder = menu.findItem(R.id.browser_menuNewFolder);
    menuNewFolder.setIcon(Utils.getTintedDrawable(getContext(), R.drawable.browser_new_folder));

    menuSortMode = menu.findItem(R.id.browser_menuSort);

    setupSortMode();

    menuSwitchLayout = menu.findItem(R.id.browser_menuSwitchLayout);
    if (mActiveLayout == Layout.LIST) {
      menuSwitchLayout.setTitle(R.string.browser_menu_viewAsGrid);
      menuSwitchLayout.setIcon(Utils.getTintedDrawable(getContext(), R.drawable.browser_grid));
    } else {
      menuSwitchLayout.setTitle(R.string.browser_menu_viewAsList);
      menuSwitchLayout.setIcon(Utils.getTintedDrawable(getContext(), R.drawable.browser_list));
    }

    menuShowHiddenFiles = menu.findItem(R.id.browser_menuShowHidden);
    if (mShowHiddenFiles) {
      menuShowHiddenFiles.setTitle(R.string.browser_menu_dontShowHiddenFiles);
      menuShowHiddenFiles.setIcon(Utils.getTintedDrawable(getContext(), R.drawable.browser_hide));
    } else {
      menuShowHiddenFiles.setTitle(R.string.browser_menu_showHiddenFiles);
      menuShowHiddenFiles.setIcon(Utils.getTintedDrawable(getContext(), R.drawable.browser_show));
    }

    mToolbar.setOnMenuItemClickListener(menuItem -> {
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
          menuItem.setIcon(Utils.getTintedDrawable(getContext(), R.drawable.browser_list));
          toGridView();
        } else if (mActiveLayout == Layout.GRID) {
          menuItem.setTitle(R.string.browser_menu_viewAsGrid);
          menuItem.setIcon(Utils.getTintedDrawable(getContext(), R.drawable.browser_grid));
          toListView();
        }
        return true;
      } else if (id == R.id.browser_menuShowHidden) {
        if (mShowHiddenFiles) {
          dontShowHiddenFiles();
        } else {
          showHiddenFiles();
        }
        return true;
      }
      return false;
    });
  }

  protected void saveFile(boolean overwrite) {
    final String filename = checkExtension(etFilename.getText().toString());
    String result = mCurrentPath + "/" + filename;

    if (!result.isEmpty() && FileUtils.isFilenameValid(result)) {
      File f = new File(result);
      if (f.exists()) {
        if (!overwrite) {
          showOverwriteDialog(filename);
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

  protected void toListView() {
    mActiveLayout = Layout.LIST;
    mItemLayoutID = R.layout.browser_listitem_layout;
    //rvFileList.addItemDecoration(mListItemDecor);
    rvFileList.setLayoutManager(mLinearLayout);
    loadList(new File(mCurrentPath));

    menuSwitchLayout.setTitle(R.string.browser_menu_viewAsGrid);
    menuSwitchLayout.setIcon(Utils.getTintedDrawable(getContext(), R.drawable.browser_grid));
  }

  protected void toGridView() {
    mActiveLayout = Layout.GRID;
    rvFileList.setLayoutManager(mGridLayout);
    //rvFileList.removeItemDecoration(mListItemDecor);
    mItemLayoutID = R.layout.browser_griditem_layout;
    loadList(new File(mCurrentPath));

    menuSwitchLayout.setTitle(R.string.browser_menu_viewAsList);
    menuSwitchLayout.setIcon(Utils.getTintedDrawable(getContext(), R.drawable.browser_list));
  }

  protected void showHiddenFiles() {
    mShowHiddenFiles = true;
    loadList(new File(mCurrentPath));

    menuShowHiddenFiles.setTitle(R.string.browser_menu_dontShowHiddenFiles);
    menuShowHiddenFiles.setIcon(Utils.getTintedDrawable(getContext(), R.drawable.browser_hide));
  }

  protected void dontShowHiddenFiles() {
    mShowHiddenFiles = false;
    loadList(new File(mCurrentPath));

    menuShowHiddenFiles.setTitle(R.string.browser_menu_showHiddenFiles);
    menuShowHiddenFiles.setIcon(Utils.getTintedDrawable(getContext(), R.drawable.browser_show));
  }

  protected void showSortDialog() {
    AlertDialog.Builder builder = new AlertDialog.Builder(getContext())
        .setTitle(R.string.browser_menu_sortBy)
        .setIcon(Utils.getTintedDrawable(getContext(), R.drawable.browser_alphabetical_sorting_asc))
        .setItems(R.array.browser_sortOptions, (dialogInterface, i) -> {
          mSortMode = SORT_HASHES[i];

          setupSortMode();

          loadList(new File(mCurrentPath));
        });
    AlertDialog ad = builder.create();
    ad.show();
  }

  protected void setupSortMode() {
    final String[] sortOptions = getResources().getStringArray(R.array.browser_sortOptions);

    switch (mSortMode) {
      case BY_NAME_ASC:
        menuSortMode.setTitle(sortOptions[0]);
        menuSortMode.setIcon(Utils.getTintedDrawable(getContext(), R.drawable.browser_alphabetical_sorting_asc));
        break;
      case BY_NAME_DESC:
        menuSortMode.setTitle(sortOptions[1]);
        menuSortMode.setIcon(Utils.getTintedDrawable(getContext(), R.drawable.browser_alphabetical_sorting_desc));
        break;
      case BY_EXTENSION_ASC:
        menuSortMode.setTitle(sortOptions[2]);
        menuSortMode.setIcon(Utils.getTintedDrawable(getContext(), R.drawable.browser_alphabetical_sorting_asc));
        break;
      case BY_EXTENSION_DESC:
        menuSortMode.setTitle(sortOptions[3]);
        menuSortMode.setIcon(Utils.getTintedDrawable(getContext(), R.drawable.browser_alphabetical_sorting_desc));
        break;
      case BY_DATE_ASC:
        menuSortMode.setTitle(sortOptions[4]);
        menuSortMode.setIcon(Utils.getTintedDrawable(getContext(), R.drawable.browser_numerical_sorting_asc));
        break;
      case BY_DATE_DESC:
        menuSortMode.setTitle(sortOptions[5]);
        menuSortMode.setIcon(Utils.getTintedDrawable(getContext(), R.drawable.browser_numerical_sorting_desc));
        break;
      case BY_SIZE_ASC:
        menuSortMode.setTitle(sortOptions[6]);
        menuSortMode.setIcon(Utils.getTintedDrawable(getContext(), R.drawable.browser_numerical_sorting_asc));
        break;
      case BY_SIZE_DESC:
        menuSortMode.setTitle(sortOptions[7]);
        menuSortMode.setIcon(Utils.getTintedDrawable(getContext(), R.drawable.browser_numerical_sorting_desc));
        break;
    }
  }

  protected void setListListeners() {

    ItemClickSupport.OnItemClickListener onItemClickListener = (parent, view, position, id) -> {
      FileListAdapter.FileHolder holder = (FileListAdapter.FileHolder) view.getTag();
      if (holder.file.getAbsolutePath().equals(File.separator + getString(R.string.browser_upFolder))) {
        loadList(new File(mCurrentPath).getParentFile());
      } else if (mBrowseMode == BrowseMode.SELECT_DIR && holder.file.getAbsolutePath()
          .equals(File.separator + getString(R.string.browser_titleSelectDir))) {
        onDialogResultListener.onPositiveResult(mCurrentPath);
        dismiss();
      } else {
        if (holder.file.isDirectory() && !mLocked) {
          loadList(holder.file);
        }
        if (holder.file.isFile()) {
          if (mBrowseMode == BrowseMode.SAVE_FILE) {
            etFilename.setText(holder.file.getName());
          } else {
            onDialogResultListener.onPositiveResult(holder.file.getAbsolutePath());
            dismiss();
          }
        }
      }
    };

    ItemClickSupport.OnItemLongClickListener onItemLongClickListener = (parent, view, position, id) -> {
      FileListAdapter.FileHolder holder = (FileListAdapter.FileHolder) view.getTag();
      if (mBrowseMode == BrowseMode.OPEN_FILE && holder.file.isFile()
          || mBrowseMode == BrowseMode.SELECT_DIR && holder.file.isDirectory()) {
        onDialogResultListener.onPositiveResult(holder.file.getAbsolutePath());
        dismiss();
        return true;
      }
      if (mBrowseMode == BrowseMode.SAVE_FILE && holder.file.isFile()) {
        showOverwriteDialog(holder.file.getAbsolutePath());
        return true;
      }
      return false;
    };

    mItemClickSupport.setOnItemClickListener(onItemClickListener);
    mItemClickSupport.setOnItemLongClickListener(onItemLongClickListener);
  }

  protected void loadList(final File directory) {
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
      if (mExtensionFilter != null) {
        filesToLoad = directory.listFiles(file -> {
          if (file.isFile()) {
            if (file.isHidden() && !mShowHiddenFiles) {
              return false;
            }

            String ext = FileUtils.getExtension(file.getName());
            int i = 0;
            int n = mExtensionFilter.length;
            while (i < n && !mExtensionFilter[i].toLowerCase(Locale.getDefault())
                .equals(ext)) {
              i++;
            }
            return file.canRead() && i < n;
          } else {
            return !(file.isHidden() && !mShowHiddenFiles) && file.canRead();
          }
        });
      } else {
        filesToLoad = directory.listFiles(file -> !(file.isHidden() && !mShowHiddenFiles) && file.canRead());
      }
    } else {
      filesToLoad = directory.listFiles(file ->
          file.isDirectory() && !(file.isHidden() && !mShowHiddenFiles) && file.canRead());
    }

    mCurrentPath = directory.getAbsolutePath();

    mFileSorter = new FileSorterTask(mSortMode) {

      long startTime;
      private AlertDialog pd;

      @Override
      protected void onPreExecute() {
        super.onPreExecute();

        pd = Utils.showProgressDialog(getContext(), getString(R.string.browser_loadingFileList));
        pd.setCanceledOnTouchOutside(false);
        pd.setOnCancelListener(dialogInterface -> {
          mFileSorter.cancel(true);
          mFileSorter = null;
        });

        startTime = System.currentTimeMillis();
      }

      @Override
      protected void onProgressUpdate(Integer... values) {
        super.onProgressUpdate(values);
//                pd.setProgress(values[0]);
//                pd.setMax(values[1]);

        //if (System.currentTimeMillis() - startTime > 3000)
        pd.show();
      }

      @Override
      protected void onPostExecute(List<File> files) {
        super.onPostExecute(files);

        pd.dismiss();

        mToolbar.setSubtitle(mCurrentPath);

        boolean isRoot = mStartIsRoot ?
            mCurrentPath.equals(mStartPath) || mCurrentPath.equals(mRootPath)
                : mCurrentPath.equals(mRootPath);

        rvFileList.setAdapter(new FileListAdapter(getContext(), mItemLayoutID, files, mBrowseMode, mSortMode, isRoot));

        Parcelable state = mStates.get(mCurrentPath);
        if (state != null) {
          rvFileList.getLayoutManager().onRestoreInstanceState(state);
        }

        File currentFile = new File(mCurrentPath);
        menuNewFolder.setVisible(currentFile.canWrite());

      }
    }.execute(filesToLoad);

  }

  protected void showOverwriteDialog(final String fileName) {
    AlertDialog.Builder builder = new AlertDialog.Builder(getContext())
        .setIcon(Utils.getTintedDrawable(getContext(), android.R.drawable.ic_dialog_alert))
        .setMessage(R.string.browser_fileExists_message)
        .setTitle(R.string.browser_fileExists_title)
        .setPositiveButton(android.R.string.yes, (dialogInterface, i) -> saveFile(true))
        .setNegativeButton(android.R.string.no, null);
    AlertDialog ad = builder.create();
    ad.show();
  }

  private void showNewFolderDialog() {
    @SuppressLint("InflateParams") final View view = LayoutInflater.from(getContext())
        .inflate(R.layout.browser_dialog_newfolder, null);

    final TypedValue tv = new TypedValue();
    getContext().getTheme().resolveAttribute(R.attr.alertDialogTheme, tv, true);

    AlertDialog.Builder builder = new AlertDialog.Builder(getContext(), tv.resourceId)
        .setTitle(R.string.browser_menu_newFolder)
        .setIcon(Utils.getTintedDrawable(getContext(), R.drawable.browser_new_folder))
        .setPositiveButton(android.R.string.yes, (dialogInterface, i) -> {
          EditText etFolderName = view.findViewById(R.id.browser_etNewFolder);
          createFolder(etFolderName.getText().toString());
        })
        .setNegativeButton(android.R.string.no, (dialogInterface, i) -> dialogInterface.dismiss())
        .setView(view);
    AlertDialog ad = builder.create();
    ad.show();
  }

  protected void createFolder(String folderName) {
    if (FileUtils.isFilenameValid(folderName)) {
      File newDir = new File(mCurrentPath + "/" + folderName);
      if (newDir.mkdir()) {
        if (mLocked) {
          loadList(new File(mCurrentPath));
        } else {
          loadList(newDir);
        }
      } else {
        showErrorDialog(Error.CANT_CREATE_FOLDER);
      }
    } else {
      showErrorDialog(Error.INVALID_FOLDERNAME);
    }
  }

  protected void showErrorDialog(Error error, String extraMessage) {
    AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
    builder.setIcon(Utils.getTintedDrawable(getContext(), android.R.drawable.ic_dialog_alert));
    builder.setPositiveButton(android.R.string.ok, null);

    @SuppressLint("InflateParams") final View view = LayoutInflater.from(getContext())
        .inflate(R.layout.browser_error_extra, null);
    TextView tvExtra = view.findViewById(R.id.tvExtra);
    builder.setView(view);

    switch (error) {
      case CANT_CREATE_FOLDER:
        builder.setTitle(R.string.browser_error_cantCreateFolder_title);
        builder.setMessage(R.string.browser_error_cantCreateFolder_message);
        break;
      case FOLDER_NOT_READABLE:
        builder.setTitle(R.string.browser_error_folderCantBeOpened_title);
        builder.setMessage(R.string.browser_error_folderCantBeOpened_message);
        break;
      case INVALID_FILENAME:
        builder.setTitle(R.string.browser_error_invalidFilename_title);
        builder.setMessage(R.string.browser_error_invalidFilename_message);
        break;
      case INVALID_FOLDERNAME:
        builder.setTitle(R.string.browser_error_invalidFolderName_title);
        builder.setMessage(R.string.browser_error_invalidFolderName_message);
        break;
      case CANT_CREATE_FILE:
        builder.setTitle(R.string.browser_error_cantCreateFile_title);
        builder.setMessage(R.string.browser_error_cantCreateFile_message);
        break;
      case USB_ERROR:
        builder.setTitle(R.string.browser_error_usbError_title);
        builder.setMessage(R.string.browser_error_usbError_message);
        break;
      default:
        break;
    }

    if (extraMessage != null) {
      tvExtra.setText(extraMessage);
    }

    builder.show();
  }

  protected void showErrorDialog(Error error) {
    showErrorDialog(error, null);
  }

  protected String checkExtension(String input) {
    final int lastDot = input.lastIndexOf('.');
    String extension;
    if (lastDot >= 0) {
      extension = input.substring(lastDot);
    } else {
      return input + "." + mCurrentExtension;
    }

    if (mExtensionFilter != null && Utils.contains(mExtensionFilter, extension)) {
      return input + "." + mCurrentExtension;
    } else {
      return input;
    }
  }

  public BrowserDialog setOnDialogResultListener(OnDialogResultListener listener) {
    onDialogResultListener = listener;
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
    mBrowseMode = browseMode;
    return this;
  }

  public SortMode getSortMode() {
    return mSortMode;
  }

  public BrowserDialog setSortMode(SortMode sortMode) {
    mSortMode = sortMode;
    return this;
  }

  public String[] getExtensionFilter() {
    return mExtensionFilter;
  }

  public BrowserDialog setExtensionFilter(String extensionFilter) {
    mExtensionFilter = extensionFilter.split(";");
    return this;
  }

  public BrowserDialog setExtensionFilter(String... extensions) {
    mExtensionFilter = extensions;
    return this;
  }

  public String getDefaultFileName() {
    return mDefaultFileName;
  }

  public BrowserDialog setDefaultFileName(String defaultFileName) {
    mDefaultFileName = defaultFileName;
    return this;
  }

  public BrowserDialog setDialogTitle(String title) {
    mDialogTitle = title;
    return this;
  }

  public String getDialogTitle() {
    return mDialogTitle;
  }

  public String getStartPath() {
    return mStartPath;
  }

  public BrowserDialog setStartPath(String startPath) {
    mStartPath = startPath;
    return this;
  }

  public BrowserDialog setRootPath(String rootPath) {
    mRootPath = rootPath;
    return this;
  }

  public boolean isStartRoot() {
    return mStartIsRoot;
  }

  public BrowserDialog setStartIsRoot(boolean startIsRoot) {
    mStartIsRoot = startIsRoot;
    return this;
  }

  public BrowserDialog setLayout(Layout layout) {
    mActiveLayout = layout;
    return this;
  }

  public BrowserDialog setShowHiddenFiles(boolean showHiddenFiles) {
    mShowHiddenFiles = showHiddenFiles;
    return this;
  }

  public BrowserDialog setLockedFolder(boolean isLocked) {
    mLocked = isLocked;
    return this;
  }

  public boolean getShowHiddenFiles() {
    return mShowHiddenFiles;
  }

  public interface OnDialogResultListener {

    void onPositiveResult(String path);

    void onNegativeResult();
  }
}
