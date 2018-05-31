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

import android.content.Context;
import android.os.AsyncTask;
import android.view.View;
import android.view.ViewGroup;
import com.github.mjdev.libaums.fs.UsbFile;
import hu.mrolcsi.android.filebrowser.FileListAdapter;
import hu.mrolcsi.android.filebrowser.R;
import hu.mrolcsi.android.filebrowser.option.BrowseMode;
import hu.mrolcsi.android.filebrowser.option.SortMode;
import hu.mrolcsi.android.filebrowser.util.FileUtils;
import hu.mrolcsi.android.filebrowser.util.Utils;
import java.io.IOException;
import java.lang.ref.WeakReference;
import java.text.DateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * Created by Matusinka Roland on 2016.04.13..
 */
public class UsbFileListAdapter extends FileListAdapter {

  private static final DateFormat DATE_FORMAT = DateFormat.getDateTimeInstance();
  private final boolean mIsRoot;

  private final List<UsbFile> mFiles;
  private final UsbFile mCurrentDir;

  public UsbFileListAdapter(Context context, int layoutResourceId, BrowseMode browseMode,
      SortMode sortMode, UsbFile directory, List<UsbFile> contents, boolean isRoot) {
    super(context, layoutResourceId, null, browseMode, sortMode, isRoot);

    mCurrentDir = directory;
    mFiles = contents;
    mIsRoot = isRoot;
    refresh();
  }

  private void refresh() {
    if (browseMode == BrowseMode.SELECT_DIR) {
      mFiles.add(0, new DummyFile() {
        @Override
        public String getName() {
          return context.getString(R.string.browser_titleSelectDir);
        }

        @Override
        public UsbFile getParent() {
          return mCurrentDir;
        }
      });
    } else {
      if (mFiles.size() == 0) {
        mFiles.add(0, new DummyFile() {
          @Override
          public String getName() {
            return context.getString(R.string.browser_emptyFolder);
          }

          @Override
          public UsbFile getParent() {
            return mCurrentDir.getParent();
          }
        });
      }
    }

    if (!mIsRoot && !mCurrentDir.isRoot()) {
      mFiles.add(0, new DummyFile() {
        @Override
        public String getName() {
          return context.getString(R.string.browser_upFolder);
        }

        @Override
        public UsbFile getParent() {
          return mCurrentDir.getParent();
        }
      });
    }

    notifyDataSetChanged();
  }

  @Override
  public int getItemCount() {
    return mFiles.size();
  }

  @Override
  public FileHolder onCreateViewHolder(ViewGroup container, int itemType) {
    final View itemView = inflater.inflate(layoutResourceId, container, false);
    return new UsbFileHolder(itemView);
  }

  @Override
  public void onViewRecycled(FileHolder holder) {
    super.onViewRecycled(holder);
    UsbFileHolder usbHolder = (UsbFileHolder) holder;
    if (usbHolder.sizeCalculator != null) {
      usbHolder.sizeCalculator.cancel(true);
      usbHolder.sizeCalculator = null;
    }
  }

