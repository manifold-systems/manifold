package manifold.internal;

import com.sun.source.tree.CompilationUnitTree;
import com.sun.source.util.JavacTask;
import com.sun.source.util.Plugin;
import com.sun.source.util.TaskEvent;
import com.sun.source.util.TaskListener;
import com.sun.tools.javac.api.BasicJavacTask;
import com.sun.tools.javac.code.Symbol;
import com.sun.tools.javac.code.Symtab;
import com.sun.tools.javac.tree.JCTree;
import com.sun.tools.javac.util.Context;
import com.sun.tools.javac.util.Name;
import com.sun.tools.javac.util.Names;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * This class serves as a placeholder for the Manifold JavacPlugin so that the Java 9 compiler will not produce an error
 * because the manifold parent pom.xml automatically applies the <code>-Xplugin:Manifold</code> compiler argument to all
 * manifold child modules, including the core manifold module, which fails because the the JavacPlugin is defined there.
 */
public class PluginPlaceholder implements Plugin, TaskListener
{
  private static boolean IS_JAVA_8;

  static
  {
    try
    {
      Class.forName( "java.lang.Module" );
      IS_JAVA_8 = false;
    }
    catch( Throwable ignore )
    {
      IS_JAVA_8 = true;
    }
  }

  private Context _ctx;
  private boolean _done;

  @Override
  public String getName()
  {
    return "BootstrapPlugin";
  }

  @Override
  public void init( JavacTask task, String... args )
  {
    _ctx = ((BasicJavacTask)task).getContext();
    task.addTaskListener( this );
  }

  @Override
  public void started( TaskEvent te )
  {
    if( IS_JAVA_8 )
    {
      return;
    }

    CompilationUnitTree compilationUnit = te.getCompilationUnit();
    if( !(compilationUnit instanceof JCTree.JCCompilationUnit) )
    {
      return;
    }

    if( _done )
    {
      return;
    }

    _done = true;

    openModule( _ctx, "jdk.compiler" );
  }

  @Override
  public void finished( TaskEvent e )
  {

  }

  public static void openModule( Context context, String moduleName )
  {
    try
    {
      Symbol moduleToOpen = (Symbol)ReflectUtil.method( Symtab.instance( context ), "getModule", Name.class )
        .invoke( Names.instance( context ).fromString( moduleName ) );

      if( moduleToOpen == null )
      {
        // not modular java 9
        return;
      }

      moduleToOpen.complete();
      
      Set<Symbol> rootModules = (Set<Symbol>)ReflectUtil.field(
        ReflectUtil.method( ReflectUtil.type( "com.sun.tools.javac.comp.Modules" ), "instance", Context.class ).invokeStatic( context ), "allModules" ).get();

      for( Symbol rootModule: rootModules )
      {
        rootModule.complete();

        List<Object> requires = (List<Object>)ReflectUtil.field( rootModule, "requires" ).get();
        List<Object> newRequires = new ArrayList( requires );
        Object addedRequires = ReflectUtil.constructor( "com.sun.tools.javac.code.Directive$RequiresDirective", ReflectUtil.type( "com.sun.tools.javac.code.Symbol$ModuleSymbol" ) ).newInstance( moduleToOpen );
        newRequires.add( addedRequires );
        requires = com.sun.tools.javac.util.List.from( newRequires );
        ReflectUtil.field( rootModule, "requires" ).set( requires );

        List<Object> exports = new ArrayList<>( (Collection)ReflectUtil.field( moduleToOpen, "exports" ).get() );
        for( Symbol pkg : (Iterable<Symbol>)ReflectUtil.field( moduleToOpen, "enclosedPackages" ).get() )
        {
          if( pkg instanceof Symbol.PackageSymbol )
          {
            System.err.println( "PACKAGE: " + pkg );
            Object exp = ReflectUtil.constructor( "com.sun.tools.javac.code.Directive$ExportsDirective", Symbol.PackageSymbol.class, com.sun.tools.javac.util.List.class ).newInstance( pkg,
              com.sun.tools.javac.util.List.of( rootModule ) );
            exports.add( exp );

            ((Map)ReflectUtil.field( rootModule, "visiblePackages" ).get()).put( ((Symbol.PackageSymbol)pkg).fullname, pkg );
          }
        }
        ReflectUtil.field( moduleToOpen, "exports" ).set( com.sun.tools.javac.util.List.from( exports ) );

        Set readModules = (Set)ReflectUtil.field( moduleToOpen, "readModules" ).get();
        readModules.add( rootModule );
        ReflectUtil.field( moduleToOpen, "readModules" ).set( readModules );
      }

    }
    catch( Throwable e )
    {
      System.err.println( "Failed to reflectively add-exports " + moduleName + "/* to root module[s], you must add the following argument to jave.exe:\n" +
                          "  --add-exports=" + moduleName + "/*=<root-module>\n" );
      throw new RuntimeException( e );
    }
  }

