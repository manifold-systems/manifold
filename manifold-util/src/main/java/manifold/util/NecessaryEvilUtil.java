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

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.HashSet;
import java.util.Set;

import sun.misc.Unsafe;

public class NecessaryEvilUtil
{
  private static Unsafe UNSAFE = null;

  static
  {
    if( JreUtil.isJava12orLater() )
    {
      try
      {
        // Shutdown Oracle's attempt at blacklisting fields and methods from reflection in Java 12
        Class<?> hackClass = Class.forName( "manifold.util.ReflectionHack_12" );
        Method hackReflection = hackClass.getMethod( "hackReflection" );
        hackReflection.invoke( null );
      }
      catch( Throwable e )
      {
        throw new RuntimeException( e );
      }
    }
  }

  public static Unsafe getUnsafe()
  {
    if( UNSAFE != null )
    {
      return UNSAFE;
    }

    try
    {
      Field theUnsafe = Unsafe.class.getDeclaredField( "theUnsafe" );
      theUnsafe.setAccessible( true );
      return UNSAFE = (Unsafe)theUnsafe.get( null );
    }
    catch( Throwable t )
    {
      throw new RuntimeException( "The 'Unsafe' class is not accessible" );
    }
  }

  public static void bypassJava9Security()
  {
    disableJava9IllegalAccessWarning();
    openJavaBase();
  }

  public static void disableJava9IllegalAccessWarning()
  {
    // runtime
    disableJava9IllegalAccessWarning( NecessaryEvilUtil.class.getClassLoader() );
    // compile-time
    disableJava9IllegalAccessWarning( Thread.currentThread().getContextClassLoader() );
  }

  // Disable Java 9 warnings re "An illegal reflective access operation has occurred"
  public static void disableJava9IllegalAccessWarning( ClassLoader cl )
  {
    if( JreUtil.isJava8() )
    {
      return;
    }

    try
    {
      Class cls = Class.forName( "jdk.internal.module.IllegalAccessLogger", false, cl );
      Field logger = cls.getDeclaredField( "logger" );
      getUnsafe().putObjectVolatile( cls, getUnsafe().staticFieldOffset( logger ), null );
    }
    catch( Throwable ignore )
    {
    }
  }

  public static void openJavaBase()
  {
    if( JreUtil.isJava8() )
    {
      return;
    }

    try
    {
      Class<?> classModule = ReflectUtil.type( "java.lang.Module" );
      ReflectUtil.MethodRef addExportsOrOpens = ReflectUtil.method( classModule, "implAddExportsOrOpens", String.class, classModule, boolean.class, boolean.class );

      //
      // Module: manifold jars
      //
      Set<Object> targetModules = new HashSet<>();
      Object /*Module*/ manifoldModule = ReflectUtil.method( Class.class, "getModule" ).invoke( NecessaryEvilUtil.class );
      targetModules.add( manifoldModule );

      maybeHandleJBoss( targetModules );

      for( Object targetModule: targetModules )
      {
        //
        // Module: java.base
        //
        Object /*Module*/ javaBaseModule = ReflectUtil.method( Class.class, "getModule" ).invoke( String.class );
        addExportsOrOpens.invoke( javaBaseModule, "jdk.internal.loader", targetModule, true, true );
        addExportsOrOpens.invoke( javaBaseModule, "jdk.internal.vm.annotation", targetModule, true, true );
        addExportsOrOpens.invoke( javaBaseModule, "java.lang.reflect", targetModule, true, true ); // for jailbreak
        addExportsOrOpens.invoke( javaBaseModule, "java.net", targetModule, true, true );

        //
        // Module: jdk.compiler
        //
        Object /*Module*/ jdkCompilerModule = ReflectUtil.method( Class.class, "getModule" )
          .invoke( ReflectUtil.type( "com.sun.tools.javac.code.Symbol", true ) );
        addExportsOrOpens.invoke( jdkCompilerModule, "com.sun.tools.javac.api", targetModule, true, true );
        addExportsOrOpens.invoke( jdkCompilerModule, "com.sun.tools.javac.code", targetModule, true, true );
        addExportsOrOpens.invoke( jdkCompilerModule, "com.sun.tools.javac.comp", targetModule, true, true );
        addExportsOrOpens.invoke( jdkCompilerModule, "com.sun.tools.javac.file", targetModule, true, true );
        addExportsOrOpens.invoke( jdkCompilerModule, "com.sun.tools.javac.jvm", targetModule, true, true );
        addExportsOrOpens.invoke( jdkCompilerModule, "com.sun.tools.javac.main", targetModule, true, true );
        addExportsOrOpens.invoke( jdkCompilerModule, "com.sun.tools.javac.model", targetModule, true, true );
        addExportsOrOpens.invoke( jdkCompilerModule, "com.sun.tools.javac.parser", targetModule, true, true );
        addExportsOrOpens.invoke( jdkCompilerModule, "com.sun.tools.javac.platform", targetModule, true, true );
        addExportsOrOpens.invoke( jdkCompilerModule, "com.sun.tools.javac.processing", targetModule, true, true );
        addExportsOrOpens.invoke( jdkCompilerModule, "com.sun.tools.javac.resources", targetModule, true, true );
        addExportsOrOpens.invoke( jdkCompilerModule, "com.sun.tools.javac.tree", targetModule, true, true );
        addExportsOrOpens.invoke( jdkCompilerModule, "com.sun.tools.javac.util", targetModule, true, true );

        //
        // Module: jdk.javadoc
        //
        Class<?> HtmlDoclet = ReflectUtil.type( "jdk.javadoc.internal.doclets.formats.html.HtmlDoclet", true );
        if( HtmlDoclet == null )
        {
          // Warn and continue
          System.out.println( "\nWARNING: Failed to find class 'jdk.javadoc.internal.doclets.formats.html.HtmlDoclet'\n" );
          return;
        }
        Object /*Module*/ jdkJavadoc = ReflectUtil.method( Class.class, "getModule" ).invoke( HtmlDoclet );
        addExportsOrOpens.invoke( jdkJavadoc, "jdk.javadoc.internal.doclets.formats.html", targetModule, true, true );
        addExportsOrOpens.invoke( jdkJavadoc, "com.sun.tools.doclets.standard", targetModule, true, true );
        addExportsOrOpens.invoke( jdkJavadoc, "com.sun.tools.javadoc.main", targetModule, true, true );
      }
    }
    catch( Throwable e )
    {
      throw new RuntimeException( "Error initializing Manifold", e );
    }
  }

  private static void maybeHandleJBoss( Set<Object> targetModules )
  {
    ClassLoader thisClassLoader = NecessaryEvilUtil.class.getClassLoader();
    if( thisClassLoader.getClass().getTypeName().equals( "org.jboss.modules.ModuleClassLoader" ) )
    {
      try
      {
        Object jbossNamedModule = ReflectUtil.field( thisClassLoader, "module" ).get();
        targetModules.add( jbossNamedModule );
      }
      catch( Exception e )
      {
        System.out.println( "\nWARNING: Failed to get JBoss module." );
        e.printStackTrace();
      }
    }
  }
}
