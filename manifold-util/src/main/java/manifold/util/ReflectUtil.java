package manifold.util;

import java.lang.reflect.InvocationTargetException;
import java.util.StringTokenizer;
import manifold.util.concurrent.ConcurrentHashSet;
import manifold.util.concurrent.ConcurrentWeakHashMap;

import java.lang.reflect.AccessibleObject;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Member;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public class ReflectUtil
{
  private static final ConcurrentWeakHashMap<Class, ConcurrentMap<String, ConcurrentHashSet<Method>>> _methodsByName = new ConcurrentWeakHashMap<>();
  private static final ConcurrentWeakHashMap<Class, ConcurrentMap<String, Field>> _fieldsByName = new ConcurrentWeakHashMap<>();
  private static final ConcurrentWeakHashMap<Class, Set<Constructor>> _constructorsByClass = new ConcurrentWeakHashMap<>();

  public static Class<?> type( String fqn )
  {
    try
    {
      return Class.forName( fqn );
    }
    catch( ClassNotFoundException e )
    {
      return type( fqn, Thread.currentThread().getContextClassLoader() );
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
      return null;
    }
  }

  public static LiveMethodRef method( Object receiver, String name, Class... params )
  {
    LiveMethodRef liveRef = WithNull.method( receiver, name, params );
    if( liveRef == null )
    {
      throw new RuntimeException( "Method '" + name + "' not found" );
    }
    return liveRef;
  }

  public static MethodRef method( String fqn, String name, Class... params )
  {
    return method( type( fqn ), name, params );
  }

  public static MethodRef method( Class<?> cls, String name, Class... params )
  {
    MethodRef match = matchFirstMethod( cls, name, params );
    if( match != null )
    {
      return match;
    }

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

  private static MethodRef matchFirstMethod( Class<?> cls, String name, Class[] params )
  {
    if( name.indexOf( '|' ) >= 0 )
    {
      for( StringTokenizer tokenizer = new StringTokenizer( name, "|" ); tokenizer.hasMoreTokens(); )
      {
        String token = tokenizer.nextToken();
        MethodRef method = method( cls, token, params );
        if( method != null )
        {
          return method;
        }
      }
    }
    return null;
  }

  public static LiveFieldRef field( Object receiver, String name )
  {
    LiveFieldRef liveRef = WithNull.field( receiver, name );
    if( liveRef == null )
    {
      throw new RuntimeException( "Field '" + name + "' not found" );
    }
    return liveRef;
  }

  public static FieldRef field( String fqn, String name )
  {
    return field( type( fqn ), name );
  }

  public static FieldRef field( Class<?> cls, String name )
  {
    FieldRef match = matchFirstField( cls, name );
    if( match != null )
    {
      return match;
    }

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

  private static FieldRef matchFirstField( Class<?> cls, String name )
  {
    if( name.indexOf( '|' ) >= 0 )
    {
      for( StringTokenizer tokenizer = new StringTokenizer( name, "|" ); tokenizer.hasMoreTokens(); )
      {
        String token = tokenizer.nextToken();
        FieldRef field = field( cls, token );
        if( field != null )
        {
          return field;
        }
      }
    }
    return null;
  }

  public static ConstructorRef constructor( String fqn, Class<?>... params )
  {
    return constructor( type( fqn ), params );
  }
  public static ConstructorRef constructor( Class<?> cls, Class<?>... params )
  {
    ConstructorRef mr = getConstructorFromCache( cls, params );
    if( mr != null )
    {
      return mr;
    }

    try
    {
      Constructor constructor = cls.getDeclaredConstructor( params );
      return addConstructorToCache( cls, constructor );
    }
    catch( Exception e )
    {
      Class superclass = cls.getSuperclass();
      if( superclass != null )
      {
        mr = constructor( superclass, params );
        if( mr != null )
        {
          return mr;
        }
      }

      for( Class iface : cls.getInterfaces() )
      {
        mr = constructor( iface, params );
        if( mr != null )
        {
          addConstructorToCache( cls, mr._constructor );
          return mr;
        }
      }
    }

    return null;
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
      NecessaryEvilUtil.getUnsafe().putObjectVolatile( m, NecessaryEvilUtil.getUnsafe().objectFieldOffset( overrideField ), true );
    }
    catch( Exception e )
    {
      throw ManExceptionUtil.unchecked( e );
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
        throw ManExceptionUtil.unchecked( e );
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

    public Method getMethod()
    {
      return _method;
    }

    public Object invoke( Object receiver, Object... args )
    {
      try
      {
        return _method.invoke( receiver, args );
      }
      catch( InvocationTargetException ite )
      {
        throw ManExceptionUtil.unchecked( ite.getCause() );
      }
      catch( Exception e )
      {
        throw ManExceptionUtil.unchecked( e );
      }
    }

    public Object invokeStatic( Object... args )
    {
      try
      {
        return _method.invoke( null, args );
      }
      catch( InvocationTargetException ite )
      {
        throw ManExceptionUtil.unchecked( ite.getCause() );
      }
      catch( Exception e )
      {
        throw ManExceptionUtil.unchecked( e );
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

    public Method getMethod()
    {
      return _method;
    }

    public Object getReceiver()
    {
      return _receiver;
    }

    public Object invoke( Object... args )
    {
      try
      {
        return _method.invoke( _receiver, args );
      }
      catch( InvocationTargetException ite )
      {
        throw ManExceptionUtil.unchecked( ite.getCause() );
      }
      catch( Exception e )
      {
        throw ManExceptionUtil.unchecked( e );
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
        throw ManExceptionUtil.unchecked( e );
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
        if( setFinal( _field, receiver, value ) )
        {
          return;
        }

        throw ManExceptionUtil.unchecked( e );
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
        throw ManExceptionUtil.unchecked( e );
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
        if( setFinal( _field, value ) )
        {
          return;
        }
        throw ManExceptionUtil.unchecked( e );
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

    public Field getField()
    {
      return _field;
    }

    public Object getReceiver()
    {
      return _receiver;
    }

    public Object get()
    {
      try
      {
        return _field.get( _receiver );
      }
      catch( Exception e )
      {
        throw ManExceptionUtil.unchecked( e );
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
        if( setFinal( _field, _receiver, value ) )
        {
          return;
        }
        throw ManExceptionUtil.unchecked( e );
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
        throw ManExceptionUtil.unchecked( e );
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
        for( Method m : methods )
        {
          int paramsLen = params == null ? 0 : params.length;
          if( m.getParameterCount() == paramsLen )
          {
            if( paramsLen > 0 )
            {
              Class<?>[] mparams = m.getParameterTypes();
              for( int i = 0; i < mparams.length; i++ )
              {
                Class<?> mparam = mparams[i];
                if( !mparam.equals( params[i] ) )
                {
                  continue outer;
                }
              }
            }
            return m;
          }
        }
      }
    }
    return null;
  }

  private static ConstructorRef addConstructorToCache( Class cls, Constructor m )
  {
    setAccessible( m );
    addRawConstructorToCache( cls, m );
    return new ConstructorRef( m );
  }

  private static void addRawConstructorToCache( Class cls, Constructor m )
  {
    Set<Constructor> constructors = _constructorsByClass.computeIfAbsent( cls, k -> ConcurrentHashMap.newKeySet() );
    constructors.add( m );
  }

  private static ConstructorRef getConstructorFromCache( Class cls, Class... params )
  {
    Constructor ctor = getRawConstructorFromCache( cls, params );
    if( ctor != null )
    {
      return new ConstructorRef( ctor );
    }
    return null;
  }

  private static Constructor getRawConstructorFromCache( Class cls, Class... params )
  {
    Set<Constructor> constructors = _constructorsByClass.get( cls );
    if( constructors != null )
    {
      outer:
      for( Constructor m : constructors )
      {
        int paramsLen = params == null ? 0 : params.length;
        if( m.getParameterCount() == paramsLen )
        {
          Class<?>[] mparams = m.getParameterTypes();
          if( paramsLen > 0 )
          {
            for( int i = 0; i < mparams.length; i++ )
            {
              Class<?> mparam = mparams[i];
              if( !mparam.equals( params[i] ) )
              {
                continue outer;
              }
            }
          }
          return m;
        }
      }
    }
    return null;
  }
  
  private static boolean setFinal( Field field, Object value )
  {
    return setFinal( field, null, value );
  }

  private static boolean setFinal( Field field, Object ctx, Object value )
  {
    if( Modifier.isFinal( field.getModifiers() ) )
    {
      try
      {
        clearFieldAccessors( field );

        removeFinalModifier( field );

        field.set( ctx, value );
        return true;
      }
      catch( Exception e )
      {
        throw ManExceptionUtil.unchecked( e );
      }
    }
    return false;
  }

  private static void removeFinalModifier( Field field ) throws Exception
  {
    Field modifiersField = Field.class.getDeclaredField( "modifiers" );
    modifiersField.setAccessible( true );
    modifiersField.setInt( field, field.getModifiers() & ~Modifier.FINAL );
  }

  private static void clearFieldAccessors( Field field ) throws Exception
  {
    Field fa = Field.class.getDeclaredField( "fieldAccessor" );
    fa.setAccessible( true );
    fa.set( field, null );

    Field ofa = Field.class.getDeclaredField( "overrideFieldAccessor" );
    ofa.setAccessible( true );
    ofa.set( field, null );

    Field rf = Field.class.getDeclaredField( "root" );
    rf.setAccessible( true );
    Field root = (Field)rf.get( field );
    if( root != null )
    {
      clearFieldAccessors( root );
    }
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

  /**
   * Utility to access live methods and fields with possible null return value if not found
   */
  public static class WithNull
  {
    public static LiveMethodRef method( Object receiver, String name, Class... params )
    {
      MethodRef ref = ReflectUtil.method( receiver.getClass(), name, params );
      if( ref == null )
      {
        return null;
      }
      return new LiveMethodRef( ref._method, receiver );
    }

    public static LiveFieldRef field( Object receiver, String name )
    {
      FieldRef ref = ReflectUtil.field( receiver.getClass(), name );
      if( ref == null )
      {
        return null;
      }
      return new LiveFieldRef( ref._field, receiver );
    }

    public static LiveMethodRef methodWithReturn( Object receiver, String name, Class<?> returnType, Class... params )
    {
      LiveMethodRef ref = method( receiver, name, params );
      if( ref != null && !returnType.isAssignableFrom( ref.getMethod().getReturnType() ) )
      {
        ref = null;
      }
      return ref;
    }
  }
}
