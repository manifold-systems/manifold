package manifold.ext.delegation.annotations.internal;

import junit.framework.TestCase;
import manifold.ext.delegation.rt.api.DelegationLinkageError;
import manifold.ext.delegation.rt.api.internal;
import manifold.ext.delegation.rt.api.link;
import manifold.ext.delegation.rt.api.part;

import static manifold.util.DebugModeUtil.isJdwpEnabled;

public class DelegationLinkageErrorTest extends TestCase
{
  public void testLinkageError()
  {
    if( !isJdwpEnabled() )
    {
      // the runtime check that throws the DelegationLinkageError for this test
      // is enabled only when the JVM is attached via JDWP (for debugging)
      return;
    }

    try
    {
      PartWithInternalInterface thepart = new PartWithInternalInterface();
      new MyClass( thepart, thepart );
      fail( "Should have resulted in DelegationLinkageError because " +
            "PartWithInternalInterface annotates Runnable with @internal" );
    }
    catch( DelegationLinkageError expected )
    {
    }
  }

  public interface Foo {
    void foo();
  }

// Note, this example is detected statically and results in a compile error
//  static class MyClass implements Foo, Runnable
//  {
//    PartWithInternalInterface thepart = new PartWithInternalInterface();
//
//    @link Foo foo = thepart;
//    @link Runnable runnable = thepart; // compile error bc the impl type is known at compile-time
//  }

  static class MyClass implements Foo, Runnable
  {
    @link Foo foo;
    @link Runnable runnable;

    public MyClass( Foo foo, Runnable runnable )
    {
      this.foo = foo;
      this.runnable = runnable; // runtime check throws DelegationLinkageError here bc runtime type of `runnable` here is PartWithInternalInterface, which annotates Runnable with @internal
    }
  }

  static @part class PartWithInternalInterface implements Foo, @internal Runnable
  {
    @Override
    public void run()
    {
      System.out.println( "PartWithInternalInterface.run");
    }

    @Override
    public void foo()
    {
      System.out.println( "PartWithInternalInterface.foo");
    }
  }
}
