package manifold.api.host;

import abc.Contact;
import abc.Junk;
import abc.Outside;
import abc.Person;
import abc.Product;
import abc.Tree;
import abc.AllOf_Hierarchy;
import abc.OneOf;
import abc.OneOf_TopLevel;
import abc.OneOf_TopLevel_Array;
import abc.Enum_TopLevel_Array;
import abc.StrangeUriFormats;
import abc.MixedArray;
import abc.HasEnum;
import abc.HasFormats;
import abc.HasBigNumbers;
import abc.HasTypeWithNull;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import javax.script.Bindings;
import junit.framework.TestCase;
import org.junit.Assert;


import static abc.Person.*;
import static abc.Person.Address.*;
import static org.junit.Assert.assertArrayEquals;

/**
 */
public class JsonTest extends TestCase
{
  public void testDefinitionsWithInvalidIdentifierCharacters()
  {
    StrangeUriFormats uriFormats = StrangeUriFormats.create();
    StrangeUriFormats.nc_VehicleType vt = StrangeUriFormats.nc_VehicleType.create();
    uriFormats.setNc_Vehicle( vt );
    StrangeUriFormats.nc_VehicleType resVt = (StrangeUriFormats.nc_VehicleType)uriFormats.getNc_Vehicle();
    assertSame( resVt, vt );

    uriFormats.setNc_VehicleAsnc_VehicleType( vt );
    resVt = (StrangeUriFormats.nc_VehicleType)uriFormats.getNc_VehicleAsnc_VehicleType();
    assertSame( resVt, vt );

    List<StrangeUriFormats.nc_VehicleType> lvt = Collections.singletonList( vt );
    uriFormats.setNc_VehicleAsListOfnc_VehicleType( lvt );
    StrangeUriFormats.nc_VehicleType resLvt = (StrangeUriFormats.nc_VehicleType)uriFormats.getNc_VehicleAsListOfnc_VehicleType();
    assertSame( resLvt, lvt );
  }

  public void testOneOf()
  {
    OneOf oneOf = OneOf.create();

    oneOf.setThingAsBoolean( Boolean.TRUE );
    assertTrue( oneOf.getThingAsBoolean() );

    OneOf.MyDef myDef = OneOf.MyDef.create();
    oneOf.setThingAsMyDef(myDef);
    OneOf.MyDef myDefRes = oneOf.getThingAsMyDef();
    assertSame( myDef, myDefRes );

    OneOf.thing.Option0 thingOption0 = OneOf.thing.Option0.create("fred");
    thingOption0.setLastName("flintstone");
    thingOption0.setSport("bowling");
    oneOf.setThingAsOption0(thingOption0);
    OneOf.thing.Option0 resThingOption0 = oneOf.getThingAsOption0();
    assertSame( thingOption0, resThingOption0 );

    OneOf.thing.Option1 thingOption1 = OneOf.thing.Option1.create();
    thingOption1.setPrice(5);
    thingOption1.setVehicle("ferrari");
    oneOf.setThingAsOption1(thingOption1);
    OneOf.thing.Option1 resThingOption1 = oneOf.getThingAsOption1();
    assertSame( thingOption1, resThingOption1 );

    OneOf_TopLevel.Option0 topLevelOption0 = OneOf_TopLevel.Option0.create("Bob");
    oneOf.setTopLevelAsOption0( topLevelOption0 );
    OneOf_TopLevel.Option0 resTopLevelOption0 = oneOf.getTopLevelAsOption0();
    assertSame( topLevelOption0, resTopLevelOption0 );
    assertSame( topLevelOption0, oneOf.getTopLevel() );

    OneOf_TopLevel.Option1 topLevelOption1 = OneOf_TopLevel.Option1.create();
    oneOf.setTopLevelAsOption1( topLevelOption1 );
    OneOf_TopLevel.Option1 resTopLevelOption1 = oneOf.getTopLevelAsOption1();
    assertSame( topLevelOption1, resTopLevelOption1 );
    assertSame( topLevelOption1, oneOf.getTopLevel() );

    List<OneOf_TopLevel_Array.Option0> topLevelArrayOption0 = Collections.singletonList( OneOf_TopLevel_Array.Option0.create("Scott") );
    oneOf.setTopLevelArrayAsOption0( topLevelArrayOption0 );
    List<OneOf_TopLevel_Array.Option0> resTopLevelArrayOption0 = oneOf.getTopLevelArrayAsOption0();
    assertSame( topLevelArrayOption0, resTopLevelArrayOption0 );
    assertSame( topLevelArrayOption0, oneOf.getTopLevelArray() );

    List<OneOf_TopLevel_Array.Option1> topLevelArrayOption1 = Collections.singletonList( OneOf_TopLevel_Array.Option1.create() );
    oneOf.setTopLevelArrayAsOption1( topLevelArrayOption1 );
    List<OneOf_TopLevel_Array.Option1> resTopLevelArrayOption1 = oneOf.getTopLevelArrayAsOption1();
    assertSame( topLevelArrayOption1, resTopLevelArrayOption1 );
    assertSame( topLevelArrayOption1, oneOf.getTopLevelArray() );

    List<Enum_TopLevel_Array> enumArray = Arrays.asList( Enum_TopLevel_Array.a, Enum_TopLevel_Array.e );
    oneOf.setTopLevelEnumArray( enumArray );
    assertSame( enumArray, oneOf.getTopLevelEnumArray() );
  }

