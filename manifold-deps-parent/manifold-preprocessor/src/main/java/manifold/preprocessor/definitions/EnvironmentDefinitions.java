/*
 * Copyright (c) 2019 - Manifold Systems LLC
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

package manifold.preprocessor.definitions;

import com.sun.tools.javac.processing.JavacProcessingEnvironment;
import com.sun.tools.javac.util.Context;
import java.util.HashMap;
import java.util.Map;
import manifold.internal.javac.JavacPlugin;
import manifold.util.ReflectUtil;
import manifold.util.concurrent.LocklessLazyVar;


import static manifold.util.JreUtil.isJava8;

public class EnvironmentDefinitions
{
  /** Java source version*/
  public static final String JAVA_ = "JAVA_";
  public static final String _OR_LATER = "_OR_LATER";

  /** JPMS mode, defined by presence of module-info.java and if source version is > 8 */
  public static final String JPMS_NONE = "JPMS_NONE";
  public static final String JPMS_UNNAMED = "JPMS_UNNAMED";
  public static final String JPMS_NAMED = "JPMS_NAMED";

  /** Operating System of the compiler/IDE process */
  public static final String OS_FREE_BSD = "OS_FREEBSD";
  public static final String OS_LINUX = "OS_LINUX";
  public static final String OS_MAC = "OS_MAC";
  public static final String OS_SOLARIS = "OS_SOLARIS";
  public static final String OS_UNIX = "OS_UNIX";
  public static final String OS_WINDOWS = "OS_WINDOWS";

  /** Architecture of the compiler/IDE process */
  public static final String ARCH_32 = "ARCH_32";
  public static final String ARCH_64 = "ARCH_64";


  private static LocklessLazyVar<EnvironmentDefinitions> INSTANCE =
    LocklessLazyVar.make( () -> new EnvironmentDefinitions() );

  private final Map<String, String> _env;
  
  static EnvironmentDefinitions instance()
  {
    return INSTANCE.get();
  }

  public EnvironmentDefinitions()
  {
    Map<String, String> map = new HashMap<>();
    addArchitecture( map );
    addOperatingSystem( map );
    addJavacEnvironment( map );
    addJpms( map );
    addMisc( map );
    _env = map;
  }

  public Map<String, String> getEnv()
  {
    return _env;
  }

  protected void addJpms( Map<String, String> map )
  {
    if( JavacPlugin.instance() == null )
    {
      return;
    }

    Context ctx = JavacPlugin.instance().getContext();

    if( isJava8() )
    {
      map.put( JPMS_NONE, "" );
    }
    else
    {
      //noinspection ConstantConditions
      Object modulesUtil = ReflectUtil.method( ReflectUtil.type( "com.sun.tools.javac.comp.Modules" ),
        "instance", ReflectUtil.type( "com.sun.tools.javac.util.Context" ) ).invokeStatic( ctx );
      Object defModule = ReflectUtil.method( modulesUtil, "getDefaultModule" ).invoke();
      if( defModule == null || (boolean)ReflectUtil.method( defModule, "isNoModule" ).invoke() )
      {
        map.put( JPMS_NONE, "" );
      }
      else if( (boolean)ReflectUtil.method( defModule, "isUnnamed" ).invoke() )
      {
        map.put( JPMS_UNNAMED, "" );
      }
      else
      {
        map.put( JPMS_NAMED, "" );
      }
    }
  }

  protected void addJavacEnvironment( Map<String, String> map )
  {
    if( JavacPlugin.instance() == null )
    {
      return ;
    }

    JavacProcessingEnvironment jpe = JavacProcessingEnvironment.instance( JavacPlugin.instance().getContext() );
    addJavaVersion( map, jpe );
    addAnnotationOptions( map, jpe );
  }

  /**
   * These are the {@code -Akey[=value]} options on the javac commmand line, much like {@code -D}, but for the javac
   * environment, not the JVM. Intended for use with annotations, but also great for a preprocessor.
   * See <a href="https://docs.oracle.com/javase/8/docs/technotes/tools/windows/javac.html">Standard Options</a>.
   */
  protected void addAnnotationOptions( Map<String, String> map, JavacProcessingEnvironment jpe )
  {
    Map<String, String> options = jpe.getOptions();
    if( options != null )
    {
      map.putAll( options );
    }
  }

  protected void addJavaVersion( Map<String, String> map, JavacProcessingEnvironment jpe )
  {
    int version = jpe.getSourceVersion().ordinal();
    makeJavaVersionDefinitions( map, version );
  }

  protected void makeJavaVersionDefinitions( Map<String, String> map, int version )
  {
    map.put( JAVA_ + version, "" );
    for( int i = 2; i <= version; i++ )
    {
      map.put( JAVA_ + version + _OR_LATER, "" );
    }
  }

  protected void addOperatingSystem( Map<String, String> map )
  {
    if( SystemInfo.isFreeBSD )
    {
      map.put( OS_FREE_BSD, "" );
    }
    else if( SystemInfo.isLinux )
    {
      map.put( OS_LINUX, "" );
    }
    else if( SystemInfo.isMac )
    {
      map.put( OS_MAC, "" );
    }
    else if( SystemInfo.isSolaris )
    {
      map.put( OS_SOLARIS, "" );
    }
    else if( SystemInfo.isUnix )
    {
      map.put( OS_UNIX, "" );
    }
    else if( SystemInfo.isWindows )
    {
      map.put( OS_WINDOWS, "" );
    }
  }

  protected void addArchitecture( Map<String, String> map )
  {
    if( SystemInfo.is32Bit )
    {
      map.put( ARCH_32, "" );
    }
    else if( SystemInfo.is64Bit )
    {
      map.put( ARCH_64, "" );
    }
  }

  protected void addMisc( Map<String, String> map )
  {
  }
}
