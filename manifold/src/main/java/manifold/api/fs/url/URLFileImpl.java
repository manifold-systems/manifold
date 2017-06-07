/*
 * Copyright 2014 Guidewire Software, Inc.
 */

package manifold.api.fs.url;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import manifold.api.fs.IDirectory;
import manifold.api.fs.IFile;
import manifold.api.fs.ResourcePath;

/**
 * HTTP-backed file. The only supported operation is to open stream.
 */
public class URLFileImpl implements IFile
{
  private URL _url;

  public URLFileImpl(URL url) {
    _url = url;
  }

  @Override
  public InputStream openInputStream() throws IOException {
    return _url.openStream();
  }

  @Override
  public OutputStream openOutputStream() throws IOException {
    throw new UnsupportedOperationException();
  }

  @Override
  public OutputStream openOutputStreamForAppend() throws IOException {
    throw new UnsupportedOperationException();
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
  public IDirectory getParent() {
    throw new UnsupportedOperationException();
  }

  @Override
  public String getName() {
    return getPath().getName();
  }

  @Override
  public boolean exists() {
    throw new UnsupportedOperationException();
  }

  @Override
  public boolean delete() throws IOException {
    throw new UnsupportedOperationException();
  }

  @Override
  public URI toURI() {
    try {
      return _url.toURI();
    } catch (URISyntaxException e) {
      throw new RuntimeException("Cannot convert to URI", e);
    }
  }

  @Override
  public ResourcePath getPath() {
    return ResourcePath.parse(_url.getPath());
  }

  @Override
  public boolean isChildOf(IDirectory dir) {
    throw new UnsupportedOperationException();
  }

  @Override
  public boolean isDescendantOf(IDirectory dir) {
    throw new UnsupportedOperationException();
  }

  @Override
  public File toJavaFile() {
    throw new UnsupportedOperationException();
  }

  @Override
  public boolean isJavaFile() {
    return false;
  }

  @Override
  public boolean isInJar() {
    return false;
  }

  @Override
  public boolean create() {
    throw new UnsupportedOperationException();
  }
}
