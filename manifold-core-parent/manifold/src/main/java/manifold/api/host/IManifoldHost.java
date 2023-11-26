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

package manifold.api.host;

import manifold.api.fs.IFile;
import manifold.api.fs.IFileFragment;
import manifold.api.fs.IFileSystem;
import manifold.api.service.IService;
import manifold.internal.javac.JavaParser;

/**
 * Implementors of this interface drive Manifold in a custom way based
 * on the environment employing Manifold's services.  These include:
 * <ul>
 * <li>Runtime class loaders - core Manifold</li>
 * <li>Compilers - the Manifold javac plugin</li>
 * <li>IDEs - the Manifold IntelliJ IDEA plugin</li>
 * </ul>
 */
public interface IManifoldHost extends IService
{
  ClassLoader getActualClassLoader();

  ClassLoader getClassLoaderForFile( IFile file );

  IModule getSingleModule();

  boolean isPathIgnored( String path );

  void addTypeSystemListenerAsWeakRef( Object ctx, ITypeSystemListener listener );

  void createdType( IFileFragment file, String[] types );

  IFileSystem getFileSystem();

  JavaParser getJavaParser();

  default String getArrayTypeName()
  {
    return null;
  }
}
