package manifold.internal.host;

import java.io.File;
import java.net.URL;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.ServiceConfigurationError;
import java.util.ServiceLoader;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.Supplier;
import javax.annotation.processing.ProcessingEnvironment;
import javax.script.Bindings;
import javax.tools.DiagnosticListener;
import javax.tools.JavaFileManager;
import javax.tools.JavaFileObject;
import manifold.api.fs.IFileSystem;
import manifold.api.host.IManifoldHost;
import manifold.api.host.IModule;
import manifold.api.host.ITypeLoaderListener;
import manifold.api.type.TypeName;

/**
 */
public class ManifoldHost
{
  private volatile static IManifoldHost HOST;

  private static IManifoldHost host()
  {
    if( HOST == null )
    {
      synchronized( ManifoldHost.class )
      {
        if( HOST != null )
        {
          return HOST;
        }

        try
        {
          ServiceLoader<IManifoldHost> loader = ServiceLoader.load( IManifoldHost.class, ManifoldHost.class.getClassLoader() );
          Iterator<IManifoldHost> iterator = loader.iterator();
          if( iterator.hasNext() )
          {
            // ⚔ there can be only one ⚔
            IManifoldHost host = iterator.next();
            while( host != null && !host.accept() )
            {
              host = iterator.hasNext() ? iterator.next() : null;
            }
            HOST = host;
            if( iterator.hasNext() )
            {
              System.out.println( "WARNING: Found multiple Manifold hosts, using first encountered: " + HOST.getClass().getName() );
            }
          }
          else
          {
            loader = ServiceLoader.load( IManifoldHost.class );
            iterator = loader.iterator();
            if( iterator.hasNext() )
            {
              // ⚔ there can be only one ⚔
              IManifoldHost host = iterator.next();
              while( host != null && !host.accept() )
              {
                host = iterator.hasNext() ? iterator.next() : null;
              }
              HOST = host;

              if( iterator.hasNext() )
              {
                System.out.println( "WARNING: Found multiple Manifold hosts, using first encountered: " + HOST.getClass().getName() );
              }
            }
          }
        }
        catch( ServiceConfigurationError e )
        {
          // let a module declare it's own service, avoid having to add a separate module just to declare
          // your Manifold host service or saves users from having to know to add META-INF services bullshit
          e.printStackTrace();
        }

        if( HOST == null )
        {
          IManifoldHost host = new DefaultManifoldHost();
          if( host.accept() )
          {
            HOST = host;
          }
        }
      }
    }

    return HOST;
  }

  public static IManifoldHost instance()
  {
    return host();
  }

  public static IFileSystem getFileSystem()
  {
    return host().getFileSystem();
  }

  public static ClassLoader getActualClassLoader()
  {
    return host().getActualClassLoader();
  }

  public static void bootstrap()
  {
    bootstrap( Collections.emptyList(), Collections.emptyList() );
  }
  public static void bootstrap( List<File> sourcepath, List<File> classpath  )
  {
    host().bootstrap( sourcepath, classpath );
  }

  public static IModule getGlobalModule()
  {
    return host().getGlobalModule();
  }


  public static IModule getCurrentModule()
  {
    return host().getCurrentModule();
  }

  public static void resetLanguageLevel()
  {
    host().resetLanguageLevel();
  }

  public static boolean isPathIgnored( String path )
  {
    return host().isPathIgnored( path );
  }

  public static String[] getAllReservedWords()
  {
    return host().getAllReservedWords();
  }

  public static Bindings createBindings()
  {
    return host().createBindings();
  }

  public static void addTypeLoaderListenerAsWeakRef( Object ctx, ITypeLoaderListener listener )
  {
    host().addTypeLoaderListenerAsWeakRef( ctx, listener );
  }

  public static JavaFileObject produceFile( String fqn, IModule module, DiagnosticListener<JavaFileObject> errorHandler )
  {
    return host().produceFile( fqn, module, errorHandler );
  }

  public static void maybeAssignType( ClassLoader loader, String strType, URL url, BiConsumer<String, Supplier<byte[]>> assigner )
  {
    host().maybeAssignManifoldType( loader, strType, url, assigner );
  }

  public static void performLockedOperation( ClassLoader loader, Runnable operation )
  {
    host().performLockedOperation( loader, operation );
  }

  public static void initializeAndCompileNonJavaFiles( ProcessingEnvironment procEnv, JavaFileManager fileManager, List<String> files, Supplier<Set<String>> sourcePath, Supplier<List<String>> classpath, Supplier<List<String>> outputPath )
  {
    host().initializeAndCompileNonJavaFiles( procEnv, fileManager, files, sourcePath, classpath, outputPath );
  }

  public static Set<TypeName> getChildrenOfNamespace( String packageName )
  {
    return host().getChildrenOfNamespace( packageName );
  }

  public static boolean isBootstrapped()
  {
    return host().isBootstrapped();
  }
}
