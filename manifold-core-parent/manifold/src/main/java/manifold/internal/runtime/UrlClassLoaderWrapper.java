package manifold.internal.runtime;

import java.io.File;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import manifold.util.JreUtil;
import manifold.util.ReflectUtil;

/**
 */
public class UrlClassLoaderWrapper
{
  private static final Set<Integer> VISITED_LOADER_IDS = new HashSet<>();
  private final ClassLoader _loader;

  private final MethodAndReceiver _getURLs;
  private final MethodAndReceiver _addUrl;

  static UrlClassLoaderWrapper wrapIfNotAlreadyVisited( ClassLoader loader )
  {
    int loaderId = System.identityHashCode( loader );
    if( VISITED_LOADER_IDS.contains( loaderId ) )
    {
      // Already visited
      return null;
    }
    VISITED_LOADER_IDS.add( loaderId );
    UrlClassLoaderWrapper wrapped = wrap( loader );
    if( wrapped == null )
    {
      throw new IllegalStateException( "Could not wrap loader: " + loader.getClass().getName() );
    }
    return wrapped;
  }

  public static UrlClassLoaderWrapper wrap( ClassLoader loader )
  {
    MethodAndReceiver getURLs = findLoaderMethod( loader, "getURLs", new Class[0], List.class, URL[].class );
    if( getURLs != null )
    {
      MethodAndReceiver addUrl = findLoaderMethod( loader, "addUrl", new Class[] {URL.class}, (Class[])null );
      if( addUrl != null )
      {
        return new UrlClassLoaderWrapper( loader, getURLs, addUrl );
      }
    }
    return null;
  }

  private static MethodAndReceiver findLoaderMethod( ClassLoader cls, String methodName, Class[] paramTypes, Class... returnType )
  {
    Object receiver = cls;
    Method method = findMethod( cls.getClass(), methodName, paramTypes, returnType );
    if( method == null )
    {
      Field ucpField = findField( cls.getClass(), "ucp" );
      if( ucpField != null )
      {
        method = findMethod( ucpField.getType(), methodName, paramTypes, returnType );
        if( method != null )
        {
          try
          {
            ucpField.setAccessible( true );
            receiver = ucpField.get( cls );
          }
          catch( IllegalAccessException e )
          {
            throw new RuntimeException( e );
          }
        }
      }
    }
    if( method != null )
    {
      method.setAccessible( true );
      return new MethodAndReceiver( method, receiver );
    }
    return null;
  }

  private static Method findMethod( Class cls, String methodName, Class[] paramTypes, Class[] returnTypes )
  {
    outer:
    for( Method m : cls.getDeclaredMethods() )
    {
      if( m.getName().equalsIgnoreCase( methodName ) )
      {
        Class<?>[] types = m.getParameterTypes();
        if( types.length == paramTypes.length )
        {
          for( int i = 0; i < paramTypes.length; i++ )
          {
            if( !paramTypes[i].equals( types[i] ) )
            {
              continue outer;
            }
          }
          if( returnTypes == null )
          {
            return m;
          }
          for( Class<?> t : returnTypes )
          {
            if( t.isAssignableFrom( m.getReturnType() ) )
            {
              m.setAccessible( true );
              return m;
            }
          }
        }
      }
    }
    return cls.getSuperclass() != null ? findMethod( cls.getSuperclass(), methodName, paramTypes, returnTypes ) : null;
  }

  private static Field findField( Class<?> cls, String fieldName )
  {
    for( Field f : cls.getDeclaredFields() )
    {
      if( f.getName().equalsIgnoreCase( fieldName ) )
      {
        return f;
      }
    }
    return cls.getSuperclass() != null
           ? findField( cls.getSuperclass(), fieldName )
           : null;
  }

  private UrlClassLoaderWrapper( ClassLoader loader, MethodAndReceiver getURLs, MethodAndReceiver addUrl )
  {
    _loader = loader;
    _getURLs = getURLs;
    _addUrl = addUrl;
  }

  public ClassLoader getLoader()
  {
    return _loader;
  }

