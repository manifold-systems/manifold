package manifold.api.fs;

/*
 * Copyright 2014 Guidewire Software, Inc.
 */

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.util.List;


public abstract class DelegateDirectory implements IDirectory {

  private final IDirectory _delegate;

  public DelegateDirectory( IDirectory delegate ) {
    _delegate = delegate;
  }

  public IDirectory getDelegate() {
    return _delegate;
  }

  @Override
  public IDirectory dir(String relativePath) {
    return _delegate.dir(relativePath);
  }

  @Override
  public IFile file(String path) {
    return _delegate.file(path);
  }

  @Override
  public boolean mkdir() throws IOException {
    return _delegate.mkdir();
  }

  @Override
  public List<? extends IDirectory> listDirs() {
    return _delegate.listDirs();
  }

  @Override
  public List<? extends IFile> listFiles() {
    return _delegate.listFiles();
  }

  @Override
  public String relativePath(IResource resource) {
    return _delegate.relativePath(resource);
  }

  @Override
  public void clearCaches() {
    _delegate.clearCaches();
  }

  @Override
  public IDirectory getParent() {
    return _delegate.getParent();
  }

  @Override
  public String getName() {
    return _delegate.getName();
  }

  @Override
  public boolean exists() {
    return _delegate.exists();
  }

  @Override
  public boolean delete() throws IOException {
    return _delegate.delete();
  }

  @Override
  public URI toURI() {
    return _delegate.toURI();
  }

  @Override
  public ResourcePath getPath() {
    return _delegate.getPath();
  }

  @Override
  public boolean isChildOf(IDirectory dir) {
    return _delegate.isChildOf(dir);
  }

  @Override
  public boolean isDescendantOf(IDirectory dir) {
    return _delegate.isDescendantOf(dir);
  }

  @Override
  public File toJavaFile() {
    return _delegate.toJavaFile();
  }

  @Override
  public boolean isJavaFile() {
    return _delegate.isJavaFile();
  }

  @Override
  public boolean isInJar() {
    return _delegate.isInJar();
  }

  @Override
  public boolean create() {
    return _delegate.create();
  }

  @Override
  public boolean hasChildFile(String path) {
    return _delegate.hasChildFile(path);
  }
}
