package manifold.ext.delegation.annotations.internal;

import junit.framework.TestCase;
import manifold.ext.delegation.rt.api.internal;
import manifold.ext.delegation.rt.api.link;
import manifold.ext.delegation.rt.api.part;

public class MethodTest extends TestCase
{
  public void testForward()
  {
    MyRoot root = new MyRoot();
    assertEquals( "notInternal", root.notInternal() );
    assertEquals( "hi", root.bar() );

    // these should produce compile errors
    //root.internal();
    //root.internalDefault();
    //root.genericMethod( "illegal" );
  }

  public void testPart()
  {
    MyRootPart root = new MyRootPart();
    assertEquals( "notInternal", root.notInternal() );
    assertEquals( "hi", root.bar() );

    // these should produce compile errors
    //root.internal();
    //root.internalDefault();
    //root.genericMethod( "illegal" );
  }

  interface Foo extends Bar
  {
    String notInternal();
    @internal String internal();
    @internal default String internalDefault()
    {
      barInternal();
      barInternalDefault();
      this.barInternal();
      this.barInternalDefault();
      Bar.super.barInternalDefault();
      return "hi";
    }
  }

  interface Bar extends GenericType<String>
  {
    default String bar() { return genericMethod( "hi" ); }

    @internal
    String barInternal();

    @internal
    default String barInternalDefault()
    {
      return "hi";
    }

    @internal <E> String genericMethod( E e );
  }

  interface GenericType<T> {
    <E> T genericMethod(E e);
  }

  static class FooDelegate implements Foo
  {
    @Override
    public String notInternal()
    {
      barInternal();
      barInternalDefault();
      this.barInternal();
      this.barInternalDefault();
      Foo.super.barInternalDefault();

      return "notInternal";
    }

    @Override
    public String internal()
    {
      barInternal();
      barInternalDefault();
      this.barInternal();
      this.barInternalDefault();
      Foo.super.barInternalDefault();

      return "internal";
    }

    @Override
    public String internalDefault()
    {
      return Foo.super.internalDefault();
    }

    @Override
    public String bar()
    {
      return Foo.super.bar();
    }

    @Override
    public String barInternal()
    {
      barInternalDefault();
      this.barInternalDefault();
      Foo.super.barInternalDefault();

      return "";
    }

    @Override
    public <E> String genericMethod( E e )
    {
      return e.toString();
    }
  }

  static class MyRoot implements Foo
  {
    @link Foo foo = new FooDelegate();
  }

  static @part class FooPart implements Foo
  {
    @Override
    public String notInternal()
    {
      internal();
      this.internal();

      barInternal();
      barInternalDefault();
      this.barInternal();
      this.barInternalDefault();
      Foo.super.barInternalDefault();

      return "notInternal";
    }

    @Override
    public String internal()
    {
      internalDefault();
      this.internalDefault();
      return "internal";
    }
    @Override
    public String internalDefault()
    {
      return Foo.super.internalDefault();
    }

    @Override
    public String bar()
    {
      return Foo.super.bar();
    }

    @Override
    public String barInternal()
    {
      barInternalDefault();
      this.barInternalDefault();
      Foo.super.barInternalDefault();

      return "";
    }

    @Override
    public <E> String genericMethod( E e )
    {
      return e.toString();
    }

    @Override
    public String barInternalDefault()
    {
      return Foo.super.barInternalDefault();
    }
  }

  static @part class MyRootPart implements Foo
  {
    @link Foo foo = new FooPart();

    public String whatever()
    {
// should produce compile error
//      return new FooPart().internal();
      return "whatever";
    }
  }

}
