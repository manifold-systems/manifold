package manifold.internal.runtime;

import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import manifold.util.concurrent.ConcurrentHashSet;

/**
*/
public class UrlClassLoaderWrapper
{
  private static final Set<Integer> VISITED_LOADER_IDS = new ConcurrentHashSet<Integer>();
  private final ClassLoader _loader;
  private final Method _getURLs;
  private final Method _addUrl;

  static UrlClassLoaderWrapper wrapIfNotAlreadyVisited( ClassLoader loader ) {
    int loaderId = System.identityHashCode( loader );
    if( VISITED_LOADER_IDS.contains( loaderId ) ) {
      // Already visited
      return null;
    }
    VISITED_LOADER_IDS.add( loaderId );
    UrlClassLoaderWrapper wrapped = wrap( loader );
    if( wrapped == null ) {
      throw new IllegalStateException( "Could not wrap loader: " + loader.getClass().getName() );
    }
    return wrapped;
  }

  public static UrlClassLoaderWrapper wrap( ClassLoader loader ) {
    Method getURLs = findMethod( loader.getClass(), "getURLs", new Class[0], List.class, URL[].class );
    if( getURLs != null ) {
      Method addUrl = findMethod( loader.getClass(), "addUrl", new Class[] {URL.class}, void.class );
      if( addUrl != null ) {
        return new UrlClassLoaderWrapper( loader, getURLs, addUrl );
      }
    }
    return null;
  }

  public static boolean canWrap( ClassLoader loader ) {
    if( loader == null ) {
      return false;
    }
    Method getURLs = findMethod( loader.getClass(), "getURLs", new Class[0], List.class, URL[].class );
    if( getURLs != null ) {
      Method addUrl = findMethod( loader.getClass(), "addUrl", new Class[] {URL.class}, void.class );
      if( addUrl != null ) {
        return true;
      }
    }
    return false;
  }

  private static Method findMethod( Class cls, String methodName, Class[] paramTypes, Class... returnType ) {
    outer: for( Method m: cls.getDeclaredMethods() ) {
      if( m.getName().equalsIgnoreCase( methodName ) ) {
        Class<?>[] types = m.getParameterTypes();
        if( types.length == paramTypes.length ) {
          for( int i = 0; i < paramTypes.length; i++ ) {
            if( !paramTypes[i].equals( types[i] ) ) {
              continue outer;
            }
          }
          for( Class t: returnType ) {
            if( m.getReturnType().equals( t ) ) {
              m.setAccessible( true );
              return m;
            }
          }
        }
      }
    }
    return cls.getSuperclass() != null ? findMethod( cls.getSuperclass(), methodName, paramTypes, returnType ) : null;
  }

  private UrlClassLoaderWrapper( ClassLoader loader, Method getURLs, Method addUrl ) {
    _loader = loader;
    _getURLs = getURLs;
    _addUrl = addUrl;
  }

  public ClassLoader getLoader() {
    return _loader;
  }

  public void addURL(URL url) {
    try {
      _addUrl.invoke( _loader, new Object[] {url} );
    }
    catch( Exception e ) {
      throw new RuntimeException( e );
    }
  }

  public List<URL> getURLs() {
    if( _loader instanceof URLClassLoader ) {
      URL[] urls = ((URLClassLoader)_loader).getURLs();
      return urls == null ? Collections.<URL>emptyList() : Arrays.asList( urls );
    }

    try {
      Object urls = _getURLs.invoke( _loader );
      urls = urls == null
             ? Collections.<URL>emptyList()
             : urls.getClass().isArray()
               ? Arrays.asList( (URL[])urls )
               : urls;
      return (List)urls;
    }
    catch( Exception e ) {
      throw new RuntimeException( e );
    }
  }

}
