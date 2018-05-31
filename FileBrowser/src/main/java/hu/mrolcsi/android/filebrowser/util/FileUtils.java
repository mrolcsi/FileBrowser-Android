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

package hu.mrolcsi.android.filebrowser.util;

import com.github.mjdev.libaums.fs.UsbFile;
import java.io.File;
import java.io.IOException;
import java.util.Locale;
import java.util.Stack;

/**
 * Created by Matusinka Roland on 2016.04.20..
 */
public abstract class FileUtils {

  private static final String[] RESERVED_CHARS = {"|", "\\", "?", "*", "<", "\"", ":", ">"};

  public static String getExtension(String fileName) {
    String ext = null;
    int i = fileName.lastIndexOf('.');
    if (i > 0 && i < fileName.length() - 1) {
      ext = fileName.substring(i + 1).toLowerCase(Locale.getDefault());
    }
    return ext;
  }

  public static String getNameWithoutExtension(String fileName) {
    int i = fileName.lastIndexOf('.');

    try {
      return fileName.substring(0, i);
    } catch (IndexOutOfBoundsException e) {
      return fileName;
    }
  }

  public static boolean isFilenameValid(String filename) {
    for (String reservedChar : RESERVED_CHARS) {
      if (filename.contains(reservedChar)) {
        return false;
      }
    }
    return true;
  }

  public static String getFriendlySize(long rawSize) {
    if (rawSize > 1000000000) {
      return String.format(Locale.getDefault(), "%.2f GB", (float) rawSize / 1000000000);
    } else if (rawSize > 1000000) {
      return String.format(Locale.getDefault(), "%.2f MB", (float) rawSize / 1000000);
    } else if (rawSize > 1000) {
      return String.format(Locale.getDefault(), "%.2f kB", (float) rawSize / 1000);
    } else {
      return String.format(Locale.getDefault(), "%d B", rawSize);
    }
  }

  /**
   * Rekurzívan megadja egy mappa méretét byte-ban.
   *
   * @param dir Kinduló mappa.
   * @return A mappa mérete byte-ban.
   * @see <a href="http://stackoverflow.com/questions/4040912/how-can-i-get-the-size-of-a-folder-on-sd-card-in-android">forrás</a>
   */
  static long dirSize(File dir) {

    if (Cache.getInstance().sizeCache.containsKey(dir.getAbsolutePath())) {
      return Cache.getInstance().sizeCache.get(dir.getAbsolutePath());
    }

    long result = 0;

    Stack<File> dirList = new Stack<>();
    dirList.clear();

    dirList.push(dir);

    while (!dirList.isEmpty()) {
      File dirCurrent = dirList.pop();

      File[] fileList = dirCurrent.listFiles();

      if (fileList != null) {
        for (File aFileList : fileList) {

          if (aFileList.isDirectory()) {
            dirList.push(aFileList);
          } else {
            result += aFileList.length();
          }
        }
      } else {
        result = 0;
      }
    }

    Cache.getInstance().sizeCache.put(dir.getAbsolutePath(), result);

    return result;
  }

  public static long dirSize(UsbFile dir) {
    if (Cache.getInstance().sizeCache.containsKey(dir.getName())) {
      return Cache.getInstance().sizeCache.get(dir.getName());
    }

    long result = 0;

    Stack<UsbFile> dirList = new Stack<>();
    dirList.clear();

    dirList.push(dir);

    while (!dirList.isEmpty()) {
      UsbFile dirCurrent = dirList.pop();

      try {
        UsbFile[] fileList = dirCurrent.listFiles();

        if (fileList != null) {
          for (UsbFile file : fileList) {

            if (file.isDirectory()) {
              dirList.push(file);
            } else {
              result += file.getLength();
            }
          }
        } else {
          result = 0;
        }
      } catch (IOException e) {
        //skip
      }
    }

    Cache.getInstance().sizeCache.put(dir.getName(), result);

    return result;
  }

  public static String getAbsolutePath(UsbFile file) {
    UsbFile child = file;

    StringBuilder sb = new StringBuilder();

    while (child.getParent() != null) {
      sb.insert(0, child.getName());
      sb.insert(0, "/");
      child = child.getParent();
    }

    return sb.toString();
  }
}
