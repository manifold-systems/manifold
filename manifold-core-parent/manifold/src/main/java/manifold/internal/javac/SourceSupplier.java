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

package manifold.internal.javac;

import java.util.Set;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import manifold.api.fs.IFile;
import manifold.api.type.ContributorKind;
import manifold.api.type.ITypeManifold;

public class SourceSupplier
{
  private String _fqn;
  private Supplier<String> _supplier;
  private final Set<ITypeManifold> _sps;

  /**
   * @param fqn      Qualified name of type
   * @param tms      The set of type manifolds responsible for producing the source.  An
   *                 empty or null set implies no type manifolds are involved.
   * @param supplier Supplier of the source code.
   */
  public SourceSupplier( String fqn, Set<ITypeManifold> tms, Supplier<String> supplier )
  {
    _fqn = fqn;
    _supplier = supplier;
    _sps = tms;
  }

  /**
   * Produce the source.
   */
  public String getSource()
  {
    return _supplier.get();
  }

  /**
   * Is this source supplier the primary or core producer of the source?  As opposed to a
   * supplementary or partial producer.
   */
  public boolean isPrimary()
  {
    return _sps == null || _sps.isEmpty() ||
           _sps.stream().anyMatch( e -> e.getContributorKind() == ContributorKind.Primary ||
                                        e.getContributorKind() == ContributorKind.Partial );
  }

  public boolean isSelfCompile( String fqn )
  {
    return _sps == null || _sps.isEmpty() ||
           _sps.stream().anyMatch( tm -> tm.isSelfCompile( fqn ) );
  }

  public void parse( String fqn )
  {
    _sps.stream()
      .filter( tm -> tm.isSelfCompile( fqn ) )
      .findFirst().orElseThrow( IllegalStateException::new )
      .parse( fqn );
  }

  public byte[] compile( String fqn )
  {
    return _sps.stream()
      .filter( tm -> tm.isSelfCompile( fqn ) )
      .findFirst().orElseThrow( IllegalStateException::new )
      .compile( fqn );
  }

  public Set<IFile> getResourceFiles()
  {
    return _sps.stream().flatMap( tm -> tm.findFilesForType( _fqn ).stream() ).collect( Collectors.toSet() );
  }
}
