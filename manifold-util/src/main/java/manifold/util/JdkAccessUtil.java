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

import com.sun.tools.javac.code.Symbol;
import com.sun.tools.javac.code.Symtab;
import com.sun.tools.javac.util.Context;
import com.sun.tools.javac.util.Name;
import com.sun.tools.javac.util.Names;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.*;

import static manifold.util.JreUtil.isJava23orLater;
import static manifold.util.JreUtil.isJava24orLater;

public class JdkAccessUtil
{
  public static Unhelmeted getUnhelmeted()
  {
    return Unhelmeted.getUnhelmeted();
  }

  public static void openModules()
  {
    openModules( true );
  }
  public static void openModules( boolean fullJdk )
  {
    muteJava9Warning();
    _openModules( fullJdk );
  }

  public static void muteJava9Warning()
  {
    // runtime
    muteJava9Warning( JdkAccessUtil.class.getClassLoader() );
    // compile-time
    muteJava9Warning( Thread.currentThread().getContextClassLoader() );
  }

  // Disable Java 9 warnings re "An illegal reflective access operation has occurred"
  public static void muteJava9Warning( ClassLoader cl )
  {
    if( JreUtil.isJava8() || JreUtil.isJava17orLater() )
    {
      // warning is specific to JDK 11
      return;
    }

    try
    {
      Class cls = Class.forName( "jdk.internal.module.IllegalAccessLogger", false, cl );
      Field logger = cls.getDeclaredField( "logger" );
      getUnhelmeted().putObjectVolatile( cls, getUnhelmeted().staticFieldOffset( logger ), null );
    }
    catch( Throwable ignore )
    {
    }
  }

  private static void _openModules( boolean fullJdk )
  {
    if( JreUtil.isJava8() )
    {
      return;
    }

    if( useInternalUnsafe() )
    {
      // requires:  --add-exports=java.base/jdk.internal.access=ALL-UNNAMED (or "manifold-util" in multi-module mode)
      try
      {
        Class<?> tenderizeClass = Class.forName( "manifold.util.Tenderizer" );
        Object instance = tenderizeClass.getDeclaredField( "INSTANCE" ).get( null );
        tenderizeClass.getDeclaredMethod( "tenderize" ).invoke( instance );
      }
      catch( Throwable t )
      {
        throw new RuntimeException( t );
      }
    }

    try
    {
      Class<?> classModule = ReflectUtil.type( "java.lang.Module" );
      ReflectUtil.MethodRef addExportsOrOpens = ReflectUtil.method( classModule, "implAddExportsOrOpens", String.class, classModule, boolean.class, boolean.class );

      //
      // Module: manifold jars
      //
      Object /*Module*/ thisModule = ReflectUtil.method( Class.class, "getModule" ).invoke( JdkAccessUtil.class );
      Object /*Module*/ javaBase = ReflectUtil.method( Class.class, "getModule" ).invoke( String.class );
      addExportsOrOpens.invoke( javaBase, "jdk.internal.misc", thisModule, true, true );
      addExportsOrOpens.invoke( javaBase, "java.lang", thisModule, true, true );

      Object /*Module*/ everyoneModule = ReflectUtil.field( "java.lang.Module", "EVERYONE_MODULE" ).getStatic();

      // Open select packages in java.base module for reflective access
      openRuntimeModules( addExportsOrOpens, everyoneModule );

      if( fullJdk )
      {
        // Open select packages in jdk.compiler for reflective access
        openCompilerModules( addExportsOrOpens, everyoneModule );
      }
    }
    catch( Throwable e )
    {
      throw new RuntimeException( "Error initializing Manifold", e );
    }
  }

  private static void openRuntimeModules( ReflectUtil.MethodRef addExportsOrOpens, Object manifoldModule )
  {
    //
    // Module: java.base
    //
    Object /*Module*/ javaBaseModule = ReflectUtil.method( Class.class, "getModule" ).invoke( String.class );
//    addExportsOrOpens.invoke( javaBaseModule, "jdk.internal.loader", manifoldModule, true, true );
//    addExportsOrOpens.invoke( javaBaseModule, "jdk.internal.misc", manifoldModule, true, true );
//    addExportsOrOpens.invoke( javaBaseModule, "jdk.internal.module", manifoldModule, true, true );
//    addExportsOrOpens.invoke( javaBaseModule, "jdk.internal.vm", manifoldModule, true, true );
//    addExportsOrOpens.invoke( javaBaseModule, "jdk.internal.vm.annotation", manifoldModule, true, true );
//    addExportsOrOpens.invoke( javaBaseModule, "java.lang", manifoldModule, true, true );
//    addExportsOrOpens.invoke( javaBaseModule, "java.lang.invoke", manifoldModule, true, true );
//    addExportsOrOpens.invoke( javaBaseModule, "java.lang.module", manifoldModule, true, true );
//    addExportsOrOpens.invoke( javaBaseModule, "java.lang.reflect", manifoldModule, true, true ); // for jailbreak
//    addExportsOrOpens.invoke( javaBaseModule, "java.net", manifoldModule, true, true );
    //noinspection unchecked
    Set<String> packages = (Set<String>)ReflectUtil.method( javaBaseModule, "getPackages" ).invoke();
    for( String pkg : packages )
    {
      addExportsOrOpens.invoke( javaBaseModule, pkg, manifoldModule, true, true );
    }

    //
    // Module: java.desktop (needed for testing manifold IJ plugin)
    //
    Class<?> Desktop = ReflectUtil.type( "java.awt.Desktop", true );
    if( Desktop == null )
    {
      // Warn and continue
      //System.out.println( "\nWARNING: Failed to find class 'java.awt.Desktop'\n" );
      return;
    }
    Object /*Module*/ javaDesktop = ReflectUtil.method( Class.class, "getModule" ).invoke( Desktop );
    addExportsOrOpens.invoke( javaDesktop, "sun.awt", manifoldModule, true, true );
  }

