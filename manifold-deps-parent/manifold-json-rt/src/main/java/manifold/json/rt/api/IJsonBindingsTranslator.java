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
 * A service interface for translating JSON bindings to a data format such as JSON, XML, YAML, and CSV.
 */
public interface IJsonBindingsTranslator
{
  LocklessLazyVar<Set<IJsonBindingsTranslator>> CODECS =
    LocklessLazyVar.make( () -> {
      Set<IJsonBindingsTranslator> registered = new HashSet<>();
      ServiceUtil.loadRegisteredServices( registered, IJsonBindingsTranslator.class, IJsonBindingsTranslator.class.getClassLoader() );
      return registered;
    } );

  static IJsonBindingsTranslator get( String name )
  {
    return IJsonBindingsTranslator.CODECS.get().stream()
      .filter( e -> e.getName().equals( name ) )
      .findFirst().orElseThrow( () -> new RuntimeException( "Missing JSON bindings encoder for : " + name ) );
  }

  /**
   * @return An acronym or abbreviated name for the encoded format, such as JSON or XML.
   */
  String getName();

  String fromBindings( Object bindingsValue );
  void fromBindings( Object bindingsValue, StringBuilder target );
  void fromBindings( Object bindingsValue, String name, StringBuilder target, int indent );

  Object toBindings( String translation );
  Object toBindings( String translation, boolean withTokens );
  Object toBindings( String translation, boolean withBigNumbers, boolean withTokens );
}
