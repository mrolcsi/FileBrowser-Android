package hu.mrolcsi.android.filebrowser.usb;

import android.content.Context;
import android.os.AsyncTask;
import android.view.View;
import android.view.ViewGroup;

import java.io.IOException;
import java.text.DateFormat;
import java.util.Date;
import java.util.List;

import com.github.mjdev.libaums.fs.UsbFile;
import hu.mrolcsi.android.filebrowser.FileListAdapter;
import hu.mrolcsi.android.filebrowser.R;
import hu.mrolcsi.android.filebrowser.option.BrowseMode;
import hu.mrolcsi.android.filebrowser.option.SortMode;
import hu.mrolcsi.android.filebrowser.util.FileUtils;
import hu.mrolcsi.android.filebrowser.util.Utils;

/**
 * Created by Matusinka Roland on 2016.04.13..
 */
public class UsbFileListAdapter extends FileListAdapter {
    private static final DateFormat DATE_FORMAT = DateFormat.getDateTimeInstance();
    private List<UsbFile> mFiles;
    private UsbFile mCurrentDir;

    public UsbFileListAdapter(Context context, int layoutResourceId, BrowseMode browseMode, SortMode sortMode, UsbFile directory, List<UsbFile> contents, boolean isRoot) {
        super(context, layoutResourceId, null, browseMode, sortMode, isRoot);

        mCurrentDir = directory;
        mFiles = contents;
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

        if (!mCurrentDir.isRoot()) {
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
    public void onBindViewHolder(final FileHolder holder, int i) {
        final UsbFileHolder usbHolder = (UsbFileHolder) holder;
        usbHolder.usbFile = mFiles.get(i);
        usbHolder.itemView.setTag(usbHolder);

        final boolean isUp = usbHolder.usbFile.getName().equals(context.getString(R.string.browser_upFolder));
        final boolean isDirSelector = usbHolder.usbFile.getName().equals(context.getString(R.string.browser_titleSelectDir));

        if (isUp) {
            usbHolder.icon.setImageDrawable(Utils.getTintedDrawable(context, R.drawable.browser_left_up_2));
            usbHolder.extra.setText(null);
        } else if (isDirSelector) {
            usbHolder.icon.setImageDrawable(Utils.getTintedDrawable(context, R.drawable.browser_checkmark));
            usbHolder.extra.setText(null);
        } else {
            if (usbHolder.usbFile.isDirectory()) {
                usbHolder.icon.setImageDrawable(Utils.getTintedDrawable(context, R.drawable.browser_folder));
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
                    if (usbHolder.extra != null)
                        usbHolder.extra.setVisibility(View.GONE);
                    break;
                case BY_EXTENSION_ASC:
                case BY_EXTENSION_DESC:
                    if (!usbHolder.usbFile.isDirectory() && FileUtils.getExtension(usbHolder.usbFile.getName()) != null) {
                        usbHolder.text.setText(FileUtils.getNameWithoutExtension(usbHolder.usbFile.getName()));
                        if (usbHolder.extra != null) {
                            usbHolder.extra.setVisibility(View.VISIBLE);
                            usbHolder.extra.setText(new StringBuilder().append(".").append(FileUtils.getExtension(usbHolder.usbFile.getName())));
                        }
                    } else {
                        if (usbHolder.extra != null)
                            usbHolder.extra.setVisibility(View.GONE);
                    }
                    break;
                case BY_DATE_ASC:
                case BY_DATE_DESC:
                    usbHolder.extra.setVisibility(View.VISIBLE);
                    usbHolder.extra.setText(DATE_FORMAT.format(new Date(usbHolder.usbFile.lastModified())));
                    break;
                case BY_SIZE_ASC:
                case BY_SIZE_DESC:
                    usbHolder.sizeCalculator = new AsyncTask<UsbFile, Long, Long>() {
                        @Override
                        protected void onPreExecute() {
                            holder.extra.setVisibility(View.GONE);
                            holder.progress.setVisibility(View.VISIBLE);
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
                            holder.extra.setText(FileUtils.getFriendlySize(values[0]));
                        }

                        @Override
                        protected void onPostExecute(Long size) {
                            if (usbHolder.usbFile.isDirectory()) {
                                int count;
                                try {
                                    count = usbHolder.usbFile.listFiles().length;
                                    usbHolder.extra.setText(context.getResources().getQuantityString(R.plurals.browser_numberOfFiles, count, count) + "\n" + FileUtils.getFriendlySize(size));
                                } catch (NullPointerException e) {
                                    usbHolder.extra.setText(R.string.browser_unknown);
                                } catch (IOException e) {
                                    usbHolder.extra.setText(R.string.browser_error_folderCantBeOpened_message);
                                }
                            } else {
                                usbHolder.extra.setText(FileUtils.getFriendlySize(usbHolder.usbFile.getLength()));
                            }
                            usbHolder.extra.setVisibility(View.VISIBLE);
                            usbHolder.progress.setVisibility(View.GONE);
                        }
                    }.execute(usbHolder.usbFile);
                    break;
            }
        }
    }

    protected class UsbFileHolder extends FileHolder {

        public AsyncTask<UsbFile, Long, Long> sizeCalculator;
        UsbFile usbFile;

        public UsbFileHolder(View itemView) {
            super(itemView);
        }
    }
}
