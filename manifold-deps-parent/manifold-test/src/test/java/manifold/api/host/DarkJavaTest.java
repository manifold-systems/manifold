package manifold.api.host;

import junit.framework.TestCase;
import manifold.internal.runtime.Bootstrap;
import manifold.util.ReflectUtil;

/**
 */
public class DarkJavaTest extends TestCase
{
  public void testDarkness()
  {
    // Dark Java is only available at runtime, bootstrap runtime
    Bootstrap.init();

    // Use reflection to work with Dark Java
    Object darkness = ReflectUtil.constructor( "abc.Darkness", String.class ).newInstance( "hi" );
    assertEquals( "hi", ReflectUtil.method( darkness, "getName" ).invoke() );
    assertEquals( "bye", ReflectUtil.method(
      ReflectUtil.method( darkness, "makeStuff", String.class )
        .invoke( "bye" ), "getStuff" )
      .invoke() );
  }
}
