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

package manifold.internal.runtime.protocols;

import java.io.IOException;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLStreamHandler;
import java.util.Arrays;
import java.util.concurrent.ConcurrentHashMap;

/**
 */
public class Handler extends URLStreamHandler
{
  public static final ConcurrentHashMap<URL, URL> _visited = new ConcurrentHashMap<URL, URL>();

  public static final Handler INSTANCE = new Handler();

  static
  {
    // Preload the Url Connection classes to prevent LinkageErrors during demo load
    Arrays.asList( ManClassesUrlConnection.class,
                   ManClassesUrlConnection.LazyByteArrayInputStream.class );
  }

  @Override
  protected URLConnection openConnection( URL u ) throws IOException
  {
    if( _visited.containsKey( u ) )
    {
      //## todo: we should try hard so that this never happens -- the type sys tries to resolve inner classes,
      // which come back around here and always fail
      return null;
    }
    _visited.put( u, u );
    try
    {
      ManClassesUrlConnection connection = new ManClassesUrlConnection( u );
      return connection.isValid() ? connection : null;
    }
    finally
    {
      _visited.remove( u );
    }
  }

  @Override
  protected int hashCode( URL u )
  {
    return (u.getProtocol() + u.getHost() + u.getPath()).hashCode();
  }

  @Override
  protected boolean equals( URL u1, URL u2 )
  {
    if( u1 == u2 )
    {
      return true;
    }
    if( u1 == null || u2 == null )
    {
      return false;
    }
    return (u1.getProtocol() + u1.getHost() + u1.getPath()).equals( u2.getProtocol() + u2.getHost() + u2.getPath() );
  }
}