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

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Array;
import java.net.URL;
import manifold.util.ReflectUtil;
import manifold.util.StreamUtil;

/**
 * A complete total and utter hack to inject the manifoldclass protocol into IntelliJ's PluginClassLoader,
 * which does not extend java.net.URLClassLoader, but instead defines its own special UrlClassLoader with
 * its own flavor of "Loader".
 * <p/>
 * Of not is the com.intellij.util.lang.UrlLoader class reflectively constructed here. It is
 * our class and is defined in the Manifold ij plugin.  It resides in the ij package because
 * its super class is package-private, which means it also must load in the same class loader,
 * which is the parent loader of the PluginClassLoader.
 */
public class IjPluginIntegration
{
  static void addUrlToIntelliJPluginClassLoader( ClassLoader cl, URL url )
  {
    if( cl.getClass().getTypeName().equals( "com.intellij.ide.plugins.cl.PluginClassLoader" ) )
    {
      try
      {
        Object classPath = ReflectUtil.method( cl, "getClassPath" ).invoke();
        ReflectUtil.field( classPath, "myCanUseCache" ).set( false );
        Object urlLoader = makeUrlLoader( cl, url );
        ReflectUtil.method( ReflectUtil.field( classPath, "myLoaders" ).get(), "add", Object.class ).invoke( urlLoader );
        ReflectUtil.method( ReflectUtil.field( classPath, "myLoadersMap" ).get(), "put", Object.class, Object.class ).invoke( url, urlLoader );
        //ReflectUtil.method( classPath, "initLoader", URL.class, ReflectUtil.type( "com.intellij.util.lang.Loader" ) ).invoke( url, urlLoader );
      }
      catch( Exception e )
      {
        //## todo: log error
        e.printStackTrace();
      }
    }
  }

  private static Object makeUrlLoader( ClassLoader cl, URL url ) throws IOException
  {
    Class cls = defineClass( cl, "/com/intellij/util/lang/UrlLoader.class" );
    defineClass( cl, "/com/intellij/util/lang/UrlLoader$JavaResource.class" );
    defineClass( cl, "/com/intellij/util/lang/UrlLoader$IjResource.class" );
    //noinspection ConstantConditions
    return ReflectUtil.constructor( cls, URL.class, int.class ).newInstance( url, 0 );
  }

  private static Class defineClass( ClassLoader cl, String fqn ) throws IOException
  {
    InputStream stream = cl.getResourceAsStream( fqn );
    byte[] bytes = StreamUtil.getContent( stream );
    ClassLoader parent = (ClassLoader)Array.get( ReflectUtil.field( cl, "myParents" ).get(), 0 );
    return (Class)ReflectUtil.method( parent, "defineClass", byte[].class, int.class, int.class ).invoke( bytes, 0, bytes.length );
  }

  public static ClassLoader getParent( ClassLoader cl )
  {
    if( cl.getClass().getTypeName().equals( "com.intellij.ide.plugins.cl.PluginClassLoader" ) )
    {
      try
      {
        ClassLoader[] parents = (ClassLoader[])ReflectUtil.field( cl, "myParents" ).get();
        return parents.length > 0 ? parents[0] : null;
      }
      catch( Exception e )
      {
        //## todo: log error
        e.printStackTrace();
      }
    }
    return null;
  }
}
