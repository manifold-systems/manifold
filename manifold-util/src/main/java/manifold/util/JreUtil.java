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

public class JreUtil
{
  private static final boolean MANIFOLD_COMPILING_JAVA9DEFINED = System.getenv( "manifold.compiling.java9defined" ) != null;

  public static final int JAVA_VERSION = getJavaVersion();
  private static Boolean _modular;
  private static Boolean _modularRuntime;
  private static Boolean _noModule;

  private static int getJavaVersion()
  {
    String version = System.getProperty( "java.version" );
    StringBuilder sb = new StringBuilder();
    for( int i = 0; i < version.length(); i++ )
    {
      char c = version.charAt( i );
      if( Character.isDigit( c ) )
      {
        sb.append( c );
      }
      else
      {
        break;
      }
    }
    int major = Integer.parseInt( sb.toString() );
    // note, android's major is 0, but is really 8 (ish)
    major = major <= 1 ? 8 : major;
    if( major == 8 && MANIFOLD_COMPILING_JAVA9DEFINED )
    {
      // total hack for compiling *.java9 files
      major = 9;
    }
    return major;
  }

  public static boolean isJava8()
  {
    return JAVA_VERSION == 8;
  }

  public static boolean isJava9()
  {
    return JAVA_VERSION == 9;
  }

  @SuppressWarnings("unused")
  public static boolean isJava9orLater()
  {
    return !isJava8();
  }

  public static boolean isJava10()
  {
    return JAVA_VERSION == 10;
  }

  @SuppressWarnings("unused")
  public static boolean isJava10orLater()
  {
    return JAVA_VERSION >= 10;
  }

  public static boolean isJava11()
  {
    return JAVA_VERSION == 11;
  }
  public static boolean isJava11orLater()
  {
    return JAVA_VERSION >= 11;
  }

  public static boolean isJava12()
  {
    return JAVA_VERSION == 12;
  }
  public static boolean isJava12orLater()
  {
    return JAVA_VERSION >= 12;
  }

  public static boolean isJava13()
  {
    return JAVA_VERSION == 13;
  }
  public static boolean isJava13orLater()
  {
    return JAVA_VERSION >= 13;
  }

  public static boolean isJava14()
  {
    return JAVA_VERSION == 14;
  }
  public static boolean isJava14orLater()
  {
    return JAVA_VERSION >= 14;
  }

  public static boolean isJava15()
  {
    return JAVA_VERSION == 15;
  }
  public static boolean isJava15orLater()
  {
    return JAVA_VERSION >= 15;
  }

  public static boolean isJava16()
  {
    return JAVA_VERSION == 16;
  }
  public static boolean isJava16orLater()
  {
    return JAVA_VERSION >= 16;
  }

  public static boolean isJava17()
  {
    return JAVA_VERSION == 17;
  }
  public static boolean isJava17orLater()
  {
    return JAVA_VERSION >= 17;
  }

  public static boolean isJava20()
  {
    return JAVA_VERSION == 20;
  }
  public static boolean isJava20orLater()
  {
    return JAVA_VERSION >= 20;
  }

  public static boolean isJava21()
  {
    return JAVA_VERSION == 21;
  }
  public static boolean isJava21orLater()
  {
    return JAVA_VERSION >= 21;
  }

  public static boolean isJava23()
  {
    return JAVA_VERSION == 23;
  }
  public static boolean isJava23orLater()
  {
    return JAVA_VERSION >= 23;
  }

  public static boolean isJava24()
  {
    return JAVA_VERSION == 24;
  }
  public static boolean isJava24orLater()
  {
    return JAVA_VERSION >= 24;
  }

  public static boolean isJava25()
  {
    return JAVA_VERSION == 25;
  }
  public static boolean isJava25orLater()
  {
    return JAVA_VERSION >= 25;
  }

  public static boolean isJava26()
  {
    return JAVA_VERSION == 26;
  }
  public static boolean isJava26orLater()
  {
    return JAVA_VERSION >= 26;
  }

  public static boolean isJava9Modular_compiler( Object/*Context*/ ctx )
  {
    if( _modular == null )
    {
      if( isJava8() )
      {
        _modular = false;
      }
      else
      {
        //noinspection ConstantConditions
        Object modulesUtil = ReflectUtil.method( ReflectUtil.type( "com.sun.tools.javac.comp.Modules" ), "instance", ReflectUtil.type( "com.sun.tools.javac.util.Context" ) ).invokeStatic( ctx );
        Object defModule = ReflectUtil.method( modulesUtil, "getDefaultModule" ).invoke();
        _modular = defModule != null && !(boolean)ReflectUtil.method( defModule, "isNoModule" ).invoke() && !(boolean)ReflectUtil.method( defModule, "isUnnamed" ).invoke();
      }
    }
    return _modular;
  }

  public static boolean isJava9NoModule( Object/*Context*/ ctx )
  {
    if( _noModule == null )
    {
      if( isJava8() )
      {
        _noModule = true;
      }
      else
      {
        //noinspection ConstantConditions
        Object modulesUtil = ReflectUtil.method( ReflectUtil.type( "com.sun.tools.javac.comp.Modules" ), "instance", ReflectUtil.type( "com.sun.tools.javac.util.Context" ) ).invokeStatic( ctx );
        Object defModule = ReflectUtil.method( modulesUtil, "getDefaultModule" ).invoke();
        _noModule = defModule == null || (boolean)ReflectUtil.method( defModule, "isNoModule" ).invoke();
      }
    }
    return _noModule;
  }

  public static boolean isJava9Modular_runtime()
  {
    if( _modularRuntime == null )
    {
      if( isJava8() )
      {
        _modularRuntime = false;
      }
      else
      {
        JdkAccessUtil.bypassJava9Security();

        //noinspection ConstantConditions
        Object /*Module*/ manifoldModule = ReflectUtil.method( Class.class, "getModule" ).invoke( JreUtil.class );
        _modularRuntime = (boolean)ReflectUtil.method( manifoldModule, "isNamed" ).invoke();
      }
    }
    return _modularRuntime;
  }
}
