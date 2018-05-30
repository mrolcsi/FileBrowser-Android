package com.github.mjdev.libaums.fs;

import java.io.OutputStreamWriter;

/**
 * Created by rmatusinka on 2017.06.01..
 */

@SuppressWarnings("ALL")
public class UsbFileWriter extends OutputStreamWriter {

  public UsbFileWriter(UsbFile file, FileSystem fs) {
    super(UsbFileStreamFactory.createBufferedOutputStream(file, fs));
  }
}