  public void testAllOf()
  {
    AllOf_Hierarchy all = AllOf_Hierarchy.create();
    AllOf_Hierarchy.address address = AllOf_Hierarchy.address.create("111 Main St.", "Cupertino", "CA");
    address.setCity("Cupertino");
    all.setBilling_address(address);
    AllOf_Hierarchy.shipping_address shipping_address = AllOf_Hierarchy.shipping_address.create("111 Main St.", "Cupertino", "CA", AllOf_Hierarchy.shipping_address.type.residential);
    shipping_address.setType(AllOf_Hierarchy.shipping_address.type.residential);
    all.setShipping_address(shipping_address);
    assertSame( shipping_address, all.getShipping_address() );
  }

  public void testAllRef()
  {
    Junk junk = Junk.create();
    junk.setElem( Collections.singletonList( Junk.A.create() ) );
    Junk.A a = junk.getElem().get( 0 );
    //assertNotNull( a );

    Junk.B b = Junk.B.create();
    b.setX( Junk.A.create() );
    b.getX().setFoo( "hi" );
    junk.setDing( Collections.singletonList( b ) );
    System.out.println( junk.getDing() );
    assertEquals( "hi", junk.getDing().get( 0 ).getX().getFoo() );

    Outside.Alpha alpha = Outside.Alpha.create();
    Outside outside = Outside.create();
    outside.setGamma( Collections.singletonList( alpha ) );
    junk.setOutside( outside );
    Outside result = junk.getOutside();
    assertSame( result, outside );
  }

  public void testRecursion()
  {
    Tree tree = Tree.create();
    tree.setNode( Tree.node.create() );
    tree.getNode().setInfo( "root" );

    Tree child1 = Tree.create();
    child1.setNode( Tree.node.create() );
    child1.getNode().setInfo( "child1" );

    Tree child2 = Tree.create();
    child2.setNode( Tree.node.create() );
    child2.getNode().setInfo( "child2" );

    tree.setChildren( Arrays.asList( child1, child2 ) );
  }

  public void testJson()
  {
    Person person = Person.create();
    person.getAge();
    person.setName( "Joe Namath" );
    assertEquals( "Joe Namath", person.getName() );

    Address address = Address.create();
    address.setCity( "Dunedin" );
    person.setAddress( address );
    assertEquals( "Dunedin", person.getAddress().getCity() );

    Planet planet = Planet.create();
    address.setPlanet( planet );

    Hobby baseball = Hobby.create();
    baseball.setCategory( "Sport" );
    Hobby fishing = Hobby.create();
    fishing.setCategory( "Recreation" );
    List<Hobby> hobbies = asList( baseball, fishing );
    person.setHobby( hobbies );
    List<Hobby> h = person.getHobby();
    assertEquals( 2, h.size() );
    assertEquals( baseball, h.get( 0 ) );
    assertEquals( fishing, h.get( 1 ) );
  }

  public void testRef()
  {
    Contact contact = Contact.create();
    contact.getDateOfBirth();
    contact.setPrimaryAddress( Contact.Address.create("111 Main St.", "Cupertino", "CA") );
    Contact.Address primaryAddress = contact.getPrimaryAddress();
    primaryAddress.setStreet_address( "111 Foo Dr" );
    primaryAddress.setCity( "Cupertino" );
    primaryAddress.setState( "CA" );
    assertEquals( "111 Foo Dr", primaryAddress.getStreet_address() );
    assertEquals( "Cupertino", primaryAddress.getCity() );
    assertEquals( "CA", primaryAddress.getState() );
    assertEquals( "{\n" +
                  "  \"street_address\": \"111 Foo Dr\",\n" +
                  "  \"city\": \"Cupertino\",\n" +
                  "  \"state\": \"CA\"\n" +
                  "}", primaryAddress.toJson() );
  }

