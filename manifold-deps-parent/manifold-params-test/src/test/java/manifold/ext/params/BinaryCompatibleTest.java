package manifold.ext.params;

import junit.framework.TestCase;
import manifold.ext.params.rt.bool;
import manifold.ext.rt.api.auto;

public class BinaryCompatibleTest extends TestCase
{
  public void testSanity()
  {
    auto result = hi( "Aidan", age: 99, givenName: "Scott" );
    assertEquals( "Aidan", result.name );
    assertEquals( 99, result.age );
    assertEquals( "Scott", result.givenName );

    auto result2 = hi( "Aidan", age: 99 );
    assertEquals( "Aidan", result2.name );
    assertEquals( 99, result2.age );
    assertEquals( "Aidan", result2.givenName );
  }

  // Since it's too difficult to write a test for binary compatible changes, we instead simulate how a pretend past method
  // is called by directly calling the internal generated method that would have been compiled. The hi() method defined
  // here pretends to be the revised method that adds the 'givenName' param. This test basically verifies that adding new
  // optional parameters to a method already having optional parameters is binary compatible with code compiled against
  // the older version of the method.
  public void testBinaryCompatible()
  {
    auto result = hi( "Aidan", 99 );
    assertEquals( "Aidan", result.name );
    assertEquals( 99, result.age );
    assertEquals( "Aidan", result.givenName );
  }

  // This test invokes hi() using reflection via jailbreak(). This verifies that a positional overload exists to bridge
  // an older method only having the 'age' optional parameter to the new method adding the 'givenName' optional parameter.
  public void testBinaryCompatiblePositional()
  {
    auto result = this.jailbreak().hi( "Aidan", 99 );
    assertEquals( "Aidan", result.name );
    assertEquals( 99, result.age );
    assertEquals( "Aidan", result.givenName );
  }

  // This test invokes hi() using reflection via jailbreak(). This verifies that a positional overload exists to bridge
  // an older, purely positional method that only defines the 'name' parameter to the new method adding the optional parameters.
  // Although, technically, this test is unnecessary because purely positional calls invoke the overloads directly, it remains
  // in case a wise guy changes how positional calls work.
  public void testBinaryCompatiblePurePositional()
  {
    auto result = this.jailbreak().hi( "Aidan" );
    assertEquals( "Aidan", result.name );
    assertEquals( -1, result.age );                       
    assertEquals( "Aidan", result.givenName );
  }

  public auto hi( String name, int age = -1, String givenName = name )
  {
    return name, age, givenName;
  }
}
