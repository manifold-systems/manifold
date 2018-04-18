package manifold.util;

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
        //noinspection ConstantConditions
        Object /*Module*/ manifoldModule = ReflectUtil.method( Class.class, "getModule" ).invoke( JreUtil.class );
        _modularRuntime = (boolean)ReflectUtil.method( manifoldModule, "isNamed" ).invoke();
      }
    }
    return _modularRuntime;
  }
}
