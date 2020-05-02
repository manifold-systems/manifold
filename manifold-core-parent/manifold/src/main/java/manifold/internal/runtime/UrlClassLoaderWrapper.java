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

package manifold.internal.runtime;

import java.io.File;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
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
import manifold.util.ReflectUtil.LiveMethodRef;

/**
 */
public class UrlClassLoaderWrapper
{
  private static final Set<Integer> VISITED_LOADER_IDS = new HashSet<>();
  private final ClassLoader _loader;

  private final LiveMethodRef _getURLs;
  private final LiveMethodRef _addUrl;

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

  @SuppressWarnings("unused")
  public static boolean canWrap( ClassLoader loader )
  {
    LiveMethodRef getURLs = getURLsMethod( loader );
    if( getURLs != null )
    {
      LiveMethodRef addUrl = ReflectUtil.WithNull.method( getURLs.getReceiver(), "addURL|addUrl", URL.class );
      return addUrl != null;
    }
    return false;
  }

  public static UrlClassLoaderWrapper wrap( ClassLoader loader )
  {
    LiveMethodRef getURLs = getURLsMethod( loader );
    if( getURLs != null )
    {
      LiveMethodRef addUrl = ReflectUtil.WithNull.method( getURLs.getReceiver(), "addURL|addUrl", URL.class );
      if( addUrl != null )
      {
        return new UrlClassLoaderWrapper( loader, getURLs, addUrl );
      }
    }
    return null;
  }

  private static LiveMethodRef getURLsMethod( Object receiver )
  {
    LiveMethodRef getURLs = ReflectUtil.WithNull.methodWithReturn( receiver, "getURLs|getUrls", URL[].class );
    if( getURLs == null )
    {
      getURLs = ReflectUtil.WithNull.methodWithReturn( receiver, "getURLs|getUrls", List.class );
      if( getURLs == null && receiver instanceof ClassLoader )
      {
        ReflectUtil.LiveFieldRef ucpField = ReflectUtil.WithNull.field( receiver, "ucp" );
        if( ucpField != null )
        {
          Object ucp = ucpField.get();
          if( ucp != null )
          {
            getURLs = getURLsMethod( ucp );
          }
        }
      }
    }
    return getURLs;
  }

  private UrlClassLoaderWrapper( ClassLoader loader, LiveMethodRef getURLs, LiveMethodRef addUrl )
  {
    _loader = loader;
    _getURLs = getURLs;
    _addUrl = addUrl;
  }

  public ClassLoader getLoader()
  {
    return _loader;
  }

  void addURL( URL url )
  {
    try
    {
      _addUrl.invoke( url );
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
        Class<?> moduleReaderClass = ReflectUtil.type( "java.lang.module.ModuleReader" );
        ManModuleReader wrapper = new ManModuleReader( reader, ReflectUtil.field( _loader, "ucp" ).get() );
        Object/*ModuleReader*/ proxy = Proxy.newProxyInstance( moduleReaderClass.getClassLoader(), new Class<?>[]{moduleReaderClass},
          new ManModuleReaderInvocationHandler( wrapper ) );
        //noinspection unchecked
        moduleToReader.put( mr, proxy );
      }
    }
  }

  private static class ManModuleReaderInvocationHandler implements InvocationHandler
  {
    private final Object /*ManModuleReader*/ _wrapper;

    private ManModuleReaderInvocationHandler( Object /*ManModuleReader*/ wrapper )
    {
      _wrapper = wrapper;
    }

    @Override
    public Object invoke( Object proxy, Method method, Object[] args )
    {
      return ReflectUtil.method( _wrapper, method.getName(), method.getParameterTypes() ).invoke( args );
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
      Object urls = _getURLs.invoke();
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
}

