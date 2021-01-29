package manifold.util;

import java.lang.reflect.AccessibleObject;
import junit.framework.TestCase;

public class ReflectUtilTest extends TestCase
{
  public void testStaticFinalFields()
  {
    Class clsTestClass1;
    try
    {
      clsTestClass1 = Class.forName( "manifold.util.testClasses.TestClass1" );
    }
    catch( Exception e )
    {
      throw new RuntimeException( e );
    }
    ReflectUtil.FieldRef field = ReflectUtil.field( clsTestClass1, "STATIC_FINAL_STRING" );
    assertNotNull( field );
    field.setStatic( "bye" );
    assertEquals( "bye", field.getStatic() );
  }

  public void testFinalFields()
  {
    Object obj = ReflectUtil.constructor( "manifold.util.testClasses.TestClass1" ).newInstance();
    ReflectUtil.LiveFieldRef field = ReflectUtil.field( obj, "FINAL_STRING" );
    assertNotNull( field );
    field.set( "bye" );
    assertEquals( "bye", field.get() );
  }

  public void testOverrideOffsetForJava12() throws NoSuchFieldException
  {
    // since we run this test in Java 8, we can test that the approximated offset for Java 12 matches the actual offset
    long approximateOffset = AccessibleObject_layout.getOverrideOffset( NecessaryEvilUtil.getUnsafe() );
    long actualOffset = NecessaryEvilUtil.getUnsafe().objectFieldOffset( AccessibleObject.class.getDeclaredField( "override" ) );
    assertEquals( actualOffset, approximateOffset );
  }

  public void testStructuralCall()
  {
    Object res = ReflectUtil.structuralCall( ReflectUtil.method( IFoo.class, "callMe", CharSequence.class ).getMethod(), new Foo(), "hi" );
    assertEquals( "hi", res );
    res = ReflectUtil.structuralCall( ReflectUtil.method( IFoo.class, "callMe", String.class ).getMethod(), new Foo(), "hi" );
    assertEquals( "hi", res );
    res = ReflectUtil.structuralCall( ReflectUtil.method( IFoo.class, "callMe", int.class ).getMethod(), new Foo(), 5 );
    assertEquals( "int", res );
    res = ReflectUtil.structuralCall( ReflectUtil.method( IFoo.class, "callMe", double.class ).getMethod(), new Foo(), 5 );
    assertEquals( "Number", res );
    try
    {
      ReflectUtil.structuralCall( ReflectUtil.method( IFoo.class, "callMe", int[].class ).getMethod(), new Foo(), 5 );
      fail( "Should have failed with 'Illegal structural call' since there is no method on Foo compatible with callMe(int[])" );
    }
    catch( RuntimeException ignore ) {}
    res = ReflectUtil.structuralCall( ReflectUtil.method( IFoo.class, "returnString", int.class ).getMethod(), new Foo(), 5 );
    assertEquals( "String", res );
    res = ReflectUtil.structuralCall( ReflectUtil.method( IFoo.class, "returnCharSequence", int.class ).getMethod(), new Foo(), 5 );
    assertEquals( "String", res );
    res = ReflectUtil.structuralCall( ReflectUtil.method( IFoo.class, "callMe", Three.class ).getMethod(), new Foo(), new Three() );
    assertEquals( "two", res );
  }

  interface IFoo
  {
    String callMe( CharSequence p );
    String callMe( String p );
    String callMe( int p );
    String callMe( double p );
    String callMe( int[] p );
    String callMe( Integer p );

    String returnString( int n );
    CharSequence returnCharSequence( int n );

    String callMe( Three three );
  }
  static class Foo
  {
    public String callMe( CharSequence p )
    {
      return p.toString();
    }

    public String callMe( int p )
    {
      return "int";
    }

    public String callMe( Number p )
    {
      return "Number";
    }

    public String callMe( Two two )
    {
      return "two";
    }
    public String callMe( One one )
    {
      return "one";
    }

    public CharSequence returnString( int n )
    {
      return "CharSequence";
    }
    public String returnString( long l )
    {
      return "String";
    }

    public String returnCharSequence( long l )
    {
      return "String";
    }
    public Object returnCharSequence( int n )
    {
      return "Object";
    }

    public String primitiveMethod( int i )
    {
      return "String";
    }

  }

  static class One {}
  static class Two extends One {}
  static class Three extends Two {}
}
