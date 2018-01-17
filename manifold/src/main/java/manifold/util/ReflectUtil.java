package manifold.util;

import java.lang.reflect.AccessibleObject;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Member;
import java.lang.reflect.Method;

public class ReflectUtil
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
  public static Class<?> type( String fqn, ClassLoader cl )
  {
    try
    {
      return Class.forName( fqn, false, cl );
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


  public static void setAccessible( Member m )
  {
    try
    {

      Field overrideField = AccessibleObject.class.getDeclaredField( "override" );
      NecessaryEvilUtil.UNSAFE.putObjectVolatile( m, NecessaryEvilUtil.UNSAFE.objectFieldOffset( overrideField ), true );
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
      setAccessible( _method );
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
      setAccessible( _method );
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
      setAccessible( _field );
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
      setAccessible( _field );
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