  public void testThing()
  {
    Product thing = Product.create( 123d, "json", 5.99 );
    thing.setPrice( 1.55 );
    assertEquals( 1.55, thing.getPrice() );

    Product.dimensions dims = Product.dimensions.create(1.2, 2.3, 3.4);
    dims.setLength( 3.0 );
    dims.setWidth( 4.0 );
    dims.setHeight( 5.0 );
    thing.setDimensions( dims );
    Product.dimensions dims2 = thing.getDimensions();
    assertSame( dims, dims2 );

    Bindings bindings = (Bindings)thing;
    dims2 = (Product.dimensions)bindings.get( "dimensions" );
    assertSame( dims, dims2 );
  }

  public void testStructuralIntefaceCasting()
  {
    Person person = Person.create();
    IWhatever whatever = (IWhatever)person;
    assertEquals( "foobar", whatever.foobar() );
  }

  public void testToJsonXml()
  {
    Person person = Person.create();
    person.setName( "Joe Namath" );

    assertEquals( "{\n" +
                  "  \"Name\": \"Joe Namath\"\n" +
                  "}", person.toJson() );

    assertEquals( "<object>\n" +
                  "  <Name>Joe Namath</Name>\n" +
                  "</object>\n", person.toXml() );

    assertEquals( "<person>\n" +
                  "  <Name>Joe Namath</Name>\n" +
                  "</person>\n", person.toXml( "person" ) );
  }

  // root array with dissimilar component types (object and array)
  public void testMixedArray()
  {
    MixedArray mixedArray = MixedArray.fromJson(
      "[\n" +
      "  {\n" +
      "    \"page\": 1,\n" +
      "    \"pages\": 7,\n" +
      "    \"per_page\": \"50\",\n" +
      "    \"total\": 304\n" +
      "  },\n" +
      "  [\n" +
      "    {\n" +
      "      \"id\": \"ABW\",\n" +
      "      \"iso2Code\": \"AW\",\n" +
      "      \"name\": \"Aruba\",\n" +
      "      \"region\": {\n" +
      "        \"id\": \"LCN\",\n" +
      "        \"iso2code\": \"ZJ\",\n" +
      "        \"value\": \"Latin America & Caribbean \"\n" +
      "      },\n" +
      "      \"capitalCity\": \"Oranjestad\",\n" +
      "      \"longitude\": \"-70.0167\",\n" +
      "      \"latitude\": \"12.5167\"\n" +
      "    }\n" +
      "  ]\n" +
      "]" );
    assertEquals( 1, (int)mixedArray.getValueAsvalue0().get(0).getPage() );
    List<MixedArray.value1> countries = mixedArray.getValueAsListOfvalue1().get(1);
    assertEquals( "Aruba", countries.get( 0 ).getName() );
    assertEquals( "ZJ", countries.get( 0 ).getRegion().getIso2code() );
  }

  @SuppressWarnings("varargs")
  public static <T> List<T> asList( T... a )
  {
    return Arrays.asList( a );
  }

