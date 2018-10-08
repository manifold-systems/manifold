package manifold.internal.host;

import junit.framework.TestCase;
import manifold.api.gen.SrcClass;
import manifold.internal.javac.ClassSymbols;
import manifold.internal.runtime.Bootstrap;

/**
 */
public class DefaultSingleModuleTest extends TestCase
{
  public void testMakeSrcClassStub()
  {
    Bootstrap.init();
    SrcClass srcClass = ClassSymbols.instance( RuntimeManifoldHost.get().getSingleModule() ).makeSrcClassStub( "java.lang.String" );
    StringBuilder sb = new StringBuilder();
    srcClass.render( sb, 0 );
    assertTrue( sb.indexOf( "package java.lang;" ) >= 0 );
    assertTrue( sb.indexOf( "public final class String " ) > 0 );
  }
}
