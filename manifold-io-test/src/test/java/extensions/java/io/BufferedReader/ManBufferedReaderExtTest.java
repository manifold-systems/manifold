package extensions.java.io.BufferedReader;

import manifold.test.api.ExtensionManifoldTest;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;

/**
 */
public class ManBufferedReaderExtTest extends ExtensionManifoldTest
{
  public void testCoverage() {
    testCoverage(ManBufferedReaderExt.class);
  }

  public void testLineSequence() throws IOException {
    File f = File.createTempFile( "foo", ".tmp" );
    f.writeText( "this\nis\na\ntest" );
    assertEquals( Arrays.asList("this", "is", "a", "test"), f.bufferedReader().lineSequence().toList() );
    //noinspection ResultOfMethodCallIgnored
    f.delete();
  }
}
