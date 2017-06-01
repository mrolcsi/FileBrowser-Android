package com.github.mjdev.libaums.fs;

import java.io.InputStreamReader;

/**
 * Created by rmatusinka on 2017.06.01..
 */

public class UsbFileReader extends InputStreamReader {

  public UsbFileReader(UsbFile file, FileSystem fs) {
    super(UsbFileStreamFactory.createBufferedInputStream(file, fs));
  }
}
