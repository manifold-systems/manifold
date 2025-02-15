package manifold.ext;

import abc.*;
import manifold.api.type.BasicIncrementalCompileDriver;
import manifold.api.type.ClassType;
import manifold.api.type.ContributorKind;
import manifold.ext.rt.api.Structural;
import manifold.ext.sup.SuperClass2;

import javax.swing.*;
import java.awt.Rectangle;
import java.awt.event.ItemEvent;
import java.io.Serializable;
import java.util.*;
import java.util.concurrent.Callable;
import java.util.concurrent.Executors;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.UnaryOperator;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.junit.Assert.assertArrayEquals;

/**
 */
public class SimpleTest extends SimpleTestSuper
{
  public void testNothing() {}

  public void testMe()
  {
    "this is impossible".echo();
    "this is impossible".helloWorld();

    assertFalse( getClass().getName().isAlpha() );

    ArrayList<String> list = new ArrayList<>();
    list.add( "hi" );
    list.add( "hello" );
    assertEquals( 'h', list.firstOne().charAt( 0 ) );

    String found = list.firstOne( e -> e.length() > 2 );
    assertEquals( found, "hello" );

    assertEquals( "ok", list.stuff() );
    //assertEquals( "ok", list.stiff( "hi" ) );

    List<Serializable> l = Arrays.asList( "hi", 5 );
    assertEquals( 5, (Object)l.findByType( 3 ) );

    List<String> l2 = new ArrayList<>();
    assertEquals( Arrays.asList( "a", "b" ), l2.<String>append( "a", "b" ) );

    Rectangle rc = new Rectangle( 4, 5, 6, 7 );
    foo( rc );
    Coordinate coord = rc;
    double d = coord.getX();
    System.out.println( d );
    System.out.println( coord.lol( 7 ) );
    IGenericThing<String> gthing = rc;
    List<String> glist = gthing.foo( "hi", new ArrayList<>() );
    System.out.println( glist.get( 0 ) );

    ContributorKind.Primary.hiContributorKind();
    ClassType.Enum.hiClassType();
    new BasicIncrementalCompileDriver(true).hiBasic();
  }

  public void testExtensionWithMethodRef()
  {
    // Consumer
    Stream.of( "this is impossible", "Foo" ).forEach( String::echo );

    // Function
    assertEquals( Arrays.asList( "FooFoo", "BarBar" ),
      Stream.of( "Foo", "Bar" ).map( String::duplicate ).collect( Collectors.toList() ) );

    //UnaryOperator
    UnaryOperator<String> unaryOperator = String::duplicate;
    assertEquals( Arrays.asList( "FooFoo", "BarBar" ),
      Stream.of( "Foo", "Bar" ).map( unaryOperator ).collect( Collectors.toList() ) );

    // BiFunction
    Map<String, Integer> map = new LinkedHashMap<>();
    map.add( "Foo", 1 );
    map.add( "Bar", 2 );
    assertEquals( Arrays.asList( "Foo1", "Bar2" ),
      map.map( String::concatenate ).collect( Collectors.toList() ) );

    // interface
    assertEquals( "public method", Stream.of( new MyInterface() { } )
      .map( MyInterface::myPublicMethod ).collect( Collectors.joining() ) );

    // static class method
    assertEquals( Collections.singletonList( "static class method - 1" ),
      Stream.of( 1 ).map( String::staticClassMethod ).collect( Collectors.toList() ) );

    // structural interface
    assertEquals( Collections.singletonList( "myMethod" ),
      Stream.of( 2 ).map( NumberStructural::myMethod ).collect( Collectors.toList() ) );
  }

  public void testMethodRefWithoutExtension()
  {
    // this method
    assertEquals( Collections.singletonList( "testbar" ),
      Stream.of( "test" ).map( this::appendBarNonStatic ).collect( Collectors.toList() ) );

    // super method
    assertEquals( Collections.singletonList( "testbarSuper" ),
      Stream.of( "test" ).map( super::appendBarSuper ).collect( Collectors.toList() ) );
    // super method in other package
    assertEquals( Collections.singletonList( "foo supertestfoo supertest2" ),
      Stream.of( new SubClass2() ).map( SubClass2::foo ).map( SuperClass2.DeferredJoiner::get).collect( Collectors.toList() ) );

    // Supplier
    assertEquals( new LinkedHashSet<>( Arrays.asList( "foo", "bar" ) ),
      Stream.of( "foo", "bar" ).collect( LinkedHashSet::new, Set::add, Set::addAll ));

    // Static method call
    assertEquals( Arrays.asList( 4, 3 ),
      Stream.of( "test","foo" ).map(String::length).collect( Collectors.toList() ) );

    // System.out.println
    Stream.of( "1", "2" ).forEach( System.out::println );

    // Array constructor
    assertArrayEquals( new String[]{ "1", "2" },
      Stream.of( "1", "2" ).toArray(String[]::new) );

    // shared interface, IntersectionClassType
    assertEquals( Arrays.asList( "A", "B", "C", "D" ),
      Stream.of( Enum1.A, Enum1.B, Enum2.C, Enum2.D ).map( EnumIntf::getValue ).collect( Collectors.toList() ) );

    // Optional
    Function<String, Optional<String>> optionalSupplier = v -> Optional.of( "test" );
    assertEquals( Optional.of( "test" ),
      Optional.of( "5" ).map( optionalSupplier ).orElseGet( Optional::empty ) );

    // BiFunction
    Map<Integer, Integer> map = new LinkedHashMap<>();
    map.add( 1, 1 );
    map.add( 2, 2 );
    assertEquals( Arrays.asList( 2, 4 ),
      map.map( Integer::sum ).collect( Collectors.toList() ) );

    // no error should be thrown during compilation
    Consumer<ItemEvent> itemListener = null;
    new JComboBox<>().addItemListener(itemListener::accept);
  }

