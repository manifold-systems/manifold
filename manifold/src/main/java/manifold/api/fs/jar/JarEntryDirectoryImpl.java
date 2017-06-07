/*
 * Copyright 2014 Guidewire Software, Inc.
 */

package manifold.api.fs.jar;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import manifold.api.fs.IDirectory;
import manifold.api.fs.IDirectoryUtil;
import manifold.api.fs.IFile;
import manifold.api.fs.IResource;

public class JarEntryDirectoryImpl extends JarEntryResourceImpl implements IJarFileDirectory {

  private Map<String, IResource> _resources = new HashMap<String, IResource>();
  private List<IDirectory> _childDirs = new ArrayList<IDirectory>();
  private List<IFile> _childFiles = new ArrayList<IFile>();

  public JarEntryDirectoryImpl(String name, IJarFileDirectory parent, JarFileDirectoryImpl jarFile) {
    super(name, parent, jarFile);
  }

  @Override
  public JarEntryDirectoryImpl getOrCreateDirectory(String relativeName) {
    JarEntryDirectoryImpl result = (JarEntryDirectoryImpl) _resources.get(relativeName);
    if (result == null) {
      result = new JarEntryDirectoryImpl(relativeName, this, _jarFile);
      _resources.put(relativeName, result);
      _childDirs.add(result);
    }
    return result;
  }

  @Override
  public JarEntryFileImpl getOrCreateFile(String relativeName) {
    JarEntryFileImpl result = (JarEntryFileImpl) _resources.get(relativeName);
    if (result == null) {
      result = new JarEntryFileImpl(relativeName, this, _jarFile);
      _resources.put(relativeName, result);
      _childFiles.add(result);
    }
    return result;
  }

  @Override
  public IDirectory dir(String relativePath) {
    return IDirectoryUtil.dir( this, relativePath);
  }

  @Override
  public IFile file(String path) {
    return IDirectoryUtil.file(this, path);
  }

  @Override
  public boolean mkdir() throws IOException {
    throw new UnsupportedOperationException();
  }

  @Override
  public List<? extends IDirectory> listDirs() {
    List<IDirectory> results = new ArrayList<IDirectory>();
    for (IDirectory child : _childDirs) {
      if (child.exists()) {
        results.add(child);
      }
    }
    return results;
  }

  @Override
  public List<? extends IFile> listFiles() {
    List<IFile> results = new ArrayList<IFile>();
    for (IFile child : _childFiles) {
      if (child.exists()) {
        results.add(child);
      }
    }
    return results;
  }

  @Override
  public String relativePath(IResource resource) {
    return IDirectoryUtil.relativePath(this, resource);
  }

  @Override
  public void clearCaches() {
    // Do nothing
  }

  @Override
  public boolean hasChildFile(String path) {
    IFile childFile = file(path);
    return childFile != null && childFile.exists();
  }

  @Override
  public boolean isAdditional() {
    return false;
  }
}
