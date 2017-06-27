package manifold.internal.host;

import com.sun.tools.javac.processing.JavacProcessingEnvironment;
import java.net.URL;
import java.util.Iterator;
import java.util.List;
import java.util.ServiceLoader;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.Supplier;
import javax.script.Bindings;
import javax.tools.DiagnosticListener;
import javax.tools.JavaFileManager;
import javax.tools.JavaFileObject;
import manifold.api.fs.IFile;
import manifold.api.fs.IFileSystem;
import manifold.api.sourceprod.TypeName;
import manifold.api.host.IModule;
import manifold.api.host.ITypeLoader;
import manifold.api.host.ITypeLoaderListener;
import manifold.api.host.IManifoldHost;

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

        ServiceLoader<IManifoldHost> loader = ServiceLoader.load( IManifoldHost.class, ManifoldHost.class.getClassLoader() );
        Iterator<IManifoldHost> iterator = loader.iterator();
        if( iterator.hasNext() )
        {
          // ⚔ there can be only one ⚔
          HOST = iterator.next();

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
            HOST = iterator.next();

            if( iterator.hasNext() )
            {
              System.out.println( "WARNING: Found multiple Manifold hosts, using first encountered: " + HOST.getClass().getName() );
            }
          }
        }
        
        if( HOST == null )
        {
          HOST = new DefaultManifoldHost();
        }
      }
    }

    return HOST;
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
    host().bootstrap();
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

  public static ITypeLoader getLoader( IFile file, IModule module )
  {
    return host().getLoader( file, module );
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

  public static void maybeAssignGosuType( ClassLoader loader, String strType, URL url, BiConsumer<String, Supplier<byte[]>> assigner )
  {
    host().maybeAssignManifoldType( loader, strType, url, assigner );
  }

  public static void performLockedOperation( ClassLoader loader, Runnable operation )
  {
    host().performLockedOperation( loader, operation );
  }

  public static void initializeAndCompileNonJavaFiles( JavacProcessingEnvironment jpe, JavaFileManager fileManager, List<String> files, Supplier<Set<String>> sourcePath, Supplier<List<String>> classpath, Supplier<String> outputPath )
  {
    host().initializeAndCompileNonJavaFiles( jpe, fileManager, files, sourcePath, classpath, outputPath );
  }

  public static Set<TypeName> getChildrenOfNamespace( String packageName )
  {
    return host().getChildrenOfNamespace( packageName );
  }
}