  public void addURL( URL url )
  {
    try
    {
      _addUrl._method.invoke( _addUrl._receiver, url );
      if( JreUtil.isJava9Modular_runtime() )
      {
        wrapReaders();
      }
    }
    catch( Exception e )
    {
      throw new RuntimeException( e );
    }
  }

  private void wrapReaders()
  {
    Map/*<ModuleReference, ModuleReader>*/ moduleToReader = (Map)ReflectUtil.field( _loader, "moduleToReader" ).get();
    for( Object mr: moduleToReader.keySet() )
    {
      //noinspection unchecked
      Optional<URI> location = (Optional<URI>)ReflectUtil.method( mr, "location" ).invoke();
      URI uri = location.orElse( null );
      if( uri == null )
      {
        continue;
      }

      //## note: "jmod" files are not supported here because they are currently (2018) supported exclusively at compiler/linker time
      String scheme = uri.getScheme();
      if( scheme.equalsIgnoreCase( "file" ) || scheme.equalsIgnoreCase( "jar" ) )
      {
        Object reader = moduleToReader.get( mr );
        Object/*ManModuleReader*/ wrapper = ReflectUtil.constructor( "manifold.internal.runtime.ManModuleReader", ReflectUtil.type( "java.lang.module.ModuleReader" ), ReflectUtil.type( "jdk.internal.loader.URLClassPath" ) ).newInstance( reader, ReflectUtil.field( _loader, "ucp" ).get() );
        moduleToReader.put( mr, wrapper );
      }
    }
  }

  public List<URL> getURLs()
  {
    if( _loader instanceof URLClassLoader )
    {
      URL[] urls = ((URLClassLoader)_loader).getURLs();
      return urls == null ? Collections.emptyList() : Arrays.asList( urls );
    }

    List<URL> allUrls = new ArrayList<>( getClasspathUrls() );
    if( JreUtil.isJava9Modular_runtime() )
    {
      allUrls.addAll( getModularUrls() );
    }

    return Collections.unmodifiableList( allUrls );
  }

  private Set<URL> getModularUrls()
  {
    //## todo: look at other JRE impls (IBM) to see if they provide a different class loader / field name (other than Oracle's BuiltinClassLoader)

    ReflectUtil.LiveFieldRef nameToModuleField;
    try
    {
      nameToModuleField = ReflectUtil.field( _loader, "nameToModule" );
    }
    catch( Exception e )
    {
      throw new RuntimeException( e );
    }

    Set<URL> modulePath = new HashSet<>();
    Map/*<String, ModuleReference>*/nameToModule = (Map)nameToModuleField.get();
    for( Object mr: nameToModule.values() )
    {
      //noinspection unchecked
      Optional<URI> location = (Optional<URI>)ReflectUtil.method( mr, "location" ).invoke();
      URI uri = location.orElse( null );
      if( uri == null )
      {
        continue;
      }

      //## note: "jmod" files are not supported here because they are currently (2018) supported exclusively at compiler/linker time
      String scheme = uri.getScheme();
      if( scheme.equalsIgnoreCase( "file" ) || scheme.equalsIgnoreCase( "jar" ) )
      {
        try
        {
          modulePath.add( new File( uri ).toURI().toURL() );
        }
        catch( MalformedURLException e )
        {
          throw new RuntimeException( e );
        }
      }
    }
    return modulePath;
  }

  private List<URL> getClasspathUrls()
  {
    try
    {
      Object urls = _getURLs._receiver == null ? null : _getURLs._method.invoke( _getURLs._receiver );
      urls = urls == null
             ? Collections.<URL>emptyList()
             : urls.getClass().isArray()
               ? Arrays.asList( (URL[])urls )
               : urls;
      //noinspection unchecked
      return (List)urls;
    }
    catch( Exception e )
    {
      throw new RuntimeException( e );
    }
  }

  private static class MethodAndReceiver
  {
    Method _method;
    Object _receiver;

    MethodAndReceiver( Method method, Object receiver )
    {
      _method = method;
      _receiver = receiver;
    }
  }
}

