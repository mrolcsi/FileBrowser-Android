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

import com.github.mjdev.libaums.fs.UsbFile;
import hu.mrolcsi.android.filebrowser.util.FileUtils;
import java.util.Comparator;

/**
 * Created by Matusinka Roland on 2016.04.14..
 */
public abstract class UsbFileComparator {

  public static class ByFileName implements Comparator<UsbFile> {

    @Override
    public int compare(UsbFile f1, UsbFile f2) {
      if (f1.isDirectory() && !f2.isDirectory()) {
        return 1;
      }
      if (!f1.isDirectory() && f2.isDirectory()) {
        return -1;
      }
      return f1.getName().compareToIgnoreCase(f2.getName());
    }
  }

  public static class ByExtension implements Comparator<UsbFile> {

    @Override
    public int compare(UsbFile f1, UsbFile f2) {
      if (f1.isDirectory() && f2.isDirectory()) {
        return f1.getName().compareToIgnoreCase(f2.getName());
      }

      if (f1.isDirectory() && !f2.isDirectory()) {
        return -1;
      }
      if (!f1.isDirectory() && f2.isDirectory()) {
        return 1;
      }

      String ext1 = FileUtils.getExtension(f1.getName());
      String ext2 = FileUtils.getExtension(f2.getName());
      if (ext1 == null) {
        return -1;
      }
      if (ext2 == null) {
        return 1;
      }
      return ext1.compareToIgnoreCase(ext2);
    }
  }

  public static class ByDate implements Comparator<UsbFile> {

    @Override
    public int compare(UsbFile f1, UsbFile f2) {
      long f1mod = f1.lastModified();
      long f2mod = f2.lastModified();
      return (int) (f1mod - f2mod);
    }
  }

  public static class BySize implements Comparator<UsbFile> {

    @Override
    public int compare(UsbFile f1, UsbFile f2) {
      if (f1 == null) {
        return 1;
      }
      if (f2 == null) {
        return -1;
      }

      long f1size = 0, f2size = 0;
      if (!f1.isDirectory() && !f2.isDirectory()) {
        f1size = f1.getLength();
        f2size = f2.getLength();
      }

      if (f1.isDirectory() && f2.isDirectory()) {
        f1size = FileUtils.dirSize(f1);
        f2size = FileUtils.dirSize(f2);
      }

      return Long.compare(f1size, f2size);
    }
  }
}
