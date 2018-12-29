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

package manifold.internal.host;

import java.io.File;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.ServiceConfigurationError;
import java.util.ServiceLoader;
import java.util.stream.Collectors;
import manifold.api.fs.IDirectory;
import manifold.api.host.IRuntimeManifoldHost;
import manifold.internal.runtime.IjPluginIntegration;
import manifold.internal.runtime.UrlClassLoaderWrapper;
import manifold.util.SourcePathUtil;
import manifold.util.concurrent.LockingLazyVar;

/**
 */
public class RuntimeManifoldHost extends SingleModuleManifoldHost implements IRuntimeManifoldHost
{
  private volatile static LockingLazyVar<IRuntimeManifoldHost> HOST =
    LockingLazyVar.make( RuntimeManifoldHost::loadRuntimeManifoldHost );

  private List<File> _classpath;


  public static IRuntimeManifoldHost get()
  {
    return HOST.get();
  }

  public static IRuntimeManifoldHost clear()
  {
    return HOST.clear();
  }

  private static IRuntimeManifoldHost loadRuntimeManifoldHost()
  {
    try
    {
      // Allow for the IRuntimeManifoldHost impl to be customised as a Java Service
      IRuntimeManifoldHost host = loadRuntimeManifoldHost( RuntimeManifoldHost.class.getClassLoader() );
      if( host == null )
      {
        host = loadRuntimeManifoldHost( Thread.currentThread().getContextClassLoader() );
      }
      if( host != null )
      {
        return host;
      }
    }
    catch( ServiceConfigurationError e )
    {
      // report, but do not throw, the exception; let the default host takeover
      e.printStackTrace();
    }

    // default
    return new RuntimeManifoldHost();
  }

  private static IRuntimeManifoldHost loadRuntimeManifoldHost( ClassLoader cl )
  {
    ServiceLoader<IRuntimeManifoldHost> loader = ServiceLoader.load( IRuntimeManifoldHost.class, cl );
    Iterator<IRuntimeManifoldHost> iterator = loader.iterator();
    if( iterator.hasNext() )
    {
      IRuntimeManifoldHost host = iterator.next();
      if( iterator.hasNext() )
      {
        System.out.println( "WARNING: Found multiple Manifold hosts, using first encountered: " + HOST.getClass().getName() );
      }
      return host;
    }
    return null;
  }

  public static void bootstrap()
  {
    get().bootstrap( Collections.emptyList(), Collections.emptyList() );
  }

  @Override
  public boolean isBootstrapped()
  {
    return getSingleModule() != null;
  }

  public void bootstrap( List<File> sourcepath, List<File> classpath )
  {
    if( isBootstrapped() )
    {
      return;
    }

    preBootstrap();
    init( sourcepath, classpath );
  }

  /**
   * Initialize host and its type manifolds. Includes classpath from the host's classloader in addition to provided
   * classpath.
   *
   * @param sourcepath List of paths containing sources/resources
   * @param classpath List of paths comprising the classpath
   *
   * @see #initDirectly(List, List)
   */
  public void init( List<File> sourcepath, List<File> classpath )
  {
    List<File> combined = new ArrayList<>();
    if( classpath != null )
    {
      combined.addAll( classpath );
    }
    combined.addAll( deriveClasspath() );
    initDirectly( sourcepath, combined );
  }

  private List<File> deriveClasspath()
  {
    List<File> ll = new LinkedList<>();
    ClassLoader loader = getActualClassLoader();
    while( loader != null )
    {
      UrlClassLoaderWrapper wrap = UrlClassLoaderWrapper.wrap( loader );
      if( wrap != null )
      {
        for( URL url : wrap.getURLs() )
        {
          try
          {
            File file = new File( url.toURI() );
            if( file.exists() && !ll.contains( file ) )
            {
              ll.add( file );
            }
          }
          catch( Exception e )
          {
            //ignore
          }
        }
      }
      ClassLoader parent = loader.getParent();
      if( parent == null )
      {
        // IntelliJ's PluginClassLoader is special
        parent = IjPluginIntegration.getParent( loader );
      }
      loader = parent;
    }
//    addBootstrapClasses( ll );
    return ll;
  }

  /**
   * Initialize the host and its type loaders using specified sourcepath and classpath.
   *
   * @param sourcepath List of paths containing sources/resources
   * @param classpath List of paths comprising the classpath
   */
  protected void initDirectly( List<File> sourcepath, List<File> classpath )
  {
    classpath = new ArrayList<>( classpath );
    removeDups( classpath );

    if( classpath.equals( _classpath ) )
    {
      return;
    }

    _classpath = classpath;

    List<IDirectory> cp = createDefaultClassPath();
    cp.addAll( classpath.stream().map( file -> getFileSystem().getIDirectory( file ) ).collect( Collectors.toList() ) );
    removeDups( cp );
    List<IDirectory> sp = sourcepath.stream().map( file -> getFileSystem().getIDirectory( file ) ).filter( e -> !SourcePathUtil.excludeFromSourcePath( e.toJavaFile().getAbsolutePath() ) ).collect( Collectors.toList() );

    List<IDirectory> all = new ArrayList<>();
    for( IDirectory p : sp )
    {
      if( !all.contains( p ) )
      {
        all.add( p );
      }
    }
    for( IDirectory p : cp )
    {
      if( !all.contains( p ) )
      {
        if( !SourcePathUtil.excludeFromSourcePath( p.toJavaFile().getAbsolutePath() ) )
        {
          all.add( p );
        }
      }
    }

    createSingleModule( cp, all, Collections.emptyList() );
  }

  private List<IDirectory> createDefaultClassPath()
  {
    List<String> vals = new ArrayList<>();
    vals.add( removeQuotes( System.getProperty( "java.class.path", "" ) ) );
    vals.add( System.getProperty( "sun.boot.class.path", "" ) );
    vals.add( System.getProperty( "java.ext.dirs", "" ) );

    return expand( vals );
  }

  private List<IDirectory> expand( List<String> paths )
  {
    LinkedHashSet<IDirectory> expanded = new LinkedHashSet<>();
    for( String path : paths )
    {
      for( String pathElement : path.split( File.pathSeparator ) )
      {
        if( pathElement.length() > 0 )
        {
          File filePath = new File( pathElement );
          IDirectory resource = getFileSystem().getIDirectory( filePath );
          expanded.add( resource );
        }
      }
    }
    return new ArrayList<>( expanded );
  }

  /**
   * trims leading and/or trailing double quotes
   * we've only seen this behavior on linux/macOS
   */
  private static String removeQuotes( String classpath )
  {
    if( classpath.startsWith( "\"" ) )
    {
      classpath = classpath.substring( 1 );
    }
    if( classpath.endsWith( "\"" ) )
    {
      classpath = classpath.substring( 0, classpath.length() - 1 );
    }
    return classpath;
  }

  private static void removeDups( List classpath )
  {
    for( int i = classpath.size() - 1; i >= 0; i-- )
    {
      Object f = classpath.get( i );
      classpath.remove( i );
      if( !classpath.contains( f ) )
      {
        //noinspection unchecked
        classpath.add( i, f );
      }
    }
  }
}