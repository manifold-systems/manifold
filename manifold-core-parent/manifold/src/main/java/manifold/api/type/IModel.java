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

import java.util.Set;
import javax.tools.DiagnosticListener;
import manifold.api.fs.IFile;
import manifold.api.host.IManifoldHost;

/**
 * For use with {@link ResourceFileTypeManifold}. Implementors of {@link IModel} store and manage
 * state necessary to generate source code in the context of {@link ResourceFileTypeManifold#contribute(javax.tools.JavaFileManager.Location, String, String, IModel, DiagnosticListener)}
 */
public interface IModel
{
  /**
   * @return The Manifold host within which this model operates
   */
  IManifoldHost getHost();

  /**
   * @return The fully qualified type name to which code will be contributed
   */
  String getFqn();

  /**
   * @return The resource file[s] from which information is gathered to contribute source
   */
  Set<IFile> getFiles();

  /**
   * Add {@code file} to the set of files this model uses.  The addition of a new flie
   */
  void addFile( IFile file );

  /**
   * Remove {@code file} from the set of files this model uses
   */
  void removeFile( IFile file );

  /**
   * Updates {@code file} in the set of files this model uses
   */
  void updateFile( IFile file );

  /**
   * @return True if the model is processing or otherwise in an unsettled state
   */
  default boolean isProcessing()
  {
    return false;
  }
}
