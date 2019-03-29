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
      Object /*Module*/ manifoldModule = ReflectUtil.method( Class.class, "getModule" ).invoke( NecessaryEvilUtil.class );

      //
      // Module: java.base
      //
      Object /*Module*/ javaBaseModule = ReflectUtil.method( Class.class, "getModule" ).invoke( String.class );
      addExportsOrOpens.invoke( javaBaseModule, "jdk.internal.loader", manifoldModule, true, true );
      addExportsOrOpens.invoke( javaBaseModule, "jdk.internal.vm.annotation", manifoldModule, true, true );
      addExportsOrOpens.invoke( javaBaseModule, "java.lang.reflect", manifoldModule, true, true ); // for jailbreak
      addExportsOrOpens.invoke( javaBaseModule, "java.net", manifoldModule, true, true );

      //
      // Module: jdk.compiler
      //
      Object /*Module*/ jdkCompilerModule = ReflectUtil.method( Class.class, "getModule" ).invoke( ReflectUtil.type( "com.sun.tools.javac.code.Symbol" ) );
      addExportsOrOpens.invoke( jdkCompilerModule, "com.sun.tools.javac.api", manifoldModule, true, true );
      addExportsOrOpens.invoke( jdkCompilerModule, "com.sun.tools.javac.code", manifoldModule, true, true );
      addExportsOrOpens.invoke( jdkCompilerModule, "com.sun.tools.javac.comp", manifoldModule, true, true );
      addExportsOrOpens.invoke( jdkCompilerModule, "com.sun.tools.javac.file", manifoldModule, true, true );
      addExportsOrOpens.invoke( jdkCompilerModule, "com.sun.tools.javac.jvm", manifoldModule, true, true );
      addExportsOrOpens.invoke( jdkCompilerModule, "com.sun.tools.javac.main", manifoldModule, true, true );
      addExportsOrOpens.invoke( jdkCompilerModule, "com.sun.tools.javac.model", manifoldModule, true, true );
      addExportsOrOpens.invoke( jdkCompilerModule, "com.sun.tools.javac.parser", manifoldModule, true, true );
      addExportsOrOpens.invoke( jdkCompilerModule, "com.sun.tools.javac.platform", manifoldModule, true, true );
      addExportsOrOpens.invoke( jdkCompilerModule, "com.sun.tools.javac.processing", manifoldModule, true, true );
      addExportsOrOpens.invoke( jdkCompilerModule, "com.sun.tools.javac.resources", manifoldModule, true, true );
      addExportsOrOpens.invoke( jdkCompilerModule, "com.sun.tools.javac.tree", manifoldModule, true, true );
      addExportsOrOpens.invoke( jdkCompilerModule, "com.sun.tools.javac.util", manifoldModule, true, true );

      //
      // Module: jdk.javadoc
      //
      Object /*Module*/ jdkJavadoc = ReflectUtil.method( Class.class, "getModule" ).invoke( ReflectUtil.type( "jdk.javadoc.internal.doclets.formats.html.HtmlDoclet" ) );
      addExportsOrOpens.invoke( jdkJavadoc, "jdk.javadoc.internal.doclets.formats.html", manifoldModule, true, true );
      addExportsOrOpens.invoke( jdkJavadoc, "com.sun.tools.doclets.standard", manifoldModule, true, true );
      addExportsOrOpens.invoke( jdkJavadoc, "com.sun.tools.javadoc.main", manifoldModule, true, true );
    }
    catch( Throwable e )
    {
      throw new RuntimeException( "Error initializing Manifold", e );
    }
  }
}
