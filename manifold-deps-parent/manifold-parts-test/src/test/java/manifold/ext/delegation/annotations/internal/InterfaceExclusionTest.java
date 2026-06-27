package manifold.ext.delegation.annotations.internal;

import junit.framework.TestCase;
import manifold.ext.delegation.rt.api.internal;
import manifold.ext.delegation.rt.api.link;
import manifold.ext.delegation.rt.api.part;
import manifold.ext.rt.api.Jailbreak;
import manifold.util.ReflectUtil;

public class InterfaceExclusionTest extends TestCase
{
  public void testInterfaceExcluded()
  {
    MyClass myClass = new MyClass();
    @Jailbreak PartWithInternalInterface part = myClass.foo;
    assertEquals( 2, part.$selves.length ); // PartWithInternalInterface dispatches Foo and Bar
    assertEquals( myClass, part.$selves[0] ); // PartWithInternalInterface's Foo identity is MyClass
    assertEquals( part, part.$selves[1] ); // PartWithInternalInterface's Bar identity is itself bc @internal makes Bar non-delegatable
    assertEquals( "PartWithInternalInterface.bar", part.foo() );
  }

  public void testInterfaceExcluded_Inherited()
  {
    MyClassInherited myClass = new MyClassInherited();
    @Jailbreak MyClassIntermediate intermediate = myClass.foo;
    assertEquals( 2, intermediate.$selves.length ); // PartWithInternalInterface dispatches Foo and Bar
    assertEquals( myClass, intermediate.$selves[0] ); // PartWithInternalInterface's Foo identity is MyClass
    assertEquals( intermediate, intermediate.$selves[1] ); // PartWithInternalInterface's Bar identity is itself bc @internal makes Bar non-delegatable

    // check @selves array in super class
    Object[] PartWithInternalInterface_$selves = (Object[])ReflectUtil.field( PartWithInternalInterface.class, "$selves" ).get( intermediate );
    assertEquals( 2, PartWithInternalInterface_$selves.length ); // PartWithInternalInterface dispatches Foo and Bar
    assertEquals( myClass, PartWithInternalInterface_$selves[0] ); // PartWithInternalInterface's Foo identity is MyClass
    assertEquals( intermediate, PartWithInternalInterface_$selves[1] ); // PartWithInternalInterface's Bar identity is itself bc @internal makes Bar non-delegatable

    // PartWithInternalInterface fields the foo() call which calls PartWithInternalInterface's bar() bc MyClassInherited cannot link to it
    assertEquals( "PartWithInternalInterface.bar", intermediate.foo() );
  }

  public interface Foo {
    String foo();
  }
  public interface Bar {
    String bar();
  }

  static class MyClass implements Foo, Bar
  {
    // should only link Foo, not Bar bc @internal
    @link PartWithInternalInterface foo = new PartWithInternalInterface();

    // must override run bc it is not delegated due to @internal Bar in PartWithInternalInterface
    @Override
    public String bar()
    {
      return "MyClass.bar";
    }
  }

  static @part class PartWithInternalInterface implements Foo, @internal Bar
  {
    @Override
    public String foo()
    {
      return bar();
    }

    @Override
    public String bar()
    {
      return "PartWithInternalInterface.bar";
    }
  }

  static class MyClassInherited implements Foo, Bar
  {
    // should only link Foo, not Bar bc @internal
    @link MyClassIntermediate foo = new MyClassIntermediate();

    // must override run bc it is not delegated due to @internal Bar in PartWithInternalInterface
    @Override
    public String bar()
    {
      return "MyClass.bar";
    }
  }

  static @part class MyClassIntermediate extends PartWithInternalInterface
  {
  }
}