  public void testEnums()
  {
    HasEnum hasEnum = HasEnum.create();

    hasEnum.setBar( HasEnum.bar._4_0 );
    assertEquals( HasEnum.bar._4_0, hasEnum.getBar() );
    assertEquals( 4.0, ((Map)hasEnum).get( "bar" ) );

    hasEnum.setFoo( HasEnum.MyEnum.red );
    assertEquals( HasEnum.MyEnum.red, hasEnum.getFoo() );
    assertEquals( "red", ((Map)hasEnum).get( "foo" ) );

    assertEquals( 6, HasEnum.baz.values().length );
    assertEquals( "red", HasEnum.baz.red.name() );
    assertEquals( "blue", HasEnum.baz.blue.name() );
    assertEquals( "orange", HasEnum.baz.orange.name() );
    assertEquals( "_6", HasEnum.baz._6.name() );
    assertEquals( "_3", HasEnum.baz._3.name() );
    assertEquals( "_9_0", HasEnum.baz._9_0.name() );

    assertEquals( 6, HasEnum.bazAnyOf.values().length );
    assertEquals( "red", HasEnum.bazAnyOf.red.name() );
    assertEquals( "blue", HasEnum.bazAnyOf.blue.name() );
    assertEquals( "orange", HasEnum.bazAnyOf.orange.name() );
    assertEquals( "_6", HasEnum.bazAnyOf._6.name() );
    assertEquals( "_3", HasEnum.bazAnyOf._3.name() );
    assertEquals( "_9_0", HasEnum.bazAnyOf._9_0.name() );

    assertEquals( 6, HasEnum.bazOneOf.values().length );
    assertEquals( "red", HasEnum.bazOneOf.red.name() );
    assertEquals( "blue", HasEnum.bazOneOf.blue.name() );
    assertEquals( "orange", HasEnum.bazOneOf.orange.name() );
    assertEquals( "_6", HasEnum.bazOneOf._6.name() );
    assertEquals( "_3", HasEnum.bazOneOf._3.name() );
    assertEquals( "_9_0", HasEnum.bazOneOf._9_0.name() );

    assertEquals( 6, HasEnum.justRefs.values().length );
    assertEquals( "red", HasEnum.justRefs.red.name() );
    assertEquals( "orange", HasEnum.justRefs.orange.name() );
    assertEquals( "_6", HasEnum.justRefs._6.name() );
    assertEquals( "_9_0", HasEnum.justRefs._9_0.name() );
    assertEquals( "stuff", HasEnum.justRefs.stuff.name() );
    assertEquals( "things", HasEnum.justRefs.things.name() );

    assertEquals( 6, HasEnum.justRefsAnyOf.values().length );
    assertEquals( "red", HasEnum.justRefsAnyOf.red.name() );
    assertEquals( "orange", HasEnum.justRefsAnyOf.orange.name() );
    assertEquals( "_6", HasEnum.justRefsAnyOf._6.name() );
    assertEquals( "_9_0", HasEnum.justRefsAnyOf._9_0.name() );
    assertEquals( "stuff", HasEnum.justRefsAnyOf.stuff.name() );
    assertEquals( "things", HasEnum.justRefsAnyOf.things.name() );

    assertEquals( 6, HasEnum.justRefsOneOf.values().length );
    assertEquals( "red", HasEnum.justRefsOneOf.red.name() );
    assertEquals( "orange", HasEnum.justRefsOneOf.orange.name() );
    assertEquals( "_6", HasEnum.justRefsOneOf._6.name() );
    assertEquals( "_9_0", HasEnum.justRefsOneOf._9_0.name() );
    assertEquals( "stuff", HasEnum.justRefsOneOf.stuff.name() );
    assertEquals( "things", HasEnum.justRefsOneOf.things.name() );

    assertEquals( 5, Enum_TopLevel_Array.values().length );
    List<Enum_TopLevel_Array> enumArray =
      Arrays.asList( Enum_TopLevel_Array.a, Enum_TopLevel_Array.b, Enum_TopLevel_Array.c, Enum_TopLevel_Array.d, Enum_TopLevel_Array.e );
    assertArrayEquals( Enum_TopLevel_Array.values(), enumArray.toArray( new Enum_TopLevel_Array[0] ) );
  }

  public void testDateTimeFormat()
  {
    HasFormats hasFormats = HasFormats.create();
    
    LocalDateTime value = LocalDateTime.of(2000, 2, 20, 1, 2);
    assertNull( hasFormats.getTheDateAndTime() );
    hasFormats.setTheDateAndTime( value );
    assertEquals( value, hasFormats.getTheDateAndTime() );
    String actualValue = (String)((Map)hasFormats).get( "TheDateAndTime" );
    assertEquals( value, LocalDateTime.parse( actualValue ) );
    
    assertNull( hasFormats.getAnotherDateTime() );
    hasFormats.setAnotherDateTime( value );
    assertEquals( value, hasFormats.getAnotherDateTime() );
    actualValue = (String)((Map)hasFormats).get( "AnotherDateTime" );
    assertEquals( value, LocalDateTime.parse( actualValue ) );
  }
  public void testDateFormat()
  {
    HasFormats hasFormats = HasFormats.create();
    
    LocalDate value = LocalDate.of(1999, 5, 22);
    assertNull( hasFormats.getTheDate() );
    hasFormats.setTheDate( value );
    assertEquals( value, hasFormats.getTheDate() );
    String actualValue = (String)((Map)hasFormats).get( "TheDate" );
    assertEquals( value, LocalDate.parse( actualValue ) );
  }
  public void testTimeFormat()
  {
    HasFormats hasFormats = HasFormats.create();
    
    LocalTime value = LocalTime.of( 1, 2, 3, 4 );
    assertNull( hasFormats.getTheTime() );
    hasFormats.setTheTime( value );
    assertEquals( value, hasFormats.getTheTime() );
    String actualValue = (String)((Map)hasFormats).get( "TheTime" );
    assertEquals( value, LocalTime.parse( actualValue ) );
  }
  public void testUtiMillisec()
  {
    HasFormats hasFormats = HasFormats.create();

    Instant value = Instant.now();
    assertNull( hasFormats.getTheTimestamp() );
    hasFormats.setTheTimestamp( value );
    assertEquals( value, hasFormats.getTheTimestamp() );
    long actualValue = (long)((Map)hasFormats).get( "TheTimestamp" );
    assertEquals( value, Instant.ofEpochMilli( actualValue ) );
  }

