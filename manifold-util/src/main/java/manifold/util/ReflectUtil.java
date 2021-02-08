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

package manifold.util;

import java.io.IOException;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.lang.reflect.*;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import manifold.util.concurrent.ConcurrentHashSet;
import manifold.util.concurrent.ConcurrentWeakHashMap;
import manifold.util.concurrent.LocklessLazyVar;

/**
 * A Java reflection utility.  Use it to efficiently access classes by name, get/set field values,
 * invoke methods, and use constructors. Notable features include:
 * <ul>
 * <li>Call a private method</li>
 * <li>Get and set the value of a private, final field</li>
 * <li>Access fields and methods of a class belonging to an inaccessible module</li>
 * <li>Fields, methods, and constructors are cached upon use to improve performance</li>
 * </ul>
 * <p>
 * (Use <b>@Jailbreak</b> to avoid writing reflection code.
 * See <a href="http://manifold.systems/docs.html#type-safe-reflection">Type-safe Reflection</a>.)
 */
public class ReflectUtil
{
  private static final ConcurrentWeakHashMap<Class, ConcurrentMap<String, ConcurrentHashSet<Method>>> _methodsByName = new ConcurrentWeakHashMap<>();
  private static final ConcurrentWeakHashMap<Class, ConcurrentMap<String, Field>> _fieldsByName = new ConcurrentWeakHashMap<>();
  private static final ConcurrentWeakHashMap<Class, Set<Constructor>> _constructorsByClass = new ConcurrentWeakHashMap<>();
  private static final ConcurrentWeakHashMap<Method, ConcurrentMap<Class, Method>> _structuralCall = new ConcurrentWeakHashMap<>();
  private static final LocklessLazyVar<ClassContextSecurityManager> _sm = LocklessLazyVar.make( () -> new ClassContextSecurityManager() );
  private static final String LAMBDA_METHOD = "lambda method";

  //private static final ConcurrentHashMap<String, Boolean> _openPackages = new ConcurrentHashMap<>();

  static
  {
    NecessaryEvilUtil.disableJava9IllegalAccessWarning();
  }

  /**
   * Searches the class loader of this class for the specified name, if not found searches
   * the current thread's context class loader.
   *
   * @param fqn The qualified name of the type e.g., {@code "java.lang.String"} or {@code "java.lang.String[]"}
   * @return The {@code Class} corresponding with {@code fqn} or null if not found
   */
  public static Class<?> type( String fqn )
  {
    return type( fqn, false );
  }
  public static Class<?> type( String fqn, boolean useCallChain )
  {
    return type( fqn, ReflectUtil.class.getClassLoader(), useCallChain );
  }

  /**
   * Searches {@code cl} for the specified class {@code fqn}.
   *
   * @param fqn The qualified name of the type e.g., {@code "java.lang.String"}
   * @param cl  The class loader to search
   * @return The {@code Class} corresponding with {@code fqn} or null if not found
   */
  public static Class<?> type( String fqn, ClassLoader cl )
  {
    return type( fqn, cl, false );
  }
  public static Class<?> type( String fqn, ClassLoader cl, boolean useCallChain )
  {
    int dims = 0;
    int iBracket = fqn.indexOf( '[' );
    String componentFqn = fqn;
    if( iBracket > 0 )
    {
      dims = (fqn.length() - iBracket)/2;
      componentFqn = fqn.substring( 0, iBracket );
    }
    //openPackage( fqn, null );
    Class<?> cls;
    try
    {
      cls = Class.forName( componentFqn, false, cl );
    }
    catch( ClassNotFoundException e )
    {
      ClassLoader contextClassLoader = Thread.currentThread().getContextClassLoader();
      if( cl != contextClassLoader && contextClassLoader != null )
      {
        return type( fqn, contextClassLoader, useCallChain );
      }

      cls = useCallChain ? findInCallChain( fqn ) : null;
    }
    if( cls != null && dims > 0 )
    {
      cls = Array.newInstance( cls, new int[dims] ).getClass();
    }
    return cls;
  }