  @Override
  public void onBindViewHolder(final FileHolder holder, int i) {
    final UsbFileHolder usbHolder = (UsbFileHolder) holder;
    usbHolder.usbFile = mFiles.get(i);
    usbHolder.itemView.setTag(usbHolder);

    final boolean isUp = usbHolder.usbFile.getName()
        .equals(context.getString(R.string.browser_upFolder));
    final boolean isDirSelector = usbHolder.usbFile.getName()
        .equals(context.getString(R.string.browser_titleSelectDir));

    if (isUp) {
      usbHolder.icon
          .setImageDrawable(Utils.getTintedDrawable(context, R.drawable.browser_left_up_2));
      usbHolder.extra.setText(null);
    } else if (isDirSelector) {
      usbHolder.icon
          .setImageDrawable(Utils.getTintedDrawable(context, R.drawable.browser_checkmark));
      usbHolder.extra.setText(null);
    } else {
      if (usbHolder.usbFile.isDirectory()) {
        usbHolder.icon
            .setImageDrawable(Utils.getTintedDrawable(context, R.drawable.browser_folder));
      } else {
        //TODO: switch (extension) -> document, image, music, video, text, other
        usbHolder.icon.setImageDrawable(Utils.getTintedDrawable(context, R.drawable.browser_file));
      }
    }

    usbHolder.text.setText(usbHolder.usbFile.getName());

    if (!isUp && !isDirSelector) {
      switch (sortMode) {
        default:
        case BY_NAME_ASC:
        case BY_NAME_DESC:
          if (usbHolder.extra != null) {
            usbHolder.extra.setVisibility(View.GONE);
          }
          break;
        case BY_EXTENSION_ASC:
        case BY_EXTENSION_DESC:
          if (!usbHolder.usbFile.isDirectory()
              && FileUtils.getExtension(usbHolder.usbFile.getName()) != null) {
            usbHolder.text.setText(FileUtils.getNameWithoutExtension(usbHolder.usbFile.getName()));
            if (usbHolder.extra != null) {
              usbHolder.extra.setVisibility(View.VISIBLE);
              usbHolder.extra.setText(new StringBuilder().append(".")
                  .append(FileUtils.getExtension(usbHolder.usbFile.getName())));
            }
          } else {
            if (usbHolder.extra != null) {
              usbHolder.extra.setVisibility(View.GONE);
            }
          }
          break;
        case BY_DATE_ASC:
        case BY_DATE_DESC:
          usbHolder.extra.setVisibility(View.VISIBLE);
          usbHolder.extra.setText(DATE_FORMAT.format(new Date(usbHolder.usbFile.lastModified())));
          break;
        case BY_SIZE_ASC:
        case BY_SIZE_DESC:
          usbHolder.sizeCalculator = new SizeCalculatorTask(context, usbHolder).execute(usbHolder.usbFile);
          break;
      }
    }
  }

  class UsbFileHolder extends FileHolder {

    AsyncTask<UsbFile, Long, Long> sizeCalculator;
    UsbFile usbFile;

    UsbFileHolder(View itemView) {
      super(itemView);
    }
  }

  private static class SizeCalculatorTask extends AsyncTask<UsbFile, Long, Long> {

    private final WeakReference<Context> wrContext;
    private final UsbFileHolder mHolder;

    public SizeCalculatorTask(Context context, UsbFileHolder holder) {
      wrContext = new WeakReference<>(context);
      mHolder = holder;
    }

    @Override
    protected void onPreExecute() {
      mHolder.extra.setVisibility(View.GONE);
      mHolder.progress.setVisibility(View.VISIBLE);
    }

    @Override
    protected Long doInBackground(UsbFile... usbFiles) {
      if (!usbFiles[0].isDirectory()) {
        return usbFiles[0].getLength();
      } else {
        return FileUtils.dirSize(usbFiles[0]);
      }
    }

    @Override
    protected void onProgressUpdate(Long... values) {
      mHolder.extra.setText(FileUtils.getFriendlySize(values[0]));
    }

    @Override
    protected void onPostExecute(Long size) {
      if (mHolder.usbFile.isDirectory()) {
        int count;
        try {
          count = mHolder.usbFile.listFiles().length;
          mHolder.extra.setText(String.format(Locale.getDefault(), "%s\n%s",
              wrContext.get().getResources().getQuantityString(R.plurals.browser_numberOfFiles, count, count),
              FileUtils.getFriendlySize(size)));
        } catch (NullPointerException e) {
          mHolder.extra.setText(R.string.browser_unknown);
        } catch (IOException e) {
          mHolder.extra.setText(R.string.browser_error_folderCantBeOpened_message);
        }
      } else {
        mHolder.extra.setText(FileUtils.getFriendlySize(mHolder.usbFile.getLength()));
      }
      mHolder.extra.setVisibility(View.VISIBLE);
      mHolder.progress.setVisibility(View.GONE);
    }
  }
}
