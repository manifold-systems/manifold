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

import java.io.File;
import java.io.IOException;
import java.net.URI;

public interface IResource
{
  /**
   * The file system supporting this resource
   */
  IFileSystem getFileSystem();

  /**
   * Gets this file's our directory's parent directory.
   *
   * @return this resource's parent directory
   */
  IDirectory getParent();

  /**
   * Gets this file's or directory's name.
   *
   * @return this resource's name
   */
  String getName();

  /**
   * Indicates whether this resource exists.
   *
   * @return true if the resource exists
   */
  boolean exists();

  boolean delete() throws IOException;

  // We do not want to support this.
  /*
  String getCanonicalPath();
  */

  URI toURI();

  ResourcePath getPath();

  /**
   * Indicates whether this resource is a direct child of the given directory.
   *
   * @param dir the directory which would be the parent
   *
   * @return true if this is a direct child of the given directory
   */
  boolean isChildOf( IDirectory dir );

  /**
   * Indicates whether this resource is a descendant of the given directory.
   *
   * @param dir the directory which would be the ancestor
   *
   * @return true if this is a descendant of the given directory
   */
  boolean isDescendantOf( IDirectory dir );

  File toJavaFile();

  boolean isJavaFile();

  boolean isInJar();

  boolean create();

}
