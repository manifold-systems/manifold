package manifold.ext;

import java.time.LocalTime;
import java.util.ArrayList;
import junit.framework.TestCase;
import manifold.ext.api.JailBreak;
import manifold.ext.stuff.Leaf;
import manifold.util.ReflectUtil;

public class JailBreakTest extends TestCase
{
  public void testJailbreakMethod()
  {
    manifold.ext.stuff.@JailBreak SecretParam secretParam =
      new manifold.ext.stuff.@JailBreak SecretParam();
    secretParam._foo = 9;

    manifold.ext.stuff.@JailBreak SecretClass secret =
      new manifold.ext.stuff.@JailBreak SecretClass( secretParam );

    int foo = secret.getParam().jailbreak()._foo;
    assertEquals( 9, foo );
    secret.getParam().jailbreak()._foo = 10;
    assertEquals( 10, secret.getParam().jailbreak()._foo );

    assertEquals( getName(), jailbreak().fName );
  }

  public void testAccessPrivateMembersDeclaredInSupers()
  {
    @JailBreak Leaf leaf = new Leaf();
    leaf.foo();
    assertEquals( 9, leaf.foo(9) );
    assertEquals( 9d, leaf.foo(9d) );

    assertEquals( 8, leaf.foo(leaf.foo(8)) );
    assertEquals( 7, leaf.foo(leaf.foo(leaf.foo(7))) );
    assertEquals( 6, leaf.foo(leaf.foo(leaf.foo(6)), this.toString()) );
    assertEquals( 5, leaf.foo(this.toString(), leaf.foo(leaf.foo(5))) );

//    leaf.foo(leaf.foo(leaf.foo(8)), false);
//    leaf.foo(leaf.foo());
//
//    leaf.foo("err");
//    leaf.foooo();
//
//    Leaf leaf2 = new Leaf();
//    leaf2.foo();
//    leaf2.foo(9);
//    leaf2.foo(9.0d);
//
//    leaf2.foo(leaf2.foo(8));
//    leaf2.foo(leaf2.foo());
//
//    leaf2.foo("err");
//    leaf2.foooo();
  }

  public void testType()
  {
    java.lang.@JailBreak AbstractStringBuilder sb = new @JailBreak StringBuilder();
    sb.append( 8 );

    manifold.ext.stuff.@JailBreak SecretParam secretParam =
      new manifold.ext.stuff.@JailBreak SecretParam();
    secretParam._foo = 9;
    manifold.ext.stuff.@JailBreak SecretClass secret =
      new manifold.ext.stuff.@JailBreak SecretClass( secretParam );
    secretParam = secret.getParam();
    assertEquals( 9, secretParam._foo );
  }

  public void testJailBreak()
  {
    // instance method
    @JailBreak LocalTime time = LocalTime.now();
    time.writeReplace();

    // static method
    @JailBreak LocalTime staticTime = null;
    LocalTime localTime = staticTime.create( 7, 59, 30, 99 );
    assertEquals( localTime.withHour( 7 ).withMinute( 59 ).withSecond( 30 ).withNano( 99 ), localTime );

    // instance field
    @JailBreak LocalTime lt = null;
    lt = LocalTime.of( 11, 12 );
    assertEquals( 11, lt.hour );
    lt.hour = 12;
    assertEquals( 12, lt.hour );

    // static field
    int hoursPerDay = staticTime.HOURS_PER_DAY;
    assertEquals( ReflectUtil.field( LocalTime.class, "HOURS_PER_DAY" ).getStatic(), hoursPerDay );
    staticTime.HOURS_PER_DAY = hoursPerDay + 1;
    assertEquals( ReflectUtil.field( LocalTime.class, "HOURS_PER_DAY" ).getStatic(), hoursPerDay + 1 );

    // new expr
    String charStr = new java.lang.@JailBreak String( new char[]{'h', 'i'}, true );
    assertEquals( "hi", charStr );

    // Test a class that is extended
    @JailBreak ArrayList<String> list = new ArrayList<>();
    list.ensureCapacityInternal( 100 );
  }
}