/*
 * Copyright 2014 Guidewire Software, Inc.
 */

package manifold.api.fs.physical;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import manifold.api.fs.IFile;
import manifold.api.fs.ResourcePath;

public class PhysicalFileImpl extends PhysicalResourceImpl implements IFile
{
  public PhysicalFileImpl( ResourcePath path, IPhysicalFileSystem backingFileSystem) {
    super(path, backingFileSystem);
  }

  @Override
  public InputStream openInputStream() throws IOException {
    return new FileInputStream(toJavaFile());
  }

  @Override
  public OutputStream openOutputStream() throws IOException {
    return new FileOutputStream(toJavaFile());
  }

  @Override
  public OutputStream openOutputStreamForAppend() throws IOException {
    return new FileOutputStream(toJavaFile(), true);
  }

  @Override
  public String getExtension() {
    int lastDot = getName().lastIndexOf(".");
    if (lastDot != -1) {
      return getName().substring(lastDot + 1);
    } else {
      return "";
    }
  }

  @Override
  public String getBaseName() {
    int lastDot = getName().lastIndexOf(".");
    if (lastDot != -1) {
      return getName().substring(0, lastDot);
    } else {
      return getName();
    }
  }

  @Override
  public boolean create() {
    try {
      return toJavaFile().createNewFile();
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }
}
