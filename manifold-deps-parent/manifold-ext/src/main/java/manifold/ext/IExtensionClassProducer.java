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

package manifold.ext;

import java.util.Set;
import manifold.api.fs.IFile;
import manifold.api.type.ITypeManifold;

/**
 * A type manifold implements this interface if it produces extension classes.  Normally extension classes
 * are provided directly as source files.  However it may be necessary for a type manifold to produce them
 * dynamically based on information provided elsewhere.  This interface serves as a means for the
 * {@link ExtensionManifold} to ask this type manifold what types it extends and so forth.
 */
public interface IExtensionClassProducer extends ITypeManifold
{
  /**
   * Does this type manifold produce extension class[es] for {@code fqn}?
   *
   * @param fqn The fully qualified name of a type for which this manifold may produce an extension class
   * @return True if this manifold produces extension class[es] for {@code fqn}
   */
  boolean isExtendedType( String fqn );

  /**
   * The subset of extension classes this type manifold produces that extend {@code fqn }
   *
   * @param fqn The fully qualified name of a potentially extended type
   * @return The subset of extension classes this type manifold produces that extend {@code fqn}
   */
  Set<String> getExtensionClasses( String fqn );

  /**
   * The set of extension classes this type manifold produces.
   */
  Set<String> getExtendedTypes();

  /**
   * The set of classes extended via {$code file}.
   *
   * @param file A resource file associated with this class producer
   */
  Set<String> getExtendedTypesForFile( IFile file );
}
