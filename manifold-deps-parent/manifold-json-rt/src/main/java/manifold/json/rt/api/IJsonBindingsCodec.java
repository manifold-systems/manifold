/*
 * Copyright (c) 2020 - Manifold Systems LLC
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

package manifold.json.rt.api;

import manifold.rt.api.util.ServiceUtil;
import manifold.util.concurrent.LocklessLazyVar;

import java.util.HashSet;
import java.util.Set;

/**
 * A service interface for encoding/decoding JSON bindings to a variety of data formats such as
 * JSON, XML, YAML, and CSV.
 */
public interface IJsonBindingsCodec
{
  LocklessLazyVar<Set<IJsonBindingsCodec>> CODECS =
    LocklessLazyVar.make( () -> {
      Set<IJsonBindingsCodec> registered = new HashSet<>();
      ServiceUtil.loadRegisteredServices( registered, IJsonBindingsCodec.class, IJsonBindingsCodec.class.getClassLoader() );
      return registered;
    } );

  static IJsonBindingsCodec get( String name )
  {
    return IJsonBindingsCodec.CODECS.get().stream()
      .filter( e -> e.getName().equals( name ) )
      .findFirst().orElseThrow( () -> new RuntimeException( "Missing JSON bindings encoder for : " + name ) );
  }

  /**
   * @return An acronym or abbreviated name for the encoded format, such as JSON or XML.
   */
  String getName();

  String encode( Object jsonValue );
  void encode( Object jsonValue, StringBuilder target );
  void encode( Object jsonValue, String name, StringBuilder target, int indent );

  Object decode( String encoded );
  Object decode( String encoded, boolean withTokens );
  Object decode( String encoded, boolean withBigNumbers, boolean withTokens );

  default StringBuilder appendValue( StringBuilder sb, Object comp )
  {
    throw new UnsupportedOperationException();
  }
}
