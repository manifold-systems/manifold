package manifold.templates.misc;

import org.junit.Test;

import static org.junit.Assert.assertTrue;

public class BigassContentChunkTest
{
  @Test
  public void bigassContentChunkWorks()
  {
    int size = misc.BigassContentChunkTest.render( "testing123" ).length();
    assertTrue( size > 65_000 );
  }
}