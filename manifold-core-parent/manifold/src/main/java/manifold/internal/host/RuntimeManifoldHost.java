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
import javax.script.Bindings;
import javax.script.SimpleBindings;
import manifold.api.fs.IDirectory;
import manifold.api.host.IRuntimeManifoldHost;
import manifold.internal.runtime.UrlClassLoaderWrapper;
import manifold.util.SourcePathUtil;

/**
 */
public class RuntimeManifoldHost extends SingleModuleManifoldHost implements IRuntimeManifoldHost
{
  private volatile static IRuntimeManifoldHost HOST;

  private List<File> _classpath;


  public static IRuntimeManifoldHost get()
  {
    if( HOST == null )
    {
      synchronized( RuntimeManifoldHost.class )
      {
        if( HOST != null )
        {
          return HOST;
        }

        // Allow for the IRuntimeManifoldHost impl to be customised as a Java Service
        loadRuntimeManifoldHost();

        if( HOST == null )
        {
          HOST = new RuntimeManifoldHost();
        }
      }
    }

    return HOST;
  }

  private static void loadRuntimeManifoldHost()
  {
    try
    {
      if( !loadRuntimeManifoldHost( RuntimeManifoldHost.class.getClassLoader() ) )
      {
        loadRuntimeManifoldHost( Thread.currentThread().getContextClassLoader() );
      }
    }
    catch( ServiceConfigurationError e )
    {
      e.printStackTrace();
    }
  }

  private static boolean loadRuntimeManifoldHost( ClassLoader cl )
  {
    ServiceLoader<IRuntimeManifoldHost> loader = ServiceLoader.load( IRuntimeManifoldHost.class, cl );
    Iterator<IRuntimeManifoldHost> iterator = loader.iterator();
    if( iterator.hasNext() )
    {
      HOST = iterator.next();
      if( iterator.hasNext() )
      {
        // ⚔ there can be only one ⚔
        System.out.println( "WARNING: Found multiple Manifold hosts, using first encountered: " + HOST.getClass().getName() );
      }
      return true;
    }
    return false;
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

  public void init( List<File> sourcepath, List<File> classpath )
  {
    List<File> combined = new ArrayList<>();
    if( classpath != null )
    {
      combined.addAll( classpath );
    }
    combined.addAll( deriveClasspathFrom( SourcePathUtil.class ) );
    setPaths( sourcepath, combined );
  }

  private List<File> deriveClasspathFrom( Class clazz )
  {
    List<File> ll = new LinkedList<>();
    ClassLoader loader = clazz.getClassLoader();
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
      loader = loader.getParent();
    }
//    addBootstrapClasses( ll );
    return ll;
  }

  private void setPaths( List<File> sourcepath, List<File> classpath )
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

  public Bindings createBindings()
  {
    return new SimpleBindings();
  }
}