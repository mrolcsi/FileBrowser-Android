package hu.mrolcsi.android.filebrowser.usb;

import android.os.AsyncTask;
import com.github.mjdev.libaums.fs.UsbFile;
import hu.mrolcsi.android.filebrowser.option.SortMode;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * Created by Matusinka Roland on 2016.04.14..
 */
public class UsbFileSorterTask extends AsyncTask<UsbFile, Integer, List<UsbFile>> {

  private final SortMode mSortMode;

  public UsbFileSorterTask(SortMode sortMode) {
    mSortMode = sortMode;
  }

  private List<UsbFile> sortFiles(UsbFile[] usbFiles, Comparator<UsbFile> comparator) {
    if (usbFiles != null) {
      List<UsbFile> files = new ArrayList<>();
      List<UsbFile> dirs = new ArrayList<>();
      for (int i = 0; i < usbFiles.length; i++) {
        UsbFile f = usbFiles[i];
        if (isCancelled()) {
          return null;
        }
        if (f.isDirectory()) {
          dirs.add(f);
        } else {
          files.add(f);
        }
        publishProgress(i, usbFiles.length);
      }
      Collections.sort(dirs, comparator);
      Collections.sort(files, comparator);
      List<UsbFile> list = new ArrayList<>();
      list.addAll(dirs);
      list.addAll(files);
      return list;
    } else {
      return null;
    }
  }

  @Override
  protected List<UsbFile> doInBackground(UsbFile... usbFiles) {
    switch (mSortMode) {
      default:
      case BY_NAME_ASC:
        return sortFiles(usbFiles, new UsbFileComparator.ByFileName());
      case BY_NAME_DESC:
        return sortFiles(usbFiles, Collections.reverseOrder(new UsbFileComparator.ByFileName()));
      case BY_EXTENSION_ASC:
        return sortFiles(usbFiles, new UsbFileComparator.ByExtension());
      case BY_EXTENSION_DESC:
        return sortFiles(usbFiles, Collections.reverseOrder(new UsbFileComparator.ByExtension()));
      case BY_DATE_ASC:
        return sortFiles(usbFiles, new UsbFileComparator.ByDate());
      case BY_DATE_DESC:
        return sortFiles(usbFiles, Collections.reverseOrder(new UsbFileComparator.ByDate()));
      case BY_SIZE_ASC:
        return sortFiles(usbFiles, new UsbFileComparator.BySize());
      case BY_SIZE_DESC:
        return sortFiles(usbFiles, Collections.reverseOrder(new UsbFileComparator.BySize()));
    }
  }
}
