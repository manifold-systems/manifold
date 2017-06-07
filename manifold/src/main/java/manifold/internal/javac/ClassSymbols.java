package manifold.internal.javac;

import com.sun.tools.javac.api.JavacTaskImpl;
import com.sun.tools.javac.api.JavacTool;
import com.sun.tools.javac.code.Symbol;
import com.sun.tools.javac.model.JavacElements;
import java.io.IOException;
import java.io.StringWriter;
import java.nio.charset.Charset;
import java.util.Collections;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import javax.tools.StandardJavaFileManager;
import javax.tools.StandardLocation;
import manifold.api.fs.IResource;
import manifold.api.gen.SrcClass;
import manifold.api.host.IModule;
import manifold.api.host.ITypeLoaderListener;
import manifold.api.host.RefreshRequest;
import manifold.internal.host.ManifoldHost;

/**
 * Utility to get ClassSymbol for a given type name.
 * <p/>
 * Note this class must have a FileManager separate from the one used in JavaParser (or JavacHook)
 * to avoid circularity issues.
 */
public class ClassSymbols
{
  private static final Map<IModule,ClassSymbols> INSTANCES = new ConcurrentHashMap<>();

  private final IModule _module;
  private JavacTool _javacTool;
  private volatile StandardJavaFileManager _fm;

  public static ClassSymbols instance( IModule module )
  {
    ClassSymbols classSymbols = INSTANCES.get( module );
    if( classSymbols == null )
    {
      INSTANCES.put( module, classSymbols = new ClassSymbols( module ) );
    }
    return classSymbols;
  }

  private ClassSymbols( IModule module )
  {
    _module = module;
    ManifoldHost.addTypeLoaderListenerAsWeakRef( module, new CacheClearer() );
  }

  private void init()
  {
    if( _fm != null )
    {
      return;
    }

    synchronized( this )
    {
      if( _fm != null )
      {
        return;
      }

      _javacTool = JavacTool.create();
      StandardJavaFileManager fm = _javacTool.getStandardFileManager( null, null, Charset.forName( "UTF-8" ) );

      try
      {
        fm.setLocation( StandardLocation.SOURCE_PATH, _module.getCollectiveSourcePath().stream().map( IResource::toJavaFile ).collect( Collectors.toList() ) );
        fm.setLocation( StandardLocation.CLASS_PATH, _module.getCollectiveJavaClassPath().stream().map( IResource::toJavaFile ).collect( Collectors.toList() ) );
        _fm = fm;
      }
      catch( IOException e )
      {
        throw new RuntimeException( e );
      }
    }
  }

  public JavacTaskImpl getJavacTask()
  {
    init();

    StringWriter errors = new StringWriter();
    return (JavacTaskImpl)_javacTool.getTask( errors, _fm, null, Collections.singletonList( "-proc:none" ), null, null );
  }

  public Symbol.ClassSymbol getClassSymbol( JavacTaskImpl javacTask, String fqn )
  {
    JavacElements elementUtils = JavacElements.instance( javacTask.getContext() );
    return elementUtils.getTypeElement( fqn );
  }

  public SrcClass makeSrcClassStub( String fqn )
  {
    return makeSrcClassStub( fqn, null );
  }
  public SrcClass makeSrcClassStub( String fqn, JavacTaskImpl[] javacTaskOut )
  {
    JavacTaskImpl javacTask = getJavacTask();
    Symbol.ClassSymbol classSymbol = getClassSymbol( javacTask, fqn );
    if( javacTaskOut != null )
    {
      javacTaskOut[0] = javacTask;
    }
    return SrcClassUtil.instance().makeStub( fqn, classSymbol );
  }

  private class CacheClearer implements ITypeLoaderListener
  {
    @Override
    public void refreshedTypes( RefreshRequest request )
    {
    }

    @Override
    public void refreshed()
    {
      INSTANCES.remove( _module );
    }
  }
}
