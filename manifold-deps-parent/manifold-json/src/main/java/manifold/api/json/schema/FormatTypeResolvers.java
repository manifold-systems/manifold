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

package manifold.api.json.schema;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.ServiceConfigurationError;
import java.util.ServiceLoader;
import manifold.util.concurrent.LocklessLazyVar;

public class FormatTypeResolvers
{
  private static final LocklessLazyVar<List<IJsonFormatTypeResolver>> _formatResolvers =
    LocklessLazyVar.make( FormatTypeResolvers::loadFormatTypeResolvers );

  public static List<IJsonFormatTypeResolver> get()
  {
    return _formatResolvers.get();
  }

  private static List<IJsonFormatTypeResolver> loadFormatTypeResolvers()
  {
    try
    {
      List<IJsonFormatTypeResolver> resolvers = loadFormatTypeResolvers( FormatTypeResolvers.class.getClassLoader() );
      resolvers.addAll( loadFormatTypeResolvers( Thread.currentThread().getContextClassLoader() ) );
      return resolvers;
    }
    catch( ServiceConfigurationError e )
    {
      e.printStackTrace();
    }
    return Collections.emptyList();
  }

  private static List<IJsonFormatTypeResolver> loadFormatTypeResolvers( ClassLoader cl )
  {
    List<IJsonFormatTypeResolver> resolvers = new ArrayList<>();
    ServiceLoader<IJsonFormatTypeResolver> loader = ServiceLoader.load( IJsonFormatTypeResolver.class, cl );
    for( IJsonFormatTypeResolver resolver: loader )
    {
      resolvers.add( resolver );
    }
    return resolvers;
  }
}
