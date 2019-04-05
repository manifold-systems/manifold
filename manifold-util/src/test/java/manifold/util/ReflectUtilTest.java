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
}