  private static void openCompilerModules( ReflectUtil.MethodRef addExportsOrOpens, Object manifoldModule )
  {
    //
    // Module: jdk.compiler
    //
    Object /*Module*/ jdkCompilerModule = ReflectUtil.method( Class.class, "getModule" )
      .invoke( ReflectUtil.type( "com.sun.tools.javac.code.Symbol", true ) );
//    addExportsOrOpens.invoke( jdkCompilerModule, "com.sun.tools.javac.api", manifoldModule, true, true );
//    addExportsOrOpens.invoke( jdkCompilerModule, "com.sun.tools.javac.code", manifoldModule, true, true );
//    addExportsOrOpens.invoke( jdkCompilerModule, "com.sun.tools.javac.comp", manifoldModule, true, true );
//    addExportsOrOpens.invoke( jdkCompilerModule, "com.sun.tools.javac.file", manifoldModule, true, true );
//    addExportsOrOpens.invoke( jdkCompilerModule, "com.sun.tools.javac.jvm", manifoldModule, true, true );
//    addExportsOrOpens.invoke( jdkCompilerModule, "com.sun.tools.javac.main", manifoldModule, true, true );
//    addExportsOrOpens.invoke( jdkCompilerModule, "com.sun.tools.javac.model", manifoldModule, true, true );
//    addExportsOrOpens.invoke( jdkCompilerModule, "com.sun.tools.javac.parser", manifoldModule, true, true );
//    addExportsOrOpens.invoke( jdkCompilerModule, "com.sun.tools.javac.platform", manifoldModule, true, true );
//    addExportsOrOpens.invoke( jdkCompilerModule, "com.sun.tools.javac.processing", manifoldModule, true, true );
//    addExportsOrOpens.invoke( jdkCompilerModule, "com.sun.tools.javac.resources", manifoldModule, true, true );
//    addExportsOrOpens.invoke( jdkCompilerModule, "com.sun.tools.javac.tree", manifoldModule, true, true );
//    addExportsOrOpens.invoke( jdkCompilerModule, "com.sun.tools.javac.util", manifoldModule, true, true );
    //noinspection unchecked
    Set<String> packages = (Set<String>)ReflectUtil.method( jdkCompilerModule, "getPackages" ).invoke();
    for( String pkg : packages )
    {
      addExportsOrOpens.invoke( jdkCompilerModule, pkg, manifoldModule, true, true );
    }

    //
    // Module: jdk.javadoc
    //
    Class<?> HtmlDoclet = ReflectUtil.type( "jdk.javadoc.internal.doclets.formats.html.HtmlDoclet", true );
    if( HtmlDoclet == null )
    {
      // Warn and continue
      //System.out.println( "\nWARNING: Failed to find class 'jdk.javadoc.internal.doclets.formats.html.HtmlDoclet'\n" );
      return;
    }
    Object /*Module*/ jdkJavadoc = ReflectUtil.method( Class.class, "getModule" ).invoke( HtmlDoclet );
    addExportsOrOpens.invoke( jdkJavadoc, "jdk.javadoc.internal.doclets.formats.html", manifoldModule, true, true );
    addExportsOrOpens.invoke( jdkJavadoc, "jdk.javadoc.internal.tool", manifoldModule, true, true );
    if( !JreUtil.isJava13orLater() )
    {
      // `com.sun.tools.doclets.standard.Standard` and `com.sun.tools.javadoc.main.Main` are removed in JDK 13
      addExportsOrOpens.invoke( jdkJavadoc, "com.sun.tools.javadoc.main", manifoldModule, true, true );
      addExportsOrOpens.invoke( jdkJavadoc, "com.sun.tools.doclets.standard", manifoldModule, true, true );
    }
  }

