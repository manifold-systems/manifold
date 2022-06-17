/*
 * Copyright (c) 2022 - Manifold Systems LLC
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

package manifold.tuple;

import manifold.api.util.fingerprint.Fingerprint;
import manifold.internal.javac.ITupleTypeProvider;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class TupleTypeProvider implements ITupleTypeProvider
{
  static String BASE_NAME = "manifold_tuple_";

  private final Map<String, Map<String, String>> _nameToFields;
  private final Map<String, String> _fingerprints;

  public TupleTypeProvider()
  {
    _nameToFields = new LinkedHashMap<>();
    _fingerprints = new ConcurrentHashMap<>();
  }

  @Override
  public String makeType( String pkg, Map<String, String> fieldNameToTypeName )
  {
    Map<String, String> alphaOrder = new LinkedHashMap<>();
    fieldNameToTypeName.keySet().stream()
      .sorted( Comparator.naturalOrder() )
      .forEach( e -> alphaOrder.put( e, fieldNameToTypeName.get( e ) ) );
    String fqn = makeName( pkg, alphaOrder );
    _nameToFields.putIfAbsent( fqn, alphaOrder );
    return fqn;
  }

  public Set<String> getTypes()
  {
    return _nameToFields.keySet();
  }

  @Override
  public Map<String, String> getFields( String fqn )
  {
    return _nameToFields.get( fqn );
  }

  private String makeName( String pkg, Map<String, String> alphaOrder )
  {
    StringBuilder sb = new StringBuilder();
    alphaOrder.forEach( (a, b) -> sb.append( a ).append( b ) );
    String fingerprint = _fingerprints.computeIfAbsent( sb.toString(), str -> {
      long rawFingerprint = new Fingerprint( str ).getRawFingerprint();
      String fp = String.valueOf( rawFingerprint );
      return fp.charAt( 0 ) == '-' ? '_' + fp.substring( 1 ) : fp;
    } );
    return pkg + '.' + BASE_NAME + fingerprint;
  }
}
