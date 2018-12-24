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

package manifold.api.fs;

import java.io.IOException;
import java.util.List;

public interface IDirectory extends IResource
{
  IDirectory dir( String relativePath );

  /**
   * Constucts a file given the path.  If the path is relative path,
   * it will be constructed based on the current directory
   *
   * @param path the path of the file
   *
   * @return The file that is under the directory with the name
   */
  IFile file( String path );

  boolean mkdir() throws IOException;

  List<? extends IDirectory> listDirs();

  List<? extends IFile> listFiles();

  String relativePath( IResource resource );

  void clearCaches();

  /**
   * Returns true if the given path represents a child of this directory that exists.
   * It's essentially equivalent to calling file(path).exists(), but in cases where
   * this directory caches its list of children and the path represents a direct child
   * of this directory, this method can be optimized to avoid file system access by looking
   * in the list of cached children.
   *
   * @param path the path of the file
   *
   * @return true if the path represents a file that exists as a child of this directory
   */
  boolean hasChildFile( String path );

  /**
   * @return true if this is an "additional" path for resources not copied to the target classpath e.g., config bullshit.
   */
  boolean isAdditional();
}
