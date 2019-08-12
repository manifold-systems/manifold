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

package manifold.internal.runtime;

import java.io.IOException;
import java.io.InputStream;
//import java.lang.module.ModuleReader;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.ByteBuffer;
import java.util.Optional;
import java.util.stream.Stream;
//import jdk.internal.loader.Resource;
//import jdk.internal.loader.URLClassPath;
import manifold.util.ReflectUtil;
import manifold.api.util.StreamUtil;

/**
 * This class facilitates dynamic class loading via Type Manifolds in a Java 9 *modular* configuration.
 * Note non-modular projects use Java 8, Java 9 with -source = 8, or Java 9 with no module-info.java;
 * only a project using Java 9+ with module-info.java requires this class.
 * <p/>
 * Basically ManModuleReader exploits Java 9's one-module-per-package rule...
 * Since Java 9 maps each package name uniquely to a single module, a class of a given package must
 * either load from that module or not load at all.  ManModuleReader contributes to this loading
 * by first delegating to the normal ModuleReader and, if the class fails to load, let Manifold
 * attempt to load it.
 * <p/>
 * Since Manifold is compiled exclusively with Java 8, this class implements ModuleReader structurally
 * via proxy.
 */
@SuppressWarnings({"unused", "unchecked"})
public class ManModuleReader //implements ModuleReader
{
  private final Object /*ModuleReader*/_delegate;
  private final Object /*URLClassPath*/ _ucp;

  public ManModuleReader( Object /*ModuleReader*/ delegate, Object /*URLClassPath*/ ucp )
  {
    _delegate = delegate;
    _ucp = ucp;
  }

  //@Override
  public Optional<URI> find( String name ) throws IOException
  {
    Optional<URI> uri = (Optional<URI>)ReflectUtil.method( _delegate, "find", String.class ).invoke( name );
    if( !uri.isPresent() )
    {
      URL resource = (URL)ReflectUtil.method( _ucp, "findResource", String.class, boolean.class ).invoke( name, false );
      if( resource != null )
      {
        try
        {
          uri = Optional.of( resource.toURI() );
        }
        catch( URISyntaxException e )
        {
          throw new IOException( e );
        }
      }
    }
    return uri;
  }

  //@Override
  public Optional<InputStream> open( String name )
  {
    Optional<InputStream> input = (Optional<InputStream>)ReflectUtil.method( _delegate, "open", String.class ).invoke( name );
    if( !input.isPresent() )
    {
      Object/*Resource*/ resource = ReflectUtil.method( _ucp, "getResource", String.class, boolean.class ).invoke( name, false );
      if( resource != null )
      {
        input = Optional.of( (InputStream)ReflectUtil.method( resource, "getInputStream" ).invoke() );
      }
    }
    return input;
  }

  //@Override
  public Optional<ByteBuffer> read( String name ) throws IOException
  {
    Optional<ByteBuffer> buffer = (Optional<ByteBuffer>)ReflectUtil.method( _delegate, "read", String.class ).invoke( name );
    if( !buffer.isPresent() )
    {
      Object/*Resource*/ resource = ReflectUtil.method( _ucp, "getResource", String.class, boolean.class ).invoke( name, false );
      if( resource != null )
      {
        ByteBuffer bytes = ByteBuffer.wrap( StreamUtil.getContent( (InputStream)ReflectUtil.method( resource, "getInputStream" ).invoke() ) );
        buffer = Optional.of( bytes );
      }
    }
    return buffer;
  }

  //@Override
  public void release( ByteBuffer bb )
  {
    try
    {
      ReflectUtil.method( _delegate, "release", ByteBuffer.class ).invoke( bb );
    }
    catch( Exception ignore )
    {
    }
  }

  //@Override
  public Stream<String> list()
  {
    return (Stream<String>)ReflectUtil.method( _delegate, "list" ).invoke();
  }

  //@Override
  public void close()
  {
    ReflectUtil.method( _delegate, "close" ).invoke();
  }
}
