package hu.mrolcsi.android.filebrowser;

import android.app.AlertDialog;
import android.app.DialogFragment;
import android.content.DialogInterface;
import android.os.Bundle;
import android.os.Environment;
import android.os.Parcelable;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.widget.*;
import hu.mrolcsi.android.filebrowser.option.BrowseMode;
import hu.mrolcsi.android.filebrowser.option.Layout;
import hu.mrolcsi.android.filebrowser.option.SortMode;

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
     * Tallózás módja:
     * <ul>
     * <li>Fájl megnyitása: {@link hu.mrolcsi.android.filebrowser.option.BrowseMode#OPEN_FILE OPEN_FILE}</li>
     * <li>Mappa kiválasztása: {@link hu.mrolcsi.android.filebrowser.option.BrowseMode#SELECT_DIR SELECT_DIR}</li>
     * <li>Fájl mentése: {@link hu.mrolcsi.android.filebrowser.option.BrowseMode#SAVE_FILE SAVE_FILE}</li>
     * </ul>
     * (Alapértelmezett: fájl megnyitása)
     *
     * @see hu.mrolcsi.android.filebrowser.option.BrowseMode
     */
    public static final String OPTION_BROWSE_MODE;
    /**
     * String:  Kezdőmappa abszolút elérési útja (Alapértelmezett: SD-kártya gyökere, ha nincs, "/")
     */
    public static final String OPTION_START_PATH;
    /**
     * String:  Engedélyezett kiterjesztések pontosvesszővel (;) elválasztva (Alapértelmezett: üres)
     */
    public static final String OPTION_EXTENSION_FILTER;
    /**
     * Visszatérési érték: a kiválasztott fájl/mappa abszolút elérési útja
     * onActivityResult metódusban használandó, mint getStringExtra paraméter.
     */
    public static final String RESULT;
    /**
     * Rendezés módja (mappák mindig elöl)
     * <ul>
     * <li>Név szerint növekvő: {@link hu.mrolcsi.android.filebrowser.option.SortMode#BY_NAME_ASC BY_NAME_ASC}</li>
     * <li>Név szerint csökkenő: {@link hu.mrolcsi.android.filebrowser.option.SortMode#BY_NAME_DESC BY_NAME_DESC}</li>
     * <li>Kiterjesztés szerint növekvő: {@link hu.mrolcsi.android.filebrowser.option.SortMode#BY_EXTENSION_ASC BY_EXTENSION_ASC}</li>
     * <li>Kiterjesztés szerint csökkenő: {@link hu.mrolcsi.android.filebrowser.option.SortMode#BY_EXTENSION_DESC BY_EXTENSION_DESC}</li>
     * <li>Módosítás dátuma szerint növekvő: {@link hu.mrolcsi.android.filebrowser.option.SortMode#BY_DATE_ASC BY_DATE_ASC}</li>
     * <li>Módosítás dátuma szerint csökkenő: {@link hu.mrolcsi.android.filebrowser.option.SortMode#BY_DATE_DESC BY_DATE_DESC}</li>
     * <li>Méret szerint növekvő: {@link hu.mrolcsi.android.filebrowser.option.SortMode#BY_SIZE_ASC BY_SIZE_ASC}</li>
     * <li>Méret szerint növekvő: {@link hu.mrolcsi.android.filebrowser.option.SortMode#BY_SIZE_DESC BY_SIZE_DESC}</li>
     * </ul>
     * (Alapértelmezett: fájlnév szerint növekvő)
     *
     * @see hu.mrolcsi.android.filebrowser.option.SortMode
     */
    public static final String OPTION_SORT_MODE;
    /**
     * String:  Alapértelmezett fájlnév, csak fájlmentéskor van rá szükség.
     *
     * @see hu.mrolcsi.android.filebrowser.option.BrowseMode
     */
    public static final String OPTION_DEFAULT_FILENAME;
    /**
     * Boolean: A kiindulópontként megadott mappát kezelje-e gyökérként? (boolean)
     *
     * @see #OPTION_START_PATH
     */
    public static final String OPTION_START_IS_ROOT;
    /**
     * Kezdeti elrendezés (futás közben váltogatható)
     * <ul>
     * <li>Lista {@link hu.mrolcsi.android.filebrowser.option.Layout#LIST LIST}</li>
     * <li>Négyzetrácsos(grid) {@link hu.mrolcsi.android.filebrowser.option.Layout#GRID GRID}</li>
     * </ul>
     * Alapértelmezett: lista.
     *
     * @see hu.mrolcsi.android.filebrowser.option.Layout
     */
    public static final String OPTION_LAYOUT;

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

    //endregion
    //region Privates

    private static final SortMode[] SORT_HASHES = new SortMode[]{
            SortMode.BY_NAME_ASC,
            SortMode.BY_NAME_DESC,
            SortMode.BY_EXTENSION_ASC,
            SortMode.BY_EXTENSION_DESC,
            SortMode.BY_DATE_ASC,
            SortMode.BY_DATE_DESC,
            SortMode.BY_SIZE_ASC,
            SortMode.BY_SIZE_DESC
    };

    private AbsListView list;
    private BrowseMode browseMode = BrowseMode.OPEN_FILE;
    private SortMode sortMode = SortMode.BY_NAME_ASC;
    private String[] extensionFilter;
    private String defaultFileName;
    private String currentExtension;
    private String startPath = Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED) ? Environment.getExternalStorageDirectory().getAbsolutePath() : "/";
    private String rootPath = File.listRoots()[0].getAbsolutePath();
    private String currentPath = startPath;
    private boolean startIsRoot = true;
    private Layout activeLayout = Layout.LIST;
    private TextView tvCurrentPath;
    private int itemLayoutID = R.layout.browser_listitem_layout;
    private ImageButton btnSave;
    private EditText etFilename;
    private ViewFlipper vf;

    private OnDialogResultListener onDialogResultListener = new OnDialogResultListener() {
        @Override
        public void onPositiveResult(String path) {
        }

        @Override
        public void onNegativeResult() {
        }
    };
    private Map<String, Parcelable> states = new ConcurrentHashMap<String, Parcelable>();
    private ImageButton btnSwitchLayout;
    private ImageButton btnNewFolder;
    private boolean overwrite = false;
    //</editor-fold>


    public BrowserDialog() {
        super();
        setStyle(STYLE_NO_TITLE, 0);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        if (savedInstanceState != null) {
            startPath = savedInstanceState.getString(OPTION_START_PATH, "/");
            currentPath = savedInstanceState.getString("currentPath", startPath);
            browseMode = (BrowseMode) savedInstanceState.getSerializable(OPTION_BROWSE_MODE);
            sortMode = (SortMode) savedInstanceState.getSerializable(OPTION_SORT_MODE);
            extensionFilter = savedInstanceState.getStringArray(OPTION_EXTENSION_FILTER);
            startIsRoot = savedInstanceState.getBoolean(OPTION_START_IS_ROOT, true);
            activeLayout = (Layout) savedInstanceState.getSerializable(OPTION_LAYOUT);
            itemLayoutID = savedInstanceState.getInt("itemLayoutID");
            defaultFileName = savedInstanceState.getString(OPTION_DEFAULT_FILENAME);
        } else {
            currentPath = startPath;
        }
        super.onCreate(savedInstanceState);
    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.browser_layout_dialog, container, false);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        btnSwitchLayout = (ImageButton) view.findViewById(R.id.browser_btnLayout);
        btnSwitchLayout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                setLayout();
            }
        });

        ImageButton btnSortMode = (ImageButton) view.findViewById(R.id.browser_btnSort);
        btnSortMode.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                showSortDialog();
            }
        });

        btnNewFolder = (ImageButton) view.findViewById(R.id.browser_btnNewFolder);
        btnNewFolder.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                showNewFolderDialog();
            }
        });

        tvCurrentPath = (TextView) view.findViewById(R.id.browser_textViewCurrentDir);

        vf = (ViewFlipper) view.findViewById(R.id.browser_viewFlipper);
        switch (activeLayout) {
            default:
            case LIST:
                toListView();
                break;
            case GRID:
                toGridView();
                break;
        }

        if (browseMode == BrowseMode.SAVE_FILE) {
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
            if (extensionFilter != null) {
                final ArrayAdapter<String> extensionAdapter = new ArrayAdapter<String>(getActivity(), android.R.layout.simple_spinner_item, extensionFilter);
                extensionAdapter.setDropDownViewResource(R.layout.browser_dropdown_item);
                spnExtension.setAdapter(extensionAdapter);
                spnExtension.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                    @Override
                    public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
                        currentExtension = (String) spnExtension.getSelectedItem();
                    }

                    @Override
                    public void onNothingSelected(AdapterView<?> adapterView) {
                    }
                });
            } else spnExtension.setVisibility(View.GONE);

            if (defaultFileName != null) etFilename.setText(defaultFileName);

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
        String result = currentPath + "/" + filename;

        if (!result.isEmpty() && Utils.isFilenameValid(result)) {
            File f = new File(result);
            if (f.exists()) {
                if (!overwrite) {
                    Toast.makeText(getActivity(), getString(R.string.browser_confirmOverwrite), Toast.LENGTH_SHORT).show();
                    overwrite = true;
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
            showErrorDialog(Error.INVALID_FILENAME);
        }
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        outState.putString("currentPath", currentPath);
        outState.putSerializable(OPTION_BROWSE_MODE, browseMode);
        outState.putSerializable(OPTION_SORT_MODE, sortMode);
        outState.putStringArray(OPTION_EXTENSION_FILTER, extensionFilter);
        outState.putString(OPTION_START_PATH, startPath);
        outState.putBoolean(OPTION_START_IS_ROOT, startIsRoot);
        outState.putSerializable(OPTION_LAYOUT, activeLayout);
        outState.putInt("itemLayoutID", itemLayoutID);
        outState.putString(OPTION_DEFAULT_FILENAME, defaultFileName);
        super.onSaveInstanceState(outState);
    }

    /**
     * Váltás Lista és Grid nézet között.
     */
    private void setLayout() {
        switch (activeLayout) {
            default:
            case LIST:
                toGridView();
                break;
            case GRID:
                toListView();
                break;
        }
        setListListeners();
        states = new ConcurrentHashMap<String, Parcelable>();
        loadList(new File(currentPath));
    }

    /**
     * Lista nézetbe váltás ViewFlipperen keresztül.
     */
    private void toListView() {
        vf.setDisplayedChild(0);
        activeLayout = Layout.LIST;
        btnSwitchLayout.setImageResource(R.drawable.browser_grid);
        list = (ListView) vf.findViewById(R.id.browser_listView);
        itemLayoutID = R.layout.browser_listitem_layout;
        setListListeners();
        loadList(new File(currentPath));
    }

    /**
     * Grid nézetbe váltás ViewFlipperen keresztül.
     */
    private void toGridView() {
        vf.setDisplayedChild(1);
        activeLayout = Layout.GRID;
        btnSwitchLayout.setImageResource(R.drawable.browser_list);
        list = (GridView) vf.findViewById(R.id.browser_gridView);
        itemLayoutID = R.layout.browser_griditem_layout;
        setListListeners();
        loadList(new File(currentPath));
    }

    /**
     * Dialógus megjelenítése a rendezési mód kiválasztásához.
     */
    private void showSortDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity())
                .setTitle(R.string.browser_menu_sortBy)
                .setIcon(R.drawable.browser_sort)
                .setItems(R.array.browser_sortOptions, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        sortMode = SORT_HASHES[i];
                        loadList(new File(currentPath));
                    }
                });
        AlertDialog ad = builder.create();
        ad.show();
    }

    /**
     * View váltás után listenerek újraregisztrálása.
     */
    private void setListListeners() {
        switch (browseMode) {
            default:
            case OPEN_FILE:
                list.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                    @Override
                    public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                        FileHolder holder = (FileHolder) view.getTag();
                        if (holder.file.getAbsolutePath().equals("/" + getString(R.string.browser_upFolder))) {
                            loadList(new File(currentPath).getParentFile());
                        } else {
                            if (holder.file.isDirectory()) loadList(holder.file);
                            if (holder.file.isFile()) {
                                onDialogResultListener.onPositiveResult(holder.file.getAbsolutePath());
                                dismiss();
                            }
                        }
                    }
                });
                list.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
                    @Override
                    public boolean onItemLongClick(AdapterView<?> adapterView, View view, int i, long l) {
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
                list.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                    @Override
                    public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                        FileHolder holder = (FileHolder) view.getTag();
                        if (holder.file.getAbsolutePath().equals("/" + getString(R.string.browser_upFolder))) {
                            loadList(new File(currentPath).getParentFile());
                        } else if (holder.file.isDirectory()) loadList(holder.file);
                    }
                });
                list.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
                    @Override
                    public boolean onItemLongClick(AdapterView<?> adapterView, View view, int i, long l) {
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
                list.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                    @Override
                    public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                        FileHolder holder = (FileHolder) view.getTag();
                        if (holder.file.getAbsolutePath().equals("/" + getString(R.string.browser_upFolder))) {
                            loadList(new File(currentPath).getParentFile());
                        } else {
                            if (holder.file.isFile()) etFilename.setText(holder.file.getName());
                            if (holder.file.isDirectory()) loadList(holder.file);
                        }
                    }
                });
                list.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
                    @Override
                    public boolean onItemLongClick(AdapterView<?> adapterView, View view, int i, long l) {
                        FileHolder holder = (FileHolder) view.getTag();
                        if (!holder.file.isFile()) return false;
                        else {
                            showOverwriteDialog(holder.file.getAbsolutePath());
                            Toast.makeText(getActivity(), "Press Save button twice to overwrite file.", Toast.LENGTH_LONG).show();
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

        states.put(currentPath, list.onSaveInstanceState());

        File[] filesToLoad;

        if (extensionFilter != null) filesToLoad = directory.listFiles(new FileFilter() {
            @Override
            public boolean accept(File file) {
                if (file.isFile()) {
                    String ext = Utils.getExtension(file.getName());
                    int i = 0;
                    int n = extensionFilter.length;
                    while (i < n && !extensionFilter[i].toLowerCase().equals(ext)) i++;
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

        currentPath = directory.getAbsolutePath();
        tvCurrentPath.setText(currentPath);

        FileListAdapter fla;
        boolean isRoot = startIsRoot ? currentPath.equals(startPath) || currentPath.equals(rootPath) : currentPath.equals(rootPath);

        Log.d(getClass().getName(), "root path = " + rootPath);
        Log.d(getClass().getName(), "start path = " + startPath);
        Log.d(getClass().getName(), "current path = " + currentPath);

        switch (browseMode) {
            default:
            case SAVE_FILE:
            case OPEN_FILE:
                fla = new FileListAdapter(getActivity(), itemLayoutID, filesToLoad, sortMode, isRoot);
                break;
            case SELECT_DIR:
                FileFilter filter = new FileFilter() {
                    @Override
                    public boolean accept(File file) {
                        return file.isDirectory();
                    }
                };
                fla = new FileListAdapter(getActivity(), itemLayoutID, directory.listFiles(filter), sortMode, isRoot);
                break;
        }

        //API Level 11 alatt castolni kell...
        switch (activeLayout) {
            case GRID:
                //noinspection RedundantCast
                ((GridView) list).setAdapter(fla);
                break;
            case LIST:
                //noinspection RedundantCast
                ((ListView) list).setAdapter(fla);
                break;
        }
        //if (browseMode == MODE_SAVE_FILE) btnSave.setEnabled(directory.canWrite());
        Parcelable state = states.get(currentPath);
        if (state != null)
            list.onRestoreInstanceState(state);

        File currentFile = new File(currentPath);
        btnNewFolder.setVisibility(currentFile.canWrite() ? View.VISIBLE : View.GONE);
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
        final View view = getActivity().getLayoutInflater().inflate(R.layout.browser_dialog_newfolder, null);
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity())
                .setTitle(R.string.browser_menu_newFolder)
                .setIcon(R.drawable.browser_new_folder).setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        EditText etFolderName = (EditText) view.findViewById(R.id.browser_etNewFolder);
                        if (Utils.isFilenameValid(etFolderName.getText().toString())) {
                            File newDir = new File(currentPath + "/" + etFolderName.getText());
                            if (newDir.mkdir()) {
                                loadList(new File(currentPath));
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
        } else return input + "." + currentExtension;

        if (Utils.contains(extensionFilter, extension)) {
            return input + "." + currentExtension;
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

    //<editor-fold desc="GETTERS & SETTERS">
    @SuppressWarnings("UnusedDeclaration")
    public BrowseMode getBrowseMode() {
        return browseMode;
    }

    @SuppressWarnings("UnusedDeclaration")
    public BrowserDialog setBrowseMode(BrowseMode browseMode) {
        this.browseMode = browseMode;
        return this;
    }

    @SuppressWarnings("UnusedDeclaration")
    public SortMode getSortMode() {
        return sortMode;
    }

    @SuppressWarnings("UnusedDeclaration")
    public BrowserDialog setSortMode(SortMode sortMode) {
        this.sortMode = sortMode;
        return this;
    }

    @SuppressWarnings("UnusedDeclaration")
    public String[] getExtensionFilter() {
        return extensionFilter;
    }

    @SuppressWarnings("UnusedDeclaration")
    public BrowserDialog setExtensionFilter(String extensionFilter) {
        this.extensionFilter = extensionFilter.split(";");
        return this;
    }

    public BrowserDialog setExtensionFilter(String... extensions) {
        this.extensionFilter = extensions;
        return this;
    }

    @SuppressWarnings("UnusedDeclaration")
    public String getDefaultFileName() {
        return defaultFileName;
    }

    @SuppressWarnings("UnusedDeclaration")
    public BrowserDialog setDefaultFileName(String defaultFileName) {
        this.defaultFileName = defaultFileName;
        return this;
    }

    @SuppressWarnings("UnusedDeclaration")
    public String getStartPath() {
        return startPath;
    }

    @SuppressWarnings("UnusedDeclaration")
    public BrowserDialog setStartPath(String startPath) {
        this.startPath = startPath;
        return this;
    }

    public BrowserDialog setRootPath(String rootPath) {
        this.rootPath = rootPath;
        return this;
    }

    @SuppressWarnings("UnusedDeclaration")
    public boolean isStartRoot() {
        return startIsRoot;
    }

    @SuppressWarnings("UnusedDeclaration")
    public BrowserDialog setStartIsRoot(boolean startIsRoot) {
        this.startIsRoot = startIsRoot;
        return this;
    }
//</editor-fold>

    public interface OnDialogResultListener {
        /**
         * Visszatérés a kiválasztott fájl/mappa teljes elérési útjával.
         *
         * @param path A hívó Activityben felhasználható elérési út.
         */
        public abstract void onPositiveResult(String path);

        /**
         * Nem lett kiválasztva fájl/mappa.
         * A dialógus bezárult.
         */
        public abstract void onNegativeResult();
    }
}
