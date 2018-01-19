package manifold.util;

import java.lang.reflect.AccessibleObject;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Member;
import java.lang.reflect.Method;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import manifold.util.concurrent.ConcurrentHashSet;
import manifold.util.concurrent.ConcurrentWeakHashMap;

public class ReflectUtil
{
  private static final ConcurrentWeakHashMap<Class, ConcurrentMap<String, ConcurrentHashSet<Method>>> _methodsByName = new ConcurrentWeakHashMap<>();
  private static final ConcurrentWeakHashMap<Class, ConcurrentMap<String, Field>> _fieldsByName = new ConcurrentWeakHashMap<>();

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
  public static MethodRef method( Class<?> cls, String name, Class... params )
  {
    MethodRef mr = getMethodFromCache( cls, name, params );
    if( mr != null ) 
    {
      return mr;
    }
    
    try
    {
      Method method = cls.getDeclaredMethod( name, params );
      return addMethodToCache( cls, method );
    }
    catch( Exception e )
    {
      Class superclass = cls.getSuperclass();
      if( superclass != null )
      {
        mr = method( superclass, name, params );
        if( mr != null )
        {
          return mr;
        }
      }

      for( Class iface : cls.getInterfaces() )
      {
        mr = method( iface, name, params );
        if( mr != null )
        {
          addMethodToCache( cls, mr._method );
          return mr;
        }
      }
    }

    return null;
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
  public static FieldRef field( Class<?> cls, String name )
  {
    FieldRef fr = getFieldFromCache( cls, name );
    if( fr != null )
    {
      return fr;
    }

    try
    {
      Field field = cls.getDeclaredField( name );
      return addFieldToCache( cls, field );
    }
    catch( Exception e )
    {
      Class superclass = cls.getSuperclass();
      if( superclass != null )
      {
        fr = field( superclass, name );
        if( fr != null )
        {
          addFieldToCache( cls, fr._field );
          return fr;
        }
      }

      for( Class iface : cls.getInterfaces() )
      {
        fr = field( iface, name );
        if( fr != null )
        {
          addFieldToCache( cls, fr._field );
          return fr;
        }
      }
    }

    return null;
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

  public static void setAccessible( Field f )
  {
    try
    {
      f.setAccessible( true );
    }
    catch( Exception e )
    {
      setAccessible( (Member)f );
    }
  }
  public static void setAccessible( Method m )
  {
    try
    {
      m.setAccessible( true );
    }
    catch( Exception e )
    {
      setAccessible( (Member)m );
    }
  }
  public static void setAccessible( Constructor c )
  {
    try
    {
      c.setAccessible( true );
    }
    catch( Exception e )
    {
      setAccessible( (Member)c );
    }
  }
  public static void setAccessible( Member m )
  {
    Field overrideField = getOverrideField();
    try
    {
      NecessaryEvilUtil.UNSAFE.putObjectVolatile( m, NecessaryEvilUtil.UNSAFE.objectFieldOffset( overrideField ), true );
    }
    catch( Exception e )
    {
      throw new RuntimeException( e );
    }
  }

  private static Field getOverrideField()
  {
    Field overrideField = getRawFieldFromCache( AccessibleObject.class, "override" );
    if( overrideField == null )
    {
      try
      {
        overrideField = AccessibleObject.class.getDeclaredField( "override" );
        addRawFieldToCache( AccessibleObject.class, overrideField );
      }
      catch( Exception e )
      {
        throw new RuntimeException( e );
      }
    }
    return overrideField;
  }

  public static class MethodRef
  {
    private final Method _method;

    private MethodRef( Method m )
    {
      _method = m;
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

  private static MethodRef addMethodToCache( Class cls, Method m )
  {
    setAccessible( m );
    addRawMethodToCache( cls, m );
    return new MethodRef( m );
  }

  private static void addRawMethodToCache( Class cls, Method m )
  {
    ConcurrentMap<String, ConcurrentHashSet<Method>> methodsByName = _methodsByName.get( cls );
    if( methodsByName == null )
    {
      _methodsByName.put( cls, methodsByName = new ConcurrentHashMap<>() );
    }
    ConcurrentHashSet<Method> methods = methodsByName.get( m.getName() );
    if( methods == null )
    {
      methodsByName.put( m.getName(), methods = new ConcurrentHashSet<>( 2 ) );
    }
    methods.add( m );
  }

  private static MethodRef getMethodFromCache( Class cls, String name, Class... params )
  {
    Method m = getRawMethodFromCache( cls, name, params );
    if( m != null )
    {
      return new MethodRef( m );
    }
    return null;
  }

  private static Method getRawMethodFromCache( Class cls, String name, Class... params )
  {
    ConcurrentMap<String, ConcurrentHashSet<Method>> methodsByName = _methodsByName.get( cls );
    if( methodsByName != null )
    {
      ConcurrentHashSet<Method> methods = methodsByName.get( name );
      if( methods != null )
      {
        outer:
        for( Method m: methods )
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
            return m;
          }
        }
      }
    }
    return null;
  }

  private static FieldRef addFieldToCache( Class cls, Field f )
  {
    setAccessible( f );
    addRawFieldToCache( cls, f );
    return new FieldRef( f );
  }

  private static FieldRef getFieldFromCache( Class cls, String name )
  {
    Field field = getRawFieldFromCache( cls, name );
    if( field != null )
    {
      return new FieldRef( field );
    }
    return null;
  }

  private static void addRawFieldToCache( Class cls, Field f )
  {
    ConcurrentMap<String, Field> fieldsByName = _fieldsByName.get( cls );
    if( fieldsByName == null )
    {
      _fieldsByName.put( cls, fieldsByName = new ConcurrentHashMap<>() );
    }
    fieldsByName.put( f.getName(), f );
  }

  private static Field getRawFieldFromCache( Class cls, String name )
  {
    ConcurrentMap<String, Field> fieldsByName = _fieldsByName.get( cls );
    if( fieldsByName != null )
    {
      return fieldsByName.get( name );
    }
    return null;
  }
}
