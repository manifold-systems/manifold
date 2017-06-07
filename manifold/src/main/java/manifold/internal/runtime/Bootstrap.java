/*
 * Copyright 2014 Guidewire Software, Inc.
 */

package manifold.internal.runtime;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Hashtable;
import manifold.internal.host.ManifoldHost;

/**
 */
public class Bootstrap
{
  public static final String GOSU_CLASS_PROTOCOL = "gosuclass";
  private static final String PROTOCOL_PACKAGE = "manifold.internal.runtime.protocols";

  //## todo: set this back to null after we upgrade to tomcat8 and reapply the changes to use a single ClassLoader monitor as the type sys lock
  private static Boolean CAN_WRAP = Boolean.FALSE; //= null;

  private static void setupLoaderChainWithGosuUrl( ClassLoader loader ) {
    UrlClassLoaderWrapper wrapped = UrlClassLoaderWrapper.wrapIfNotAlreadyVisited( loader );
    if( wrapped == null ) {
      return;
    }
    addGosuClassUrl( wrapped );
    if( canWrapChain() ) {
      if( loader != ClassLoader.getSystemClassLoader() ) { // we don't bother messing with any loaders above the system loader e.g., ExtClassLoader
        loader = loader.getParent();
        if( loader != null ) {
          setupLoaderChainWithGosuUrl( loader );
        }
      }
    }
  }

  /*
    We don't currently wrap the chain of loaders for WebSphere or WebLogic or JBoss
    because they use "module" class loaders that are not URLClassLoader-like.  We
    can maybe someday handle them seperately.

    IBM class loader chain:
    ~~~~~~~~~~~~~~~~~~~~~~~
    com.guidewire.pl.system.gosu.GosuPluginContainer ->
       com.guidewire.pl.system.integration.plugins.PluginContainer ->
         com.guidewire.pl.system.integration.plugins.SharedPluginContainer ->
           com.guidewire.pl.system.integration.plugins.PluginContainer ->

             [weblogic.utils.classloaders.ChangeAwareClassLoader ->
               weblogic.utils.classloaders.FilteringClassLoader ->
                 weblogic.utils.classloaders.GenericClassLoader]* ->

                   sun.misc.Launcher$AppClassLoader ->
                     sun.misc.Launcher$ExtClassLoader ->
                       <null>

    WebLogic class loader chain:
    ~~~~~~~~~~~~~~~~~~~~~~~~~~~~
    com.guidewire.pl.system.gosu.GosuPluginContainer ->
       com.guidewire.pl.system.integration.plugins.PluginContainer ->
         com.guidewire.pl.system.integration.plugins.SharedPluginContainer ->
            com.guidewire.pl.system.integration.plugins.PluginContainer ->

              org.jboss.modules.ModuleClassLoader ->

                sun.misc.Launcher$AppClassLoader ->
                  sun.misc.Launcher$ExtClassLoader ->
                    <null>

   */
  private static boolean canWrapChain( ClassLoader loader ) {
    if( loader == null ) {
      return false;
    }
    UrlClassLoaderWrapper wrapped = UrlClassLoaderWrapper.wrap( loader );
    boolean bSysLoader = loader == ClassLoader.getSystemClassLoader();
    if( bSysLoader ) {
      return wrapped != null;
    }
    loader = loader.getParent();
    return wrapped != null && canWrapChain( loader );
  }

  private static void addGosuClassUrl( UrlClassLoaderWrapper urlLoader ) {
    try {
      URL url = makeUrl( urlLoader.getLoader() );
      if( !urlLoader.getURLs().contains( url ) ) {
        urlLoader.addURL( url );
      }
    }
    catch( MalformedURLException e ) {
      throw new RuntimeException( e );
    }
  }

  private static URL makeUrl( ClassLoader loader ) throws MalformedURLException {
    int loaderAddress = System.identityHashCode( loader );
    String spec = GOSU_CLASS_PROTOCOL + "://" + loaderAddress + "/";
    URL url;
    try {
      url = new URL( null, spec );
    }
    catch( Exception e ) {
      // If our Handler class is not in the system loader and not accessible within the Caller's
      // classloader from the URL constructor (3 activation records deep), then our Handler class
      // is not loadable by the URL class, but the honey badger doesn't really care; it gets
      // what it wants.
      addOurProtocolHandler();
      url = new URL( null, spec );
    }
    return url;
  }

  public static void addOurProtocolHandler() {
    try {
      Field field = URL.class.getDeclaredField( "handlers" );
      field.setAccessible( true );
      Method put = Hashtable.class.getMethod( "put", Object.class, Object.class );
      Field instanceField = Class.forName( "manifold.internal.runtime.protocols.Handler" ).getField( "INSTANCE" );
      Object handler = instanceField.get( null );
      put.invoke( field.get( null ), "gosuclass", handler );
    } catch (Exception e) {
      throw new IllegalStateException("Failed to configure gosu protocol handler", e);
    }
  }

  private static void removeOurProtocolHandler() {
    try {
      Field field = URL.class.getDeclaredField( "handlers" );
      field.setAccessible( true );
      Method remove = Hashtable.class.getMethod( "remove", Object.class );
      remove.invoke( field.get( null ), "gosuclass" );
    } catch (Exception e) {
      throw new IllegalStateException("Failed to cleanup gosu protocol handler", e);
    }
  }

  private static boolean addOurProtocolPackage() {
    // XXX: Do not add protocol package since OSGi implementation of URLStreamFactory
    // first delegates to those and only then calls service from Service Registry
    String strProtocolProp = "java.protocol.handler.pkgs";
    String protocols = PROTOCOL_PACKAGE;
    String oldProp = System.getProperty( strProtocolProp );
    if( oldProp != null ) {
      if( oldProp.contains( PROTOCOL_PACKAGE ) ) {
        return false;
      }
      protocols += '|' + oldProp;
    }
    System.setProperty( strProtocolProp, protocols );
    return true;
  }

  private static void removeOurProtocolPackage() {
    String strProtocolProp = "java.protocol.handler.pkgs";
    String protocols = System.getProperty( strProtocolProp );
    if( protocols != null ) {
      // Remove our protocol from the list
      protocols = protocols.replace( PROTOCOL_PACKAGE + '|' , "" );
      System.setProperty( strProtocolProp, protocols );
    }
  }

  //!! Do Not Rename or Remove this method.  Calls to it are generated by the compiler and annotation processor.
  public synchronized static boolean init() {
    if( addOurProtocolPackage() ) {
      ManifoldHost.bootstrap();
    }
    ClassLoader loader = ManifoldHost.getActualClassLoader();
    if( loader != null )
    {
      setupLoaderChainWithGosuUrl( loader );
      return true;
    }
    return false;
  }

  public static boolean canWrapChain() {
    return CAN_WRAP == null ? CAN_WRAP = canWrapChain( ManifoldHost.getActualClassLoader() ) : CAN_WRAP;
  }

  public synchronized static void cleanup() {
    removeOurProtocolPackage();
    // XXX: We can't remove URL from classloader easily.
    //removeGosuClassProtocolToClasspath();
    removeOurProtocolHandler();
  }

}
