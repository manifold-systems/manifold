/*
 * Copyright (c) 2021 - Manifold Systems LLC
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

package manifold.preprocessor.definitions;

import manifold.preprocessor.api.SymbolProvider;
import manifold.rt.api.util.ServiceUtil;
import manifold.util.concurrent.LocklessLazyVar;

import java.util.*;

public class ServiceDefinitions extends Definitions
{
  public static final LocklessLazyVar<Set<SymbolProvider>> REGISTERED_SYMBOL_PROVIDERS =
    LocklessLazyVar.make( () -> {
      Set<SymbolProvider> registered = new HashSet<>();
      ServiceUtil.loadRegisteredServices( registered, SymbolProvider.class, ServiceDefinitions.class.getClassLoader() );
      return registered;
    } );

  private final Definitions _rootDefinitions;

  public ServiceDefinitions( Definitions rootDefinitions )
  {
    super( rootDefinitions.getSourceFile() );
    _rootDefinitions = rootDefinitions;
  }

  public Definitions getRootDefinitions()
  {
    return _rootDefinitions;
  }

  @Override
  protected Definitions loadParentDefinitions()
  {
    return null;
  }

  @Override
  public boolean isDefined( String def )
  {
    Set<SymbolProvider> providers = REGISTERED_SYMBOL_PROVIDERS.get();
    return providers.stream().anyMatch( p -> p.isDefined( getRootDefinitions(), getSourceFile(), def ) );
  }

  @Override
  public String getValue( String def )
  {
    Set<SymbolProvider> providers = REGISTERED_SYMBOL_PROVIDERS.get();
    return providers.stream()
      .filter( p -> p.isDefined( getRootDefinitions(), getSourceFile(), def ) )
      .map( p -> p.getValue( getRootDefinitions(), getSourceFile(), def ) )
      .map( v -> v == null ? "" : v )
      .findFirst()
      .orElse( null );
  }
}
