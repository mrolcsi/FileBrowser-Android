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

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import com.github.mjdev.libaums.fs.UsbFile;
import java.nio.ByteBuffer;

/**
 * Created by Matusinka Roland on 2016.04.14..
 */
public abstract class DummyFile implements UsbFile {

  @Nullable
  @Override
  public UsbFile search(@NonNull String path) {
    return null;
  }

  @Override
  public boolean isDirectory() {
    return false;
  }

  @Override
  public void setName(String newName) {
  }

  @Override
  public long createdAt() {
    return 0;
  }

  @Override
  public long lastModified() {
    return 0;
  }

  @Override
  public long lastAccessed() {
    return 0;
  }

  @Override
  public String[] list() {
    return new String[0];
  }

  @Override
  public UsbFile[] listFiles() {
    return new UsbFile[0];
  }

  @Override
  public long getLength() {
    return -1;
  }

  @Override
  public void setLength(long newLength) {
  }

  @Override
  public void read(long offset, ByteBuffer destination) {
  }

  @Override
  public void write(long offset, ByteBuffer source) {
  }

  @Override
  public void flush() {
  }

  @Override
  public void close() {
  }

  @Override
  public UsbFile createDirectory(String name) {
    return null;
  }

  @Override
  public UsbFile createFile(String name) {
    return null;
  }

  @Override
  public void moveTo(UsbFile destination) {
  }

  @Override
  public void delete() {
  }

  @Override
  public boolean isRoot() {
    return false;
  }
}
