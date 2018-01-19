package manifold.util;

import com.sun.tools.javac.code.Symbol;
import com.sun.tools.javac.util.Context;

public class JreUtil
{
  private static final boolean JAVA_8 = System.getProperty( "java.version" ).startsWith( "1.8" );
  private static Boolean _modular;
  private static Boolean _modularRuntime;

  public static boolean isJava8()
  {
    return JAVA_8;
  }

  @SuppressWarnings("unused")
  public static boolean isJava9orLater()
  {
    return !JAVA_8;
  }

  public static boolean isJava9Modular_compiler( Context ctx )
  {
    if( _modular == null )
    {
      if( isJava8()  )
      {
        _modular = false;
      }
      else
      {
        Object modulesUtil = ReflectUtil.method( ReflectUtil.type( "com.sun.tools.javac.comp.Modules" ), "instance", Context.class ).invokeStatic( ctx );
        Symbol defModule = (Symbol)ReflectUtil.method( modulesUtil, "getDefaultModule" ).invoke();
        _modular = defModule != null && !(boolean)ReflectUtil.method( defModule, "isNoModule" ).invoke() && !(boolean)ReflectUtil.method( defModule, "isUnnamed" ).invoke();
      }
    }
    return _modular;
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
        Object /*Module*/ manifoldModule = ReflectUtil.method( Class.class, "getModule" ).invoke( JreUtil.class );
        _modularRuntime = (boolean)ReflectUtil.method( manifoldModule, "isNamed" ).invoke();
      }
    }
    return _modularRuntime;
  }
}