  public void testBigInteger()
  {
    HasBigNumbers hasBig = HasBigNumbers.create();

    BigInteger value = new BigInteger( "1000000000000000000000" );
    assertNull( hasBig.getBigInt() );
    hasBig.setBigInt( value );
    assertEquals( value, hasBig.getBigInt() );
    String actualValue = (String)((Map)hasBig).get( "bigInt" );
    assertEquals( value, new BigInteger( actualValue ) );

    assertNull( hasBig.getAnotherBigInt() );
    hasBig.setAnotherBigInt( value );
    assertEquals( value, hasBig.getAnotherBigInt() );
    actualValue = (String)((Map)hasBig).get( "anotherBigInt" );
    assertEquals( value, new BigInteger( actualValue ) );
  }
  public void testBigDecimal()
  {
    HasBigNumbers hasBig = HasBigNumbers.create();

    BigDecimal value = new BigDecimal( "1000000000000000000000.0000000000000000000001" );
    assertNull( hasBig.getBigDec() );
    hasBig.setBigDec( value );
    assertEquals( value, hasBig.getBigDec() );
    String actualValue = (String)((Map)hasBig).get( "bigDec" );
    assertEquals( value, new BigDecimal( actualValue ) );

    assertNull( hasBig.getAnotherBigDec() );
    hasBig.setAnotherBigDec( value );
    assertEquals( value, hasBig.getAnotherBigDec() );
    actualValue = (String)((Map)hasBig).get( "anotherBigDec" );
    assertEquals( value, new BigDecimal( actualValue ) );
  }

  public void testNullable()
  {
    HasTypeWithNull top = HasTypeWithNull.create();

    String nullableString = top.getNullableString();
    assertNull( nullableString );
    top.setNullableString( "hi" );
    assertEquals( "hi", top.getNullableString() );

    nullableString = top.getNullableString_oneOf();
    assertNull( nullableString );
    top.setNullableString_oneOf( "hi" );
    assertEquals( "hi", top.getNullableString_oneOf() );

    Double nullableNumber = top.getNullableNumber();
    assertNull( nullableNumber );
    top.setNullableNumber( 5d );
    assertEquals( 5d, top.getNullableNumber() );

    nullableNumber = top.getNullableNumber_oneOf();
    assertNull( nullableNumber );
    top.setNullableNumber_oneOf( 5d );
    assertEquals( 5d, top.getNullableNumber_oneOf() );

    LocalDateTime now = LocalDateTime.now();
    LocalDateTime nullableDate = top.getNullableDate();
    assertNull( nullableDate );
    top.setNullableDate( now );
    assertEquals( now, top.getNullableDate() );

    nullableDate = top.getNullableDate_oneOf();
    assertNull( nullableDate );
    top.setNullableDate_oneOf( now );
    assertEquals( now, top.getNullableDate_oneOf() );

    nullableDate = top.getNullableDate_inline();
    assertNull( nullableDate );
    top.setNullableDate_inline( now );
    assertEquals( now, top.getNullableDate_inline() );

    Instant timestamp = Instant.now();
    Instant nullableTimestamp = top.getNullableTimestamp();
    assertNull( nullableTimestamp );
    top.setNullableTimestamp( timestamp );
    assertEquals( timestamp, top.getNullableTimestamp() );

    nullableTimestamp = top.getNullableTimestamp_oneOf();
    assertNull( nullableTimestamp );
    top.setNullableTimestamp_oneOf( timestamp );
    assertEquals( timestamp, top.getNullableTimestamp_oneOf() );

    List<Double> list = Arrays.asList( 1d, 2d, 3d );
    List<Double> nullableList = top.getNullableList();
    assertNull( nullableList );
    top.setNullableList( list );
    assertEquals( list, top.getNullableList() );

    nullableList = top.getNullableList_oneOf();
    assertNull( nullableList );
    top.setNullableList_oneOf( list );
    assertEquals( list, top.getNullableList_oneOf() );
  }
}
