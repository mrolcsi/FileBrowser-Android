package hu.mrolcsi.android.filebrowser.usb;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import com.github.mjdev.libaums.fs.UsbFile;
import java.io.IOException;
import java.nio.ByteBuffer;

/**
 * Created by Matusinka Roland on 2016.04.14..
 */
public abstract class DummyFile implements UsbFile {

  @Nullable
  @Override
  public UsbFile search(@NonNull String path) throws IOException {
    return null;
  }

  @Override
  public boolean isDirectory() {
    return false;
  }

  @Override
  public void setName(String newName) throws IOException {
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
  public String[] list() throws IOException {
    return new String[0];
  }

  @Override
  public UsbFile[] listFiles() throws IOException {
    return new UsbFile[0];
  }

  @Override
  public long getLength() {
    return -1;
  }

  @Override
  public void setLength(long newLength) throws IOException {
  }

  @Override
  public void read(long offset, ByteBuffer destination) throws IOException {
  }

  @Override
  public void write(long offset, ByteBuffer source) throws IOException {
  }

  @Override
  public void flush() throws IOException {
  }

  @Override
  public void close() throws IOException {
  }

  @Override
  public UsbFile createDirectory(String name) throws IOException {
    return null;
  }

  @Override
  public UsbFile createFile(String name) throws IOException {
    return null;
  }

  @Override
  public void moveTo(UsbFile destination) throws IOException {
  }

  @Override
  public void delete() throws IOException {
  }

  @Override
  public boolean isRoot() {
    return false;
  }
}
