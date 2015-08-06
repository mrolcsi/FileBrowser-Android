package hu.mrolcsi.android.filebrowser;

import android.app.Activity;
import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import hu.mrolcsi.android.filebrowser.option.BrowseMode;
import hu.mrolcsi.android.filebrowser.option.SortMode;
import hu.mrolcsi.android.filebrowser.util.SizeCalculatorTask;
import hu.mrolcsi.android.filebrowser.util.Utils;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * Created with IntelliJ IDEA.
 * User: Matusinka Roland
 * Date: 2013.03.16.
 * Time: 16:17
 */

class FileListAdapter extends RecyclerView.Adapter<FileHolder> {

    private final int layoutResourceId;
    private final Context context;
    private final SortMode sortMode;
    private final LayoutInflater inflater;
    private List<File> data = null;

    public FileListAdapter(Context context, int layoutResourceId, File[] inputData, BrowseMode browseMode, SortMode sortMode, boolean isRoot) {
        super();
        this.context = context;
        this.layoutResourceId = layoutResourceId;
        this.sortMode = sortMode;
        this.inflater = ((Activity) context).getLayoutInflater();

        data = new ArrayList<>();
        if (!isRoot) data.add(new File(context.getString(R.string.browser_upFolder)));

        if (inputData.length > 0) {
            switch (this.sortMode) {
                default:
                case BY_NAME_ASC:
                    this.data.addAll(Utils.sortByNameAsc(inputData));
                    break;
                case BY_NAME_DESC:
                    this.data.addAll(Utils.sortByNameDesc(inputData));
                    break;
                case BY_EXTENSION_ASC:
                    this.data.addAll(Utils.sortByExtensionAsc(inputData));
                    break;
                case BY_EXTENSION_DESC:
                    this.data.addAll(Utils.sortByExtensionDesc(inputData));
                    break;
                case BY_DATE_ASC:
                    this.data.addAll(Utils.sortByDateAsc(inputData));
                    break;
                case BY_DATE_DESC:
                    this.data.addAll(Utils.sortByDateDesc(inputData));
                    break;
                case BY_SIZE_ASC:
                    this.data.addAll(Utils.sortBySizeAsc(inputData));
                    break;
                case BY_SIZE_DESC:
                    this.data.addAll(Utils.sortBySizeDesc(inputData));
                    break;
            }
        } else {
            if (browseMode != BrowseMode.SELECT_DIR)
                data.add(new File(context.getString(R.string.browser_emptyFolder)));
        }

        if (browseMode == BrowseMode.SELECT_DIR) {
            data.add(new File(context.getString(R.string.browser_titleSelectDir)));
        }
    }

    @Override
    public FileHolder onCreateViewHolder(ViewGroup container, int itemType) {
        final View itemView = inflater.inflate(layoutResourceId, container, false);
        return new FileHolder(itemView);
    }

    @Override
    public void onBindViewHolder(final FileHolder holder, int i) {
        holder.file = data.get(i);
        holder.itemView.setTag(holder);

        final boolean isUp = holder.file.getAbsolutePath().equals("/..");

        if (isUp) {
            holder.icon.setImageResource(R.drawable.browser_left_up_2_dark);
        } else {
            if (holder.file.isDirectory()) {
                holder.icon.setImageResource(R.drawable.browser_folder_dark);
            }
            if (holder.file.isFile()) {
                //TODO: switch (extension) -> document, image, music,video,text,other
                holder.icon.setImageResource(R.drawable.browser_file_dark);
            }
            if (holder.file.getAbsolutePath().equals(File.separator + context.getString(R.string.browser_titleSelectDir))) {
                holder.icon.setImageResource(R.drawable.browser_checkmark_dark);
            }
        }

        switch (sortMode) {
            default:
            case BY_NAME_ASC:
            case BY_NAME_DESC:
                holder.text.setText(holder.file.getName());
                if (holder.extra != null) holder.extra.setVisibility(View.GONE);
                break;
            case BY_EXTENSION_ASC:
            case BY_EXTENSION_DESC:
                if (holder.file.isFile() && Utils.getExtension(holder.file.getName()) != null) {
                    holder.text.setText(Utils.getNameWithoutExtension(holder.file.getName()));
                    if (holder.extra != null) {
                        holder.extra.setVisibility(View.VISIBLE);
                        holder.extra.setText(new StringBuilder().append(".").append(Utils.getExtension(holder.file.getName())));
                    }
                } else {
                    holder.text.setText(holder.file.getName());
                    if (holder.extra != null) holder.extra.setVisibility(View.GONE);
                }
                break;
            case BY_DATE_ASC:
            case BY_DATE_DESC:
                holder.text.setText(holder.file.getName());
                if (!isUp && holder.extra != null) {
                    holder.extra.setVisibility(View.VISIBLE);
                    holder.extra.setText(String.format("%1$tY.%1$tm.%1$td\n%1$tH:%1$tM", holder.file.lastModified()));
                }
                break;
            case BY_SIZE_ASC:
            case BY_SIZE_DESC:
                holder.text.setText(holder.file.getName());
                new SizeCalculatorTask() {
                    @Override
                    protected void onPreExecute() {
                        super.onPreExecute();
                        holder.extra.setVisibility(View.GONE);
                        holder.progress.setVisibility(View.VISIBLE);
                    }

                    @Override
                    protected void onProgressUpdate(Long... values) {
                        super.onProgressUpdate(values);
                        holder.extra.setText(Utils.getFriendlySize(values[0]));
                    }

                    @Override
                    protected void onPostExecute(Long size) {
                        super.onPostExecute(size);
                        if (!isUp && holder.file.isDirectory()) {
                            int count;
                            try {
                                count = holder.file.listFiles().length;
                                holder.extra.setText(context.getResources().getQuantityString(R.plurals.browser_numberOfFiles, count, count) + "\n" + Utils.getFriendlySize(size));
                            } catch (NullPointerException e) {
                                holder.extra.setText(R.string.browser_unknown);
                            }
                        } else if (holder.file.isFile()) holder.extra.setText(Utils.getFriendlySize(size));
                        holder.extra.setVisibility(View.VISIBLE);
                        holder.progress.setVisibility(View.GONE);
                    }
                }.execute(holder.file);
                break;
        }
    }

    @Override
    public int getItemCount() {
        return data.size();
    }
}

class FileHolder extends RecyclerView.ViewHolder {
    ImageView icon;
    TextView text;
    TextView extra;
    ProgressBar progress;
    File file;

    public FileHolder(View itemView) {
        super(itemView);

        icon = (ImageView) itemView.findViewById(R.id.browser_listItemIcon);
        text = (TextView) itemView.findViewById(R.id.browser_listItemText);
        extra = (TextView) itemView.findViewById(R.id.browser_listItemExtra);
        progress = (ProgressBar) itemView.findViewById(R.id.browser_listItemProgress);
    }
}