package hu.mrolcsi.android.filebrowser;

import android.content.Context;
import android.os.AsyncTask;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import hu.mrolcsi.android.filebrowser.option.BrowseMode;
import hu.mrolcsi.android.filebrowser.option.SortMode;
import hu.mrolcsi.android.filebrowser.util.FileUtils;
import hu.mrolcsi.android.filebrowser.util.SizeCalculatorTask;
import hu.mrolcsi.android.filebrowser.util.Utils;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Created with IntelliJ IDEA.
 * User: Matusinka Roland
 * Date: 2013.03.16.
 * Time: 16:17
 */

public class FileListAdapter extends RecyclerView.Adapter<FileListAdapter.FileHolder> {

  protected final int layoutResourceId;
  protected final Context context;
  protected final BrowseMode browseMode;
  protected final SortMode sortMode;
  protected final LayoutInflater inflater;
  private List<File> data;

  public FileListAdapter(Context context, int layoutResourceId, List<File> inputData,
      BrowseMode browseMode, SortMode sortMode, boolean isRoot) {
    super();
    this.context = context;
    this.layoutResourceId = layoutResourceId;
    this.browseMode = browseMode;
    this.sortMode = sortMode;
    inflater = LayoutInflater.from(context);

    data = inputData == null ? new ArrayList<File>() : inputData;

    if (browseMode == BrowseMode.SELECT_DIR) {
      data.add(0, new File(context.getString(R.string.browser_titleSelectDir)));
    }

    if (!isRoot) {
      data.add(0, new File(context.getString(R.string.browser_upFolder)));
    }

    if (inputData == null || inputData.size() <= 0) {
      if (browseMode != BrowseMode.SELECT_DIR) {
        data.add(new File(context.getString(R.string.browser_emptyFolder)));
      }
    }
  }

  @Override
  public FileHolder onCreateViewHolder(ViewGroup container, int itemType) {
    final View itemView = inflater.inflate(layoutResourceId, container, false);
    return new FileHolder(itemView);
  }

  @Override
  public void onViewRecycled(FileHolder holder) {
    super.onViewRecycled(holder);

    if (holder.sizeCalculator != null) {
      holder.sizeCalculator.cancel(true);
      holder.sizeCalculator = null;
    }
  }

  @Override
  public void onBindViewHolder(final FileHolder holder, int i) {
    holder.file = data.get(i);
    holder.itemView.setTag(holder);

    final boolean isUp = holder.file.getAbsolutePath()
        .equals(File.separator + context.getString(R.string.browser_upFolder));
    final boolean isDirSelector = holder.file.getAbsolutePath()
        .equals(File.separator + context.getString(R.string.browser_titleSelectDir));

    if (isUp) {
      holder.icon.setImageDrawable(Utils.getTintedDrawable(context, R.drawable.browser_left_up_2));
      holder.extra.setText(null);
    } else if (isDirSelector) {
      holder.icon.setImageDrawable(Utils.getTintedDrawable(context, R.drawable.browser_checkmark));
      holder.extra.setText(null);
    } else {
      if (holder.file.isDirectory()) {
        holder.icon.setImageDrawable(Utils.getTintedDrawable(context, R.drawable.browser_folder));
      }
      if (holder.file.isFile()) {
        //TODO: switch (extension) -> document, image, music,video,text,other
        holder.icon.setImageDrawable(Utils.getTintedDrawable(context, R.drawable.browser_file));
      }
    }

    holder.text.setText(holder.file.getName());

    if (!isUp && !isDirSelector) {
      switch (sortMode) {
        default:
        case BY_NAME_ASC:
        case BY_NAME_DESC:
          if (holder.extra != null) {
            holder.extra.setVisibility(View.GONE);
          }
          break;
        case BY_EXTENSION_ASC:
        case BY_EXTENSION_DESC:
          if (holder.file.isFile() && FileUtils.getExtension(holder.file.getName()) != null) {
            holder.text.setText(FileUtils.getNameWithoutExtension(holder.file.getName()));
            if (holder.extra != null) {
              holder.extra.setVisibility(View.VISIBLE);
              holder.extra.setText(new StringBuilder().append(".")
                  .append(FileUtils.getExtension(holder.file.getName())));
            }
          } else {
            if (holder.extra != null) {
              holder.extra.setVisibility(View.GONE);
            }
          }
          break;
        case BY_DATE_ASC:
        case BY_DATE_DESC:
          holder.extra.setVisibility(View.VISIBLE);
          holder.extra.setText(String.format(Locale.getDefault(), "%1$tY.%1$tm.%1$td\n%1$tH:%1$tM",
              holder.file.lastModified()));
          break;
        case BY_SIZE_ASC:
        case BY_SIZE_DESC:
          holder.sizeCalculator = new SizeCalculatorTask() {
            @Override
            protected void onPreExecute() {
              super.onPreExecute();
              holder.extra.setVisibility(View.GONE);
              holder.progress.setVisibility(View.VISIBLE);
            }

            @Override
            protected void onProgressUpdate(Long... values) {
              super.onProgressUpdate(values);
              holder.extra.setText(FileUtils.getFriendlySize(values[0]));
            }

            @Override
            protected void onPostExecute(Long size) {
              super.onPostExecute(size);
              if (holder.file.isDirectory()) {
                int count;
                try {
                  count = holder.file.listFiles().length;
                  holder.extra.setText(String.format(Locale.getDefault(), "%s\n%s",
                      context.getResources()
                          .getQuantityString(R.plurals.browser_numberOfFiles, count, count),
                      FileUtils.getFriendlySize(size)));
                } catch (NullPointerException e) {
                  holder.extra.setText(R.string.browser_unknown);
                }
              } else if (holder.file.isFile()) {
                holder.extra.setText(FileUtils.getFriendlySize(size));
              }
              holder.extra.setVisibility(View.VISIBLE);
              holder.progress.setVisibility(View.GONE);
            }
          }.execute(holder.file);
          break;
      }
    }
  }

  @Override
  public int getItemCount() {
    return data.size();
  }

  protected class FileHolder extends RecyclerView.ViewHolder {

    public ImageView icon;
    public TextView text;
    public TextView extra;
    public ProgressBar progress;
    AsyncTask<File, Long, Long> sizeCalculator;
    File file;

    public FileHolder(View itemView) {
      super(itemView);

      icon = (ImageView) itemView.findViewById(R.id.browser_listItemIcon);
      text = (TextView) itemView.findViewById(R.id.browser_listItemText);
      extra = (TextView) itemView.findViewById(R.id.browser_listItemExtra);
      progress = (ProgressBar) itemView.findViewById(R.id.browser_listItemProgress);
    }
  }
}