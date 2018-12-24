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

import manifold.api.fs.IFile;
import manifold.api.host.RefreshKind;

/**
 * An abstraction representing a connection between types and files.
 */
public interface IFileConnected
{
  /**
   * @return True if this type manifold handles files having the given {@code fileExtension}
   */
  boolean handlesFileExtension( String fileExtension );

  /**
   * @return True if the type manifold handles the given {@code file}
   */
  boolean handlesFile( IFile file );

  /**
   * Returns ALL type names associated with the given file
   * whether or not the types have been loaded yet.
   * Type loading should NOT be used in the implementation of this method.
   *
   * @param file The file in question
   *
   * @return All known types derived from that file
   */
  String[] getTypesForFile( IFile file );

  /**
   * Notifies that a file has been refreshed.  The implementor should return all
   * types that it knows need to be refreshed based on the given file.
   *
   * @param file The file that was refreshed
   * @param kind @return All known types affected by the file change
   */
  RefreshKind refreshedFile( IFile file, String[] types, RefreshKind kind );
}
