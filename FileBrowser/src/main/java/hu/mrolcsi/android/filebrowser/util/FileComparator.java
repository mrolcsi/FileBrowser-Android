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

import java.io.File;
import java.util.Comparator;

/**
 * Created with IntelliJ IDEA.
 * User: Matusinka Roland
 * Date: 2015.08.06.
 * Time: 15:30
 */

public abstract class FileComparator {

  static class ByFileName implements Comparator<File> {

    @Override
    public int compare(File f1, File f2) {
      if (f1.isDirectory() && f2.isFile()) {
        return 1;
      }
      if (f1.isFile() && f2.isDirectory()) {
        return -1;
      }
      return f1.getName().compareToIgnoreCase(f2.getName());
    }
  }

  static class ByExtension implements Comparator<File> {

    @Override
    public int compare(File f1, File f2) {
      if (f1.isDirectory() && f2.isDirectory()) {
        return f1.getName().compareToIgnoreCase(f2.getName());
      }
      if (f1.isDirectory() && f2.isFile()) {
        return -1;
      }
      if (f1.isFile() && f2.isDirectory()) {
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

  static class ByDate implements Comparator<File> {

    @Override
    public int compare(File f1, File f2) {
      long f1mod = f1.lastModified();
      long f2mod = f2.lastModified();
      return Long.compare(f1mod, f2mod);
    }
  }

  static class BySize implements Comparator<File> {

    @Override
    public int compare(File f1, File f2) {
      if (f1 == null) {
        return 1;
      }
      if (f2 == null) {
        return -1;
      }

      long f1size = 0, f2size = 0;
      if (f1.isFile() && f2.isFile()) {
        f1size = f1.length();
        f2size = f2.length();
      }
      if (f1.isDirectory() && f2.isDirectory()) {
        f1size = FileUtils.dirSize(f1);
        f2size = FileUtils.dirSize(f2);
      }
      return Long.compare(f1size, f2size);
    }
  }
}
