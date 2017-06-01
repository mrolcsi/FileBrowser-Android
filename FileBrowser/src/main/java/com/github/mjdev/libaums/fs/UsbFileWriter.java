package com.github.mjdev.libaums.fs;

import java.io.OutputStreamWriter;

/**
 * Created by rmatusinka on 2017.06.01..
 */

public class UsbFileWriter extends OutputStreamWriter {

  public UsbFileWriter(UsbFile file, FileSystem fs) {
    super(UsbFileStreamFactory.createBufferedOutputStream(file, fs));
  }
}