  public static String appendBarStatic(String text){
    return text + "bar";
  }

  public String appendBarNonStatic(String text){
    return text + "bar";
  }

  interface MyInterface
  {
  }

  interface EnumIntf {
    String getValue();
  }

  enum Enum1 implements EnumIntf {
    A, B;

    @Override
    public String getValue()
    {
      return name();
    }
  }

  enum Enum2 implements EnumIntf {
    C,D;

    @Override
    public String getValue()
    {
      return name();
    }
  }

  @Structural
  public interface NumberStructural
  {
    String myMethod();
  }

  public void testSelfTypeOnExtension()
  {
    LinkedList<String> linkedList = new LinkedList<>();
    LinkedList<String> ret = linkedList.plus( "hi" );
    assertEquals( "hi", ret.getFirst() );
    List<LinkedList<String>> ret2 = linkedList.plusPlus( "hi" );

    HashMap<Integer,String> map1 = new HashMap<Integer, String>()
      .add(1, "One")
      .add(2, "Two")
      .add(3, "Three");
    map1.get( 1 );
  }

  public void testStructural()
  {
    ImplementStructurally is = new ImplementStructurally();
    Coordinate coord = (Coordinate)is;
    assertEquals( 1.0, coord.getX() );
    assertEquals( 2.0, coord.getY() );
    assertEquals( "lol3", coord.lol( 3 ) );

    // asserts object identity is not lost
    assertSame( coord, is );

    Point pt = new Point();
    coord = (Coordinate)pt;
    assertEquals( 3.0, coord.getX() );
    assertEquals( 4.0, coord.getY() );
    assertEquals( "lol5", coord.lol( 5 ) );

    // asserts object identity is not lost
    assertSame( coord, pt );

    TestFields<String> tf = new TestFields<>();
    ITestFields<String> itf = (ITestFields<String>)tf;
    itf.setWidth( 2 );
    assertEquals( 2, itf.getWidth() );
    itf.setName( "hi" );
    assertEquals( "hi", itf.getName() );
    itf.setFoo( Arrays.asList( "a", "b" ) );
    assertEquals( Arrays.asList( "a", "b" ), itf.getFoo() );

    // asserts object identity is not lost
    assertSame( itf, tf );
  }

  public void testStructuralIterator()
  {
    StringBuilder sb = new StringBuilder();
    for( Character c : "hello" ) // String extension impls iterator()
    {
      sb.append( c );
    }
    assertEquals( "hello", sb.toString() );
  }

  public void testHashMap()
  {
    HashMap<String, String> map = new HashMap<>();
    map.fubar();
  }

  public void testCodeGenSupportsClassWithAnnotatedMethodHavingEnumConstants()
  {
    ClassWithAnnotatedMethod classWithAnno = new ClassWithAnnotatedMethod();
    assertSame( classWithAnno, classWithAnno.myExtensionMethod() );
  }

  public void testStructuralOnExistingInterface()
  {
    Callable<String> callable = (Callable<String>)new MyCallable();
    try
    {
      assertEquals( "callable", callable.call() );
    }
    catch( Exception e )
    {
      throw new RuntimeException( e );
    }
  }

  public void testStructuralOnExistingInterfaceHasCompileErrorForUsingOutsideProjectScope()
  {
    try
    {
      Executors.privilegedCallable( (Callable<String>)new MyCallable() ).call();
      fail();
    }
    catch( IncompatibleClassChangeError e )
    {
      // expected, cannot use structural type outside module scope of declared @Structural
    }
    catch( Exception e )
    {
      fail();
    }
  }

  public void testClassExtension()
  {
    assertEquals( "whatever", SimpleTest.class.whateverClassMethod() );
    assertEquals( "whatever", getClass().whateverClassMethod() );
  }

  public void testEmptyArrayMethod()
  {
    String[] emptyStringArray = String.emptyArray();
    assertArrayEquals( new String[0], emptyStringArray );
    
    SimpleTest[] emptySimpleTestArray = SimpleTest.emptyArray();
    assertArrayEquals( new SimpleTest[0], emptySimpleTestArray );
  }

