/*
 * Copyright (c) 2018 - Manifold Systems LLC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package manifold.api.type;

import java.util.HashSet;
import java.util.Set;
import manifold.api.fs.IFile;
import manifold.api.host.IManifoldHost;

/**
 * For use with {@link ResourceFileTypeManifold}.  Models the common use-case where
 * a type is backed by a single resource file e.g.,
 * a <a href="https://en.wikipedia.org/wiki/Comma-separated_values">CSV</a> file.
 */
public abstract class AbstractSingleFileModel implements IModel
{
  final private IManifoldHost _host;
  final private String _fqn;
  final private Set<IFile> _files;

  public AbstractSingleFileModel( IManifoldHost host, String fqn, Set<IFile> files )
  {
    _host = host;
    _fqn = fqn;
    _files = new HashSet<>( files );
  }

  @Override
  public IManifoldHost getHost()
  {
    return _host;
  }

  @Override
  public String getFqn()
  {
    return _fqn;
  }

  @Override
  public Set<IFile> getFiles()
  {
    return _files;
  }

  public IFile getFile()
  {
    return _files.iterator().next();
  }

  @Override
  public void addFile( IFile file )
  {
    // tolerate adding a file even though this is a single file model
    // the idea is to issue a warning during compilation if this model has more than one file
    _files.add( file );
  }

  @Override
  public void removeFile( IFile file )
  {
    // tolerate adding a file even though this is a single file model
    // the idea is to issue a warning during compilation if this model has more than one file
    _files.remove( file );
  }

  @Override
  public void updateFile( IFile file )
  {
    _files.remove( file );
    _files.add( file );
  }
}