  public static void openModule( Context context, String moduleName )
  {
    try
    {
      Symbol moduleToOpen = (Symbol)ReflectUtil.method( Symtab.instance( context ), "getModule", Name.class )
        .invoke( Names.instance( context ).fromString( moduleName ) );

      if( moduleToOpen == null )
      {
        // not modular java 9+
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
            //System.err.println( "PACKAGE: " + pkg );
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

  private static Boolean useInternalUnsafe;
  static boolean useInternalUnsafe()
  {
    if( useInternalUnsafe != null )
    {
      return useInternalUnsafe;
    }

    if( !isJava23orLater() )
    {
      // use sun.misc.Unsafe for earlier JDK versions
      return useInternalUnsafe = false;
    }

    try
    {
      Method getModule = Class.class.getMethod( "getModule" );
      Object /*Module*/ manifoldUtil = getModule.invoke( JdkAccessUtil.class );
      Object /*Module*/ javaBase = getModule.invoke( String.class );
      boolean isJdkInternalAccessExported = (boolean)javaBase.getClass().getMethod( "isExported", String.class, javaBase.getClass() )
        .invoke( javaBase, "jdk.internal.access", manifoldUtil );
      if( !isJdkInternalAccessExported )
      {
        isJdkInternalAccessExported = tryExportJdkInternalAccess( javaBase, manifoldUtil );
      }
      if( isJdkInternalAccessExported )
      {
        // prefer internal Unsafe when jdk.internal.access is exported
        return useInternalUnsafe = true;
      }
    }
    catch( Throwable e )
    {
      throw ManExceptionUtil.unchecked( e );
    }

    try
    {
      Field theUnsafe = sun.misc.Unsafe.class.getDeclaredField( "theUnsafe" );
      theUnsafe.setAccessible( true );
      sun.misc.Unsafe unsafe = (sun.misc.Unsafe)theUnsafe.get( null );
      long offset = unsafe.staticFieldOffset( JdkAccessUtil.class.getDeclaredField( "useInternalUnsafe" ) );
      if( offset >= 0 )
      {
        // above staticFieldOffset() call may trigger a warning if `--sun-misc-unsafe-memory-access=allow` is not on cmd line
        logWarningInstructions();
        return useInternalUnsafe = false;
      }
    }
    catch( Throwable ignore )
    {
    }
    // whelp, sun.misc.Unsafe no longer supports features we use. However, since "jdk.internal.access" was not exported
    // on the command line, the "jdk.internal.misc.Unsafe" can't be used. Print a message with a remedy.
    logErrorInstructions();
    useInternalUnsafe = true;
    throw new ManifoldInitException();
  }

  // check if 'java.lang' is open (such is the case inside IntelliJ) and if so, export `jdk.internal.access` dynamically
  private static boolean tryExportJdkInternalAccess( Object javaBase, Object manifoldUtil ) throws IllegalAccessException, InvocationTargetException, NoSuchMethodException
  {
    boolean isJavaLangOpen = (boolean)javaBase.getClass().getMethod( "isOpen", String.class, javaBase.getClass() )
      .invoke( javaBase, "java.lang", manifoldUtil );
    if( !isJavaLangOpen )
    {
      // `java.lang` is not open to our module from `java.base`
      return false;
    }
    // `java.lang` is open, we can use that to dynamically export `jdk.internal.access`
    Method getDeclaredMethods0 = Class.class.getDeclaredMethod( "getDeclaredMethods0", boolean.class );
    getDeclaredMethods0.setAccessible( true );
    Method[] methods = (Method[])getDeclaredMethods0.invoke( javaBase.getClass(), false );
    for( Method method : methods )
    {
      if( method.getName().equals( "implAddExportsOrOpens" ) && method.getParameterTypes().length == 4 )
      {
        // dynamically export java.base/jdk.internal.access
        method.setAccessible( true );
        method.invoke( javaBase, "jdk.internal.access", manifoldUtil, false, true );
        return true;
      }
    }
    throw new IllegalAccessException( "Should have exported jdk.internal.access" );
  }

  private static void logErrorInstructions()
  {
    System.err.println( "#  Manifold requires the following Java process option:\n" +
                        "#      --add-exports=java.base/jdk.internal.access=ALL-UNNAMED\n" +
                        "#  If you are using the Java Platform Module System (JPMS), use this instead:\n" +
                        "#      --add-exports=java.base/jdk.internal.access=manifold.util\n" );
  }

  private static void logWarningInstructions()
  {
    if( !isJava24orLater() )
    {
      // JDKs prior to 24 do not print a warning message concerning use of Unsafe
      return;
    }

    List<String> values = java.lang.management.ManagementFactory.getRuntimeMXBean().getInputArguments();
    if( values.stream().noneMatch( arg -> arg.contains( "--sun-misc-unsafe-memory-access=allow" ) ) )
    {
      // this message prints immediately after JDK's Unsafe warning message
      System.err.println( "# To eliminate the warning, configure Manifold to use an alternative to sun.misc.Unsafe:\n" +
                          "#      --add-exports=java.base/jdk.internal.access=ALL-UNNAMED\n" +
                          "# If using JPMS with named modules:\n" +
                          "#      --add-exports=java.base/jdk.internal.access=manifold.util\n" );
    }
  }
}