  // copied from manifold for now
  private static class ReflectUtil
  {
    public static Class<?> type( String fqn )
    {
      try
      {
        return Class.forName( fqn );
      }
      catch( ClassNotFoundException e )
      {
        throw new RuntimeException( e );
      }
    }

    public static LiveMethodRef method( Object receiver, String name, Class... params )
    {
      MethodRef ref = method( receiver.getClass(), name, params );
      if( ref == null )
      {
        throw new RuntimeException( "Method '" + name + "' not found" );
      }
      return new LiveMethodRef( ref._method, receiver );
    }
    public static MethodRef method( Class cls, String name, Class... params )
    {
      outer:
      for( Method m: cls.getDeclaredMethods() )
      {
        if( m.getName().equals( name ) )
        {
          Class<?>[] mparams = m.getParameterTypes();
          int paramsLen = params == null ? 0 : params.length;
          if( mparams.length == paramsLen )
          {
            for( int i = 0; i < mparams.length; i++ )
            {
              Class<?> mparam = mparams[i];
              if( !mparam.equals( params[i] ) )
              {
                continue outer;
              }
            }
            return new MethodRef( m );
          }
        }
      }

      Class superclass = cls.getSuperclass();
      if( superclass != null )
      {
        return method( superclass, name, params );
      }

      throw new RuntimeException( "Method '" + name + "' not found" );
    }

    public static LiveFieldRef field( Object receiver, String name )
    {
      FieldRef ref = field( receiver.getClass(), name );
      if( ref == null )
      {
        throw new RuntimeException( "Field '" + name + "' not found" );
      }
      return new LiveFieldRef( ref._field, receiver );
    }
    public static FieldRef field( Class cls, String name )
    {
      for( Field f: cls.getDeclaredFields() )
      {
        if( f.getName().equals( name ) )
        {
          return new FieldRef( f );
        }
      }

      Class superclass = cls.getSuperclass();
      if( superclass != null )
      {
        return field( superclass, name );
      }

      throw new RuntimeException( "Field '" + name + "' not found" );
    }

    public static ConstructorRef constructor( String fqn, Class<?>... params )
    {
      try
      {
        Class<?> cls = Class.forName( fqn );
        return new ConstructorRef( cls.getDeclaredConstructor( params ) );
      }
      catch( Exception e )
      {
        throw new RuntimeException( e );
      }
    }

    public static class MethodRef
    {
      private final Method _method;

      private MethodRef( Method m )
      {
        _method = m;
        _method.setAccessible( true );
      }

      public Object invoke( Object receiver, Object... args )
      {
        try
        {
          return _method.invoke( receiver, args );
        }
        catch( Exception e )
        {
          throw new RuntimeException( e );
        }
      }

      public Object invokeStatic( Object... args )
      {
        try
        {
          return _method.invoke( null, args );
        }
        catch( Exception e )
        {
          throw new RuntimeException( e );
        }
      }

    }

    public static class LiveMethodRef
    {
      private Method _method;
      private Object _receiver;

      private LiveMethodRef( Method m, Object receiver )
      {
        _method = m;
        _method.setAccessible( true );
        _receiver = receiver;
      }

      public Object invoke( Object... args )
      {
        try
        {
          return _method.invoke( _receiver, args );
        }
        catch( Exception e )
        {
          throw new RuntimeException( e );
        }
      }
    }

    public static class FieldRef
    {
      private final Field _field;

      private FieldRef( Field f )
      {
        _field = f;
        _field.setAccessible( true );
      }

      public Object get( Object receiver )
      {
        try
        {
          return _field.get( receiver );
        }
        catch( Exception e )
        {
          throw new RuntimeException( e );
        }
      }

      public void set( Object receiver, Object value )
      {
        try
        {
          _field.set( receiver, value );
        }
        catch( Exception e )
        {
          throw new RuntimeException( e );
        }
      }

      public Object getStatic()
      {
        try
        {
          return _field.get( null );
        }
        catch( Exception e )
        {
          throw new RuntimeException( e );
        }
      }

      public void setStatic( Object value )
      {
        try
        {
          _field.set( null, value );
        }
        catch( Exception e )
        {
          throw new RuntimeException( e );
        }
      }

    }
    public static class LiveFieldRef
    {
      private final Field _field;
      private final Object _receiver;

      private LiveFieldRef( Field f, Object receiver )
      {
        _field = f;
        _field.setAccessible( true );
        _receiver = receiver;
      }

      public Object get()
      {
        try
        {
          return _field.get( _receiver );
        }
        catch( Exception e )
        {
          throw new RuntimeException( e );
        }
      }

      public void set( Object value )
      {
        try
        {
          _field.set( _receiver, value );
        }
        catch( Exception e )
        {
          throw new RuntimeException( e );
        }
      }
    }

    public static class ConstructorRef
    {
      private final Constructor<?> _constructor;

      private ConstructorRef( Constructor<?> constructor )
      {
        _constructor = constructor;
      }

      public Object newInstance( Object... args )
      {
        try
        {
          return _constructor.newInstance( args );
        }
        catch( Exception e )
        {
          throw new RuntimeException( e );
        }
      }
    }
  }
}
