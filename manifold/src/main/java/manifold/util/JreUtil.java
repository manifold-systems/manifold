package manifold.util;

public class JreUtil
{
  private static final boolean JAVA_8 = System.getProperty( "java.version" ).startsWith( "1.8" );

  public static boolean isJava8()
  {
    return JAVA_8;
  }

  public static boolean isJava9()
  {
    return !JAVA_8;
  }

  public static void main( String[] args )
  {
    System.out.println( isJava8() );
  }
}
