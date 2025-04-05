package de.grnx.fftlogger.FileHandling;

import java.io.File;
// i just want to copy a file from the app specific dir into a public dir why is this so complicated
public interface FileCopyPermissionCallback {
    void requestFileCopyPermission(File file, String newFileName);
}
