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

import manifold.api.fs.IFile;
import manifold.preprocessor.api.SymbolProvider;
import manifold.rt.api.util.ServiceUtil;
import manifold.util.concurrent.ConcurrentWeakHashMap;

import java.util.*;

public class ServiceDefinitions extends Definitions
{
  public static final Map<ClassLoader, Set<SymbolProvider>> REGISTERED_SYMBOL_PROVIDERS = new ConcurrentWeakHashMap<>();

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
    Set<SymbolProvider> providers = getSymbolProviders( getSourceFile() );
    return providers.stream().anyMatch( p -> p.isDefined( getRootDefinitions(), getSourceFile(), def ) );
  }

  @Override
  public String getValue( String def )
  {
    Set<SymbolProvider> providers = getSymbolProviders( getSourceFile() );
    return providers.stream()
      .filter( p -> p.isDefined( getRootDefinitions(), getSourceFile(), def ) )
      .map( p -> p.getValue( getRootDefinitions(), getSourceFile(), def ) )
      .map( v -> v == null ? "" : v )
      .findFirst()
      .orElse( null );
  }

  public static Set<SymbolProvider> getSymbolProviders( IFile sourceFile )
  {
    ClassLoader currentLoader = Thread.currentThread().getContextClassLoader();
    ClassLoader cl = null;
    if( sourceFile != null )
    {
      cl = sourceFile.getFileSystem().getHost().getClassLoaderForFile( sourceFile );
      if( cl != null )
      {
        // a non-null cl here is an IDE module class loader
        Thread.currentThread().setContextClassLoader( cl );
      }
    }
    else
    {
      cl = currentLoader;
    }

    try
    {
      // cl can't be null as a key to a ConcurrentHashMap, the non-null value we use here is inconsequential so long as it's stable
      cl = cl == null ? ServiceDefinitions.class.getClassLoader() : cl;

      return REGISTERED_SYMBOL_PROVIDERS.computeIfAbsent( cl, __ -> {
        Set<SymbolProvider> registered = new HashSet<>();
        ServiceUtil.loadRegisteredServices( registered, SymbolProvider.class, ServiceDefinitions.class.getClassLoader() );
        return registered;
      } );
    }
    finally
    {

      Thread.currentThread().setContextClassLoader( currentLoader );
    }
  }
}