  private static Class<?> findInCallChain( String fqn )
  {
    Class[] stackTraceClasses = Objects.requireNonNull( _sm.get() ).getClassContext();
    if( stackTraceClasses == null )
    {
      return null;
    }

    HashSet<ClassLoader> attempted = new HashSet<>();
    attempted.add( ReflectUtil.class.getClassLoader() );
    attempted.add( Thread.currentThread().getContextClassLoader() );

    for( Class cls: stackTraceClasses )
    {
      ClassLoader cl = cls.getClassLoader();
      if( attempted.contains( cl ) )
      {
        continue;
      }
      attempted.add( cl );
      return type( fqn, cl, false );
    }
    return null;
  }

  /**
   * Get a {@link LiveMethodRef} to the specified method. Typical use:
   * <p>
   * <pre> method(str, "substring", int.class).invoke(2) </pre>
   *
   * @param receiver The object to make the call on
   * @param name     The name of the method to call or a '|' separated list of names, where the first found is used
   * @param params   The types of the method's parameters
   * @return A reference to the specified method, throws {@link RuntimeException} if the method is not found.
   * Use {@link WithNull} to avoid the RuntimeException.
   */
  public static LiveMethodRef method( Object receiver, String name, Class... params )
  {
    LiveMethodRef liveRef = WithNull.method( receiver, name, params );
    if( liveRef == null )
    {
      throw new RuntimeException( "Method '" + name + "' not found" );
    }
    return liveRef;
  }

  /**
   * Get a {@link MethodRef} to the specified method. Typical use:
   * <p>
   * <pre> method("java.time.LocalTime", "of", int.class, int.class).invokeStatic(5, 30) </pre>
   *
   * @param fqn    The qualified name of the class containing the method
   * @param name   The name of the method or a '|' separated list of names, where the first found is used
   * @param params The types of the method's parameters
   * @return A reference to the specified method or null if not found
   */
  public static MethodRef method( String fqn, String name, Class... params )
  {
    return method( type( fqn ), name, params );
  }

