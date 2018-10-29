package manifold.util;

import java.lang.reflect.Field;
import sun.misc.Unsafe;

public class NecessaryEvilUtil
{
  private static Unsafe UNSAFE = null;

  static Unsafe getUnsafe()
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
//      addExportsOrOpens.invoke( javaBaseModule, "jdk.internal.loader", ReflectUtil.field( manifoldModule.getClass(), "ALL_UNNAMED_MODULE" ).getStatic(), true, true );
      addExportsOrOpens.invoke( javaBaseModule, "java.net", manifoldModule, true, true );

//      Object /*Module*/ javaScriptingModule = ReflectUtil.method( Class.class, "getModule" ).invoke( ReflectUtil.type( "javax.script.Bindings" ) );
//      addExportsOrOpens.invoke( javaScriptingModule, "javax.script", manifoldModule, true, true );

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
      System.err.println( "Error initializing Manifold\n:" +
                          "Failed to automatically configure Java module access, you must add the following arguments to java.exe:\n" +
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
