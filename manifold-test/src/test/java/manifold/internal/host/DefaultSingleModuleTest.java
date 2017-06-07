package manifold.internal.host;

import junit.framework.TestCase;
import manifold.api.gen.SrcClass;
import manifold.internal.javac.ClassSymbols;

/**
 */
public class DefaultSingleModuleTest extends TestCase
{
  public void testMakeSrcClassStub()
  {
    SrcClass srcClass = ClassSymbols.instance( ManifoldHost.getCurrentModule() ).makeSrcClassStub( "java.lang.String" );
    StringBuilder sb = new StringBuilder();
    srcClass.render( sb, 0 );
  }
}