  /**
   * Get a {@link MethodRef} to the specified method. Typical use:
   * <p>
   * <pre> method(LocalTime.class, "of", int.class, int.class).invokeStatic(5, 30) </pre>
   *
   * @param cls    The class containing the method
   * @param name   The name of the method or a '|' separated list of names, where the first found is used
   * @param params The types of the method's parameters
   * @return A reference to the specified method or null if not found
   */
  public static MethodRef method( Class<?> cls, String name, Class... params )
  {
    // This indirection avoids NPE checks from IJ, which cause annoying compile warnings
    // that otherwise cause crap like 'Optional' to be layered on the API
    return _method( cls, name, params );
  }
  private static MethodRef _method( Class<?> cls, String name, Class... params )
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
        mr = _method( superclass, name, params );
        if( mr != null )
        {
          addMethodToCache( cls, mr._method );
          return mr;
        }
      }

      for( Class iface: cls.getInterfaces() )
      {
        mr = _method( iface, name, params );
        if( mr != null )
        {
          addMethodToCache( cls, mr._method );
          return mr;
        }
      }
    }

    return null;
  }

  /**
   * Get a {@link MethodRef} to from the specified {@code name} without regard to parameter types. If more than one
   * method has the {@code name}, the first one encountered is used, in no particular order. This method should be used
   * only when the named method is <i>not</i> overloaded. Typical use:
   * <p/>
   * <pre> methodByName(LocalTime.class, "isAfter").invoke(source, time) </pre>
   *
   * @param cls  The class containing the method
   * @param name The name of the method or a '|' separated list of names, where the first found is used
   * @return A reference to the specified method or null if not found
   */
  @SuppressWarnings( "unused" )
  public static MethodRef methodFromName( Class<?> cls, String name )
  {
    MethodRef mr = getMethodFromCacheUsingNameOnly( cls, name );
    if( mr != null )
    {
      return mr;
    }

    for( Method method: cls.getDeclaredMethods() )
    {
      if( method.getName().equals( name ) )
      {
        return addMethodToCache( cls, method );
      }
    }

    Class superclass = cls.getSuperclass();
    if( superclass != null )
    {
      mr = methodFromName( superclass, name );
      if( mr != null )
      {
        addMethodToCache( cls, mr._method );
        return mr;
      }
    }

    for( Class iface: cls.getInterfaces() )
    {
      mr = methodFromName( iface, name );
      if( mr != null )
      {
        addMethodToCache( cls, mr._method );
        return mr;
      }
    }

    return null;
  }

  /**
   * Get a {@link MethodRef} corresponding with the functional interface implemented by {@code lambdaClass}.
   * <p/>
   * <pre> lambdaMethod(function.getClass()).invoke(function, args) </pre>
   *
   * @return A reference to the specified method or null if not found
   */
  public static MethodRef lambdaMethod( Class<?> lambdaClass )
  {
    MethodRef mr = getMethodFromCacheUsingNameOnly( lambdaClass, LAMBDA_METHOD );
    if( mr != null )
    {
      return mr;
    }

    for( Class iface: lambdaClass.getInterfaces() )
    {
      for( Method m: iface.getMethods() )
      {
        if( (m.getModifiers() & (Modifier.ABSTRACT | Modifier.PUBLIC)) == (Modifier.ABSTRACT | Modifier.PUBLIC) )
        {
          return addMethodToCache( lambdaClass, m, LAMBDA_METHOD );
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

  /**
   * Get a {@link LiveFieldRef} to the specified field.  Typical use:
   * <p>
   * <pre> String name = field(foo, "name").get(); </pre>
   *
   * @param receiver The object having the field
   * @param name     The name of the field or a '|' separated list of names, where the first found is used
   * @return A reference to the specified field, throws {@link RuntimeException} if the field is not found.
   * Use {@link WithNull} to avoid the RuntimeException.
   */
  public static LiveFieldRef field( Object receiver, String name )
  {
    LiveFieldRef liveRef = WithNull.field( receiver, name );
    if( liveRef == null )
    {
      throw new RuntimeException( "Field '" + name + "' not found" );
    }
    return liveRef;
  }

  /**
   * Get a {@link FieldRef} to the specified field.  Typical use:
   * <p>
   * <pre> field("java.time.LocalTime", "hour").get(); </pre>
   *
   * @param fqn  The qualified name of the class having the field
   * @param name The name of the field or a '|' separated list of names, where the first found is used
   * @return A reference to the specified field or null if not found
   */
  public static FieldRef field( String fqn, String name )
  {
    return field( type( fqn ), name );
  }

  /**
   * Get a {@link FieldRef} to the specified field.  Typical use:
   * <p>
   * <pre> field(LocalTime.class, "hour").get(); </pre>
   *
   * @param cls  The class having the field
   * @param name The name of the field or a '|' separated list of names, where the first found is used
   * @return A reference to the specified field or null if not found
   */
  public static FieldRef field( Class<?> cls, String name )
  {
    // This indirection avoids NPE checks from IJ, which cause annoying compile warnings
    // that otherwise cause crap like 'Optional' to be layered on the API
    return _field( cls, name );
  }
  private static FieldRef _field( Class<?> cls, String name )
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
        fr = _field( superclass, name );
        if( fr != null )
        {
          addFieldToCache( cls, fr._field );
          return fr;
        }
      }

      for( Class iface: cls.getInterfaces() )
      {
        fr = _field( iface, name );
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

  /**
   * Get a {@link ConstructorRef} to the specified constructor. Typical use:
   * <p>
   * <pre> constructor("java.util.ArrayList", int.class).newInstance(32) </pre>
   *
   * @param fqn    The qualified name of the class to construct
   * @param params A list of parameter types for the constructor
   * @return A reference to the constructor or null if not found
   */
  public static ConstructorRef constructor( String fqn, Class<?>... params )
  {
    return constructor( type( fqn ), params );
  }

  /**
   * Get a {@link ConstructorRef} to the specified constructor. Typical use:
   * <p>
   * <pre> constructor(ArrayList.class, int.class).newInstance(32) </pre>
   *
   * @param cls    The class to construct
   * @param params A list of parameter types for the constructor
   * @return A reference to the constructor or null if not found
   */
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

      for( Class iface: cls.getInterfaces() )
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
    Field overrideField = field( FakeAccessibleObject.class, "override" )._field;
    try
    {
      NecessaryEvilUtil.getUnsafe().putObjectVolatile( m, NecessaryEvilUtil.getUnsafe().objectFieldOffset( overrideField ), true );
    }
    catch( Exception e )
    {
      throw ManExceptionUtil.unchecked( e );
    }
  }

// This class mirrors the layout/structure of the AccessibleObject class so we can get the offset of 'override'
  @SuppressWarnings( "unused" )
  private static class FakeAccessibleObject
  {
    static final private String ACCESS_PERMISSION = "";
    boolean override;
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

    public Object invokeSuper( Object... args )
    {
      try
      {
        MethodHandle superMethod = MethodHandles.lookup().findSpecial(
          _receiver.getClass().getSuperclass(), _method.getName(),
          MethodType.methodType( _method.getReturnType(), _method.getParameterTypes() ), _receiver.getClass() );
        return superMethod.bindTo( _receiver ).invokeExact( args );
      }
      catch( Throwable t )
      {
        throw ManExceptionUtil.unchecked( t );
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

    @SuppressWarnings( "unused" )
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
    return addMethodToCache( cls, m, m.getName() );
  }
  private static MethodRef addMethodToCache( Class cls, Method m, String name )
  {
    setAccessible( m );
    addRawMethodToCache( cls, m, name );
    return new MethodRef( m );
  }

  private static void addRawMethodToCache( Class cls, Method m, String name )
  {
    ConcurrentMap<String, ConcurrentHashSet<Method>> methodsByName = _methodsByName.get( cls );
    if( methodsByName == null )
    {
      _methodsByName.put( cls, methodsByName = new ConcurrentHashMap<>() );
    }
    ConcurrentHashSet<Method> methods = methodsByName.get( name );
    if( methods == null )
    {
      methodsByName.put( name, methods = new ConcurrentHashSet<>( 2 ) );
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

  private static MethodRef getMethodFromCacheUsingNameOnly( Class cls, String name )
  {
    Method m = getRawMethodFromCacheUsingNameOnly( cls, name );
    if( m != null )
    {
      return new MethodRef( m );
    }
    return null;
  }

  private static Method getRawMethodFromCacheUsingNameOnly( Class cls, String name )
  {
    ConcurrentMap<String, ConcurrentHashSet<Method>> methodsByName = _methodsByName.get( cls );
    if( methodsByName != null )
    {
      ConcurrentHashSet<Method> methods = methodsByName.get( name );
      if( methods != null )
      {
        return methods.iterator().next();
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
      for( Constructor m: constructors )
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
   * Force class with name {@code fqn} to be loaded by {@code parentLoader}. Facilitates the case where a class
   * must be declared in a package defined in a parent class loader in order to subclass and use package-local
   * features defined there.
   * <p>
   * Note {@code fqn}'s natural class loader must have the {@code parentLoader} in its chain of parent loaders.
   * Also be certain {@code fqn} is not already loaded by its natural loader, otherwise {@link LinkageError}s
   * will result.
   *
   * @param fqn           The qualified name of the class to load
   * @param content       The location of the class resource.  With Java 8 this can be {@code wouldBeLoader.getResource(className)}.
   *                      But with Java 9 and later the JPMS strictly prohibits a package from existing in two loaders,
   *                      therefore the class file must be placed in a different package, perhaps prefixed with a
   *                      suitably named root package, otherwise the VM will throw a {@code LayerInstantiationException}
   *                      when your application loads, before any of your code executes.
   * @param wouldBeLoader The class loader that would naturally load {@code fqn}, must have {@code parentLoader} in its
   *                      parent loader chain
   * @param parentLoader  The class loader to load the class in, must be in the parent chain of {@code wouldBeLoader}
   */
  @SuppressWarnings( "unused" )
  public static void preloadClassIntoParentLoader( String fqn, URI content, ClassLoader wouldBeLoader, ClassLoader parentLoader )
  {
    if( null != method( parentLoader, "findLoadedClass", String.class ).invoke( fqn ) )
    {
      // already loaded
      return;
    }

    try
    {
      byte[] bytes = Files.readAllBytes( Paths.get( content ) );
      method( parentLoader, "defineClass", byte[].class, int.class, int.class ).invoke( bytes, 0, bytes.length );
    }
    catch( IOException e )
    {
      throw new RuntimeException( e );
    }
  }

  /**
   * Use to access live methods and fields with possible null return value if not found
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

  static class ClassContextSecurityManager extends SecurityManager
  {
    /**
     * Expose getClassContext() to enable finding classloaders in the stack trace
     */
    @Override
    protected Class[] getClassContext()
    {
      return super.getClassContext();
    }
  }

  public static MethodRef structuralMethod( Class target, Class structIface, String name, Class... params )
  {
    MethodRef structMethod = method( structIface, name, params );
    if( structMethod != null )
    {
      Method bestMethod = findBestMethod( structMethod.getMethod(), target );
      return bestMethod == null ? null : new MethodRef( bestMethod );
    }
    return null;
  }

  public static LiveMethodRef structuralMethod( Object receiver, Class structIface, String name, Class... params )
  {
    MethodRef structMethod = method( structIface, name, params );
    if( structMethod != null )
    {
      Method bestMethod = findBestMethod( structMethod.getMethod(), receiver.getClass() );
      return bestMethod == null ? null : new LiveMethodRef( bestMethod, receiver );
    }
    return null;
  }

  public static Object structuralCall( Method structMethod, Object receiver, Object... args )
  {
    Method bestMethod = findBestMethod( structMethod, receiver.getClass() );
    if( bestMethod == null )
    {
      throw new RuntimeException( "Receiver type '" + receiver.getClass().getTypeName() +
        "' does not implement a method structurally compatible with method: " +  structMethod );
    }

    try
    {
      return bestMethod.invoke( receiver, args );
    }
    catch( Throwable t )
    {
      throw ManExceptionUtil.unchecked( t );
    }

  }

  private static Method findBestMethod( Method structMethod, Class receiverClass )
  {
    ConcurrentMap<Class, Method> map = _structuralCall.computeIfAbsent( structMethod, cls -> new ConcurrentWeakHashMap<>() );
    return map.computeIfAbsent( receiverClass, rc -> {
      List<Method> methods = new ArrayList<>();
      for( Method m : rc.getMethods() )
      {
        if( m.getName().equals( structMethod.getName() ) )
        {
          methods.add( m );
        }
      }
      List<MethodScore> methodScores = MethodScorer.instance()
        .scoreMethods( methods, Arrays.asList( structMethod.getParameterTypes() ), structMethod.getReturnType() );
      for( MethodScore score : methodScores )
      {
        if( !score.isErrant() )
        {
          Method method = score.getMethod();
          setAccessible( method );
          return method;
        }
      }
      return null;
    } );
  }

  //## not necessary (until Unsafe goes away), using Unsafe.putObjectVolatile() to set 'override' directly
//
//  private static void openPackage( String fqn, ClassLoader cl )
//  {
//    if( JreUtil.isJava8() || _openPackages.containsKey( fqn ) )
//    {
//      return;
//    }
//
//    int iDot = fqn.lastIndexOf( '.' );
//    if( iDot < 0 )
//    {
//      return;
//    }
//
//    String pkg = fqn.substring( 0, iDot );
//    cl = cl == null ? ReflectUtil.class.getClassLoader() : cl;
//    LiveFieldRef packageToModule = WithNull.field( cl, "packageToModule" );
//    if( packageToModule != null )
//    {
//      Object loadedModule = ((Map)packageToModule.get()).get( pkg );
//      if( loadedModule != null )
//      {
//        if( method( loadedModule, "loader" ).invoke() == cl )
//        {
//          String moduleName = (String)method( loadedModule, "name" ).invoke();
//          //noinspection unchecked
//          Object module = ((Optional)ReflectUtil.method( ReflectUtil.method( "java.lang.ModuleLayer", "boot" ).invokeStatic(), "findModule", String.class ).invoke( moduleName )).orElse( null );
//          if( module != null )
//          {
//            Class<?> classModule = ReflectUtil.type( "java.lang.Module" );
//            ReflectUtil.MethodRef addExportsOrOpens = method( classModule, "implAddExportsOrOpens", String.class, classModule, boolean.class, boolean.class );
//            //noinspection ConstantConditions
//            Object /*Module*/ manifoldModule = method( Class.class, "getModule" ).invoke( ReflectUtil.class );
//            //noinspection ConstantConditions
//            addExportsOrOpens.invoke( module, pkg, manifoldModule, true, true );
//            _openPackages.put( pkg, true );
//            return;
//          }
//        }
//      }
//    }
//    _openPackages.put( pkg, false );
//  }
}