  public void testStaticMethod()
  {
    List<String> l = Arrays.asList( "hi", "bye" );
    assertEquals( "hi, bye", String.valueOf( l ) );
  }

  public void testSuperClassWithCtorParamReferencingTypeVar()
  {
    SubClass sc = new SubClass();
    assertEquals( "myMethod", sc.myMethod() );
  }

  public void testCompileTimeConstantFieldInitializersPreserved()
  {
    // verify test extension exists on Integer
    assertEquals( "myMethod", Integer.getInteger( "1" ).myMethod() );

    assertEquals( 0x7fffffff, Integer.MAX_VALUE );
    assertEquals( 0x80000000, Integer.MIN_VALUE );
    assertEquals( 4, Integer.BYTES );

    assertEquals( "myMethod", new ClassWithConstants().myMethod() );

    assertTrue( ClassWithConstants.BOOL_VALUE1 );
    assertFalse( ClassWithConstants.BOOL_VALUE2 );
    assertTrue( ClassWithConstants.BOOL_VALUE3 );

    assertEquals( 10, ClassWithConstants.BYTE_VALUE1 );
    assertEquals( Byte.MAX_VALUE, ClassWithConstants.BYTE_VALUE2 );

    assertEquals( 1000, ClassWithConstants.SHORT_VALUE1 );
    assertEquals( -1000, ClassWithConstants.SHORT_VALUE2 );
    assertEquals( Short.MAX_VALUE, ClassWithConstants.SHORT_VALUE3 );

    assertEquals( 32768, ClassWithConstants.INT_VALUE1 );
    assertEquals( -32769, ClassWithConstants.INT_VALUE2 );
    assertEquals( Integer.MAX_VALUE, ClassWithConstants.INT_VALUE3 );
    assertEquals( Integer.MAX_VALUE, ClassWithConstants.INT_VALUE4 );

    assertEquals( 8575799808933029326L, ClassWithConstants.LONG_VALUE1 );
    assertEquals( 8575799808933029326L, ClassWithConstants.LONG_VALUE2 );

    assertEquals( 3.4028235e+38f, ClassWithConstants.FLOAT_VALUE1 );
    assertEquals( Float.MIN_VALUE, ClassWithConstants.FLOAT_VALUE2 );
    assertEquals( Float.MIN_VALUE, ClassWithConstants.FLOAT_VALUE3 );

    assertEquals( 1.7976931348623157e+308, ClassWithConstants.DOUBLE_VALUE1 );
    assertEquals( Double.MAX_VALUE, ClassWithConstants.DOUBLE_VALUE2 );

    assertEquals( 's', ClassWithConstants.CHAR_VALUE1 );
    assertEquals( '\n', ClassWithConstants.CHAR_VALUE2 );
    assertEquals( '\u263A', ClassWithConstants.CHAR_VALUE3 );
    assertEquals( '\u263A', ClassWithConstants.CHAR_VALUE4 );

    assertNull( ClassWithConstants.STRING_VALUE0 );
    assertEquals( "", ClassWithConstants.STRING_VALUE1 );
    assertEquals( "abc", ClassWithConstants.STRING_VALUE2 );
    assertEquals( "\u263Aabc\u263A\ndef", ClassWithConstants.STRING_VALUE3 );
    assertEquals( "\u263Aabc\u263A\ndef", ClassWithConstants.STRING_VALUE4 );

    assertNull( ClassWithConstants.OBJ_VALUE1 );
    assertTrue( ClassWithConstants.OBJ_VALUE2 instanceof ClassWithConstants );
  }

  public void testAbstractEnum()
  {
    assertEquals( AbstractEnum.A, AbstractEnum.A.myExtensionMethod() );
  }

  public void testAnnotatedParameters()
  {
    Double dub = 1.2;
    List<String> list = new ArrayList<>();
    dub.myMethod( list );
  }
  
  private double foo( Coordinate c )
  {
    return c.getX();
  }

  @Structural
  public interface ITestFields<T extends CharSequence>
  {
    int getWidth();
    void setWidth( int i );

    T getName();
    void setName( T t );

    <E extends List<T>> E getFoo();
    <E extends List<T>> void setFoo( E e );
  }

  public static class TestFields<Z extends CharSequence> // implements ITestFields
  {
    public int width;
    public Z name;
    public List<Z> foo;
  }

  public static class ImplementStructurally //implements Coordinate
  {
    public double getX()
    {
      return 1;
    }
    public double getY()
    {
      return 2;
    }
    public String lol( Integer i )
    {
      return "lol" + i;
    }
  }

  public static class Point // implements Coordinate
  {
    public int x = 3;
    public int y = 4;
    public String lol( Integer i )
    {
      return "lol" + i;
    }
  }

  public static class MyRunnable // implemnets Runnable
  {
    public void run()
    {
      System.out.println( "runnable" );
    }
  }

  public static class MyCallable // implemnets Runnable
  {
    public String call()
    {
      return "callable";
    }
  }
}
