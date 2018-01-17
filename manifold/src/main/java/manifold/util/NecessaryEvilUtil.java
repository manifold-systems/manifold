package manifold.util;

import java.lang.reflect.Field;
import sun.misc.Unsafe;

public class NecessaryEvilUtil
{
  static final Unsafe UNSAFE = getUnsafe();
  private static Unsafe getUnsafe()
  {
    try
    {
      Field theUnsafe = Unsafe.class.getDeclaredField( "theUnsafe" );
      theUnsafe.setAccessible( true );
      return (Unsafe)theUnsafe.get( null );
    }
    catch( Throwable t )
    {
      System.err.println( "The 'Unsafe' class is not accessible" );
      return null;
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
      UNSAFE.putObjectVolatile( cls, UNSAFE.staticFieldOffset( logger ), null );
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

      Object /*Module*/ manifoldModule = ReflectUtil.method( Class.class, "getModule" ).invoke( NecessaryEvilUtil.class );

      Object /*Module*/ javaBaseModule = ReflectUtil.method( Class.class, "getModule" ).invoke( String.class );
      addExportsOrOpens.invoke( javaBaseModule, "jdk.internal.loader", manifoldModule, true, true );
      addExportsOrOpens.invoke( javaBaseModule, "java.net", manifoldModule, true, true );

//      Object /*Module*/ javaScriptingModule = ReflectUtil.method( Class.class, "getModule" ).invoke( ReflectUtil.type( "javax.script.Bindings" ) );
//      addExportsOrOpens.invoke( javaScriptingModule, "javax.script", manifoldModule, true, true );

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
      addExportsOrOpens.invoke( jdkCompilerModule, "com.sun.tools.javac.tree", manifoldModule, true, true );
      addExportsOrOpens.invoke( jdkCompilerModule, "com.sun.tools.javac.util", manifoldModule, true, true );
    }
    catch( Throwable e )
    {
      System.err.println( "Failed to automatically configure Java 9 module accesss, you must explicitly add the following arguments to java.exe:\n" +
                          "--add-opens=java.base/jdk.internal.loader=<ALL-UNNAMED or manifold-all>\n" +
                          "--add-opens=java.base/java.net=<ALL-UNNAMED or manifold-all>\n" +
                          "--add-opens=jdk.compiler/com.sun.tools.javac.api=<ALL-UNNAMED or manifold-all>\n" +
                          "--add-opens=jdk.compiler/com.sun.tools.javac.code=<ALL-UNNAMED or manifold-all>\n" +
                          "--add-opens=jdk.compiler/com.sun.tools.javac.comp=<ALL-UNNAMED or manifold-all>\n" +
                          "--add-opens=jdk.compiler/com.sun.tools.javac.file=<ALL-UNNAMED or manifold-all>\n" +
                          "--add-opens=jdk.compiler/com.sun.tools.javac.jvm=<ALL-UNNAMED or manifold-all>\n" +
                          "--add-opens=jdk.compiler/com.sun.tools.javac.main=<ALL-UNNAMED or manifold-all>\n" +
                          "--add-opens=jdk.compiler/com.sun.tools.javac.model=<ALL-UNNAMED or manifold-all>\n" +
                          "--add-opens=jdk.compiler/com.sun.tools.javac.parser=<ALL-UNNAMED or manifold-all>\n" +
                          "--add-opens=jdk.compiler/com.sun.tools.javac.platform=<ALL-UNNAMED or manifold-all>\n" +
                          "--add-opens=jdk.compiler/com.sun.tools.javac.processing=<ALL-UNNAMED or manifold-all>\n" +
                          "--add-opens=jdk.compiler/com.sun.tools.javac.tree=<ALL-UNNAMED or manifold-all>\n" +
                          "--add-opens=jdk.compiler/com.sun.tools.javac.util=<ALL-UNNAMED or manifold-all>\n" );
      throw new RuntimeException( e );
    }
  }
}
