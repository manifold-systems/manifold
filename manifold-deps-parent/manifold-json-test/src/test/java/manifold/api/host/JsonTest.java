package manifold.api.host;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import manifold.rt.api.Bindings;

import abc.*;
import junit.framework.TestCase;
import manifold.json.rt.api.Base64Encoding;
import manifold.json.rt.api.OctetEncoding;
import manifold.util.ReflectUtil;


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
    assertEquals( resVt, vt );

    uriFormats.setNc_VehicleAsnc_VehicleType( vt );
    resVt = (StrangeUriFormats.nc_VehicleType)uriFormats.getNc_VehicleAsnc_VehicleType();
    assertEquals( resVt, vt );

    StrangeUriFormats.nc_VehicleType lvt = (StrangeUriFormats.nc_VehicleType)Collections.singletonList( vt );
    uriFormats.setNc_VehicleAsnc_VehicleType( lvt );
    StrangeUriFormats.nc_VehicleType resLvt = (StrangeUriFormats.nc_VehicleType)uriFormats.getNc_VehicleAsnc_VehicleType();
    assertEquals( resLvt, lvt );
  }

  public void testOneOf()
  {
    OneOf oneOf = OneOf.create();

    OneOf.MyDef myDef = OneOf.MyDef.builder().withChocolate("milk").build();

    oneOf.setOneOfRefAsMyDef(myDef);
    String choco = oneOf.getOneOfRefAsMyDef().getChocolate();
    assertEquals( "milk", choco );
    oneOf.setOneOfRefAsString("hi");
    assertEquals( "hi", oneOf.getOneOfRefAsString() );
    assertEquals( "hi", (String)oneOf.getOneOfRef() );

    oneOf.setThingAsBoolean( Boolean.TRUE );
    assertTrue( oneOf.getThingAsBoolean() );

    myDef = OneOf.MyDef.create();
    oneOf.setThingAsMyDef(myDef);
    OneOf.MyDef myDefRes = oneOf.getThingAsMyDef();
    assertEquals( myDef, myDefRes );

    OneOf.thing.Option0 thingOption0 = OneOf.thing.Option0.create("fred");
    thingOption0.setLastName("flintstone");
    thingOption0.setSport("bowling");
    oneOf.setThingAsOption0(thingOption0);
    OneOf.thing.Option0 resThingOption0 = oneOf.getThingAsOption0();
    assertEquals( thingOption0, resThingOption0 );

    OneOf.thing.Option1 thingOption1 = OneOf.thing.Option1.create();
    thingOption1.setPrice(5);
    thingOption1.setVehicle("ferrari");
    oneOf.setThingAsOption1(thingOption1);
    OneOf.thing.Option1 resThingOption1 = oneOf.getThingAsOption1();
    assertEquals( thingOption1, resThingOption1 );

    OneOf_TopLevel.Option0 topLevelOption0 = OneOf_TopLevel.Option0.create("Bob");
    oneOf.setTopLevelAsOption0( topLevelOption0 );
    OneOf_TopLevel.Option0 resTopLevelOption0 = oneOf.getTopLevelAsOption0();
    assertEquals( topLevelOption0, resTopLevelOption0 );
    assertEquals( topLevelOption0, oneOf.getTopLevel() );

    OneOf_TopLevel.Option1 topLevelOption1 = OneOf_TopLevel.Option1.create();
    oneOf.setTopLevelAsOption1( topLevelOption1 );
    OneOf_TopLevel.Option1 resTopLevelOption1 = oneOf.getTopLevelAsOption1();
    assertEquals( topLevelOption1, resTopLevelOption1 );
    assertEquals( topLevelOption1, oneOf.getTopLevel() );

    List<OneOf_TopLevel_Array.OneOf_TopLevel_ArrayItem.Option0> topLevelArrayOption0 = Collections.singletonList( OneOf_TopLevel_Array.OneOf_TopLevel_ArrayItem.Option0.create("Scott") );
    oneOf.setTopLevelArrayAsOption0( topLevelArrayOption0 );
    List<OneOf_TopLevel_Array.OneOf_TopLevel_ArrayItem.Option0> resTopLevelArrayOption0 = oneOf.getTopLevelArrayAsOption0();
    assertEquals( topLevelArrayOption0, resTopLevelArrayOption0 );
    assertEquals( topLevelArrayOption0, oneOf.getTopLevelArray() );

    List<OneOf_TopLevel_Array.OneOf_TopLevel_ArrayItem.Option1> topLevelArrayOption1 = Collections.singletonList( OneOf_TopLevel_Array.OneOf_TopLevel_ArrayItem.Option1.create() );
    oneOf.setTopLevelArrayAsOption1( topLevelArrayOption1 );
    List<OneOf_TopLevel_Array.OneOf_TopLevel_ArrayItem.Option1> resTopLevelArrayOption1 = oneOf.getTopLevelArrayAsOption1();
    assertEquals( topLevelArrayOption1, resTopLevelArrayOption1 );
    assertEquals( topLevelArrayOption1, oneOf.getTopLevelArray() );

    List<Enum_TopLevel_Array.Enum_TopLevel_ArrayItem> enumArray = Arrays.asList( Enum_TopLevel_Array.Enum_TopLevel_ArrayItem.a, Enum_TopLevel_Array.Enum_TopLevel_ArrayItem.e );
    oneOf.setTopLevelEnumArray( enumArray );
    assertEquals( enumArray, oneOf.getTopLevelEnumArray() );
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
    assertEquals( shipping_address, all.getShipping_address() );
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
    assertEquals( result, outside );
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

    Hobby.HobbyItem baseball = Hobby.HobbyItem.create();
    baseball.setCategory( "Sport" );
    Hobby.HobbyItem fishing = Hobby.HobbyItem.create();
    fishing.setCategory( "Recreation" );
    Hobby hobbies = (Hobby)asList( baseball, fishing );
    person.setHobby( hobbies );
    Hobby h = (Hobby)person.getHobby();
    assertEquals( 2, h.size() );
    assertEquals( baseball, h.get( 0 ) );
    assertEquals( fishing, h.get( 1 ) );
  }

  public void testRef()
  {
    Contact contact = Contact.builder()
      .withName("Scott McKinney")
      .withDateOfBirth(LocalDate.of(1986, 8, 9))
      .withNumDependents(2)
      .withPrimaryAddress(Contact.Address.create("111 Main St.", "Cupertino", "CA")).build();
    Contact.Address primaryAddress = contact.getPrimaryAddress();
    assertEquals( "111 Main St.", primaryAddress.getStreet_address() );
    assertEquals( "Cupertino", primaryAddress.getCity() );
    assertEquals( "CA", primaryAddress.getState() );
    assertEquals( "Scott McKinney", contact.getName() );
    assertEquals( LocalDate.of(1986, 8, 9), contact.getDateOfBirth() );
    assertEquals( "{\n" +
                  "  \"Name\": \"Scott McKinney\",\n" +
                  "  \"DateOfBirth\": \"1986-08-09\",\n" +
                  "  \"NumDependents\": 2,\n" +
                  "  \"PrimaryAddress\": {\n" +
                  "    \"street_address\": \"111 Main St.\",\n" +
                  "    \"city\": \"Cupertino\",\n" +
                  "    \"state\": \"CA\"\n" +
                  "  }\n" +
                  "}", contact.write().toJson() );

    String xml = contact.write().toXml();
    contact = Contact.load().fromXml(xml);
    assertEquals( 2, contact.getNumDependents() );
  }

  public void testThing()
  {
    Product.ProductItem thing = Product.ProductItem.create( 123d, "json", 5.99 );
    thing.setPrice( 1.55 );
    assertEquals( 1.55, thing.getPrice() );

    Product.ProductItem.dimensions dims = Product.ProductItem.dimensions.create(1.2, 2.3, 3.4);
    dims.setLength( 3.0 );
    dims.setWidth( 4.0 );
    dims.setHeight( 5.0 );
    thing.setDimensions( dims );
    Product.ProductItem.dimensions dims2 = thing.getDimensions();
    assertEquals( dims, dims2 );
  }

  public void testStructuralInterfaceCasting()
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
                  "}", person.write().toJson() );

    assertEquals( "<person Name=\"Joe Namath\"/>\n", person.write().toXml( "person" ) );
  }

  // root array with dissimilar component types (object and array)
  public void testMixedArray()
  {
    MixedArray mixedArray = MixedArray.load().fromJson(
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
    assertEquals( 1, (int)mixedArray.getAsMixedArrayItem0(0).getPage() );
    MixedArray.MixedArrayItem1 countries = (MixedArray.MixedArrayItem1) mixedArray.getAsMixedArrayItem1(1);
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
    assertEquals( 4.0, hasEnum.getBindings().get( "bar" ) );

    hasEnum.setFoo( HasEnum.MyEnum.red );
    assertEquals( HasEnum.MyEnum.red, hasEnum.getFoo() );
    assertEquals( "red", hasEnum.getBindings().get( "foo" ) );

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

    assertEquals( 5, Enum_TopLevel_Array.Enum_TopLevel_ArrayItem.values().length );
    List<Enum_TopLevel_Array.Enum_TopLevel_ArrayItem> enumArray =
      Arrays.asList( Enum_TopLevel_Array.Enum_TopLevel_ArrayItem.a, Enum_TopLevel_Array.Enum_TopLevel_ArrayItem.b,
        Enum_TopLevel_Array.Enum_TopLevel_ArrayItem.c, Enum_TopLevel_Array.Enum_TopLevel_ArrayItem.d, Enum_TopLevel_Array.Enum_TopLevel_ArrayItem.e );
    assertArrayEquals( Enum_TopLevel_Array.Enum_TopLevel_ArrayItem.values(), enumArray.toArray( new Enum_TopLevel_Array.Enum_TopLevel_ArrayItem[0] ) );
  }

  public void testDateTimeFormat()
  {
    HasFormats hasFormats = HasFormats.create();

    LocalDateTime value = LocalDateTime.of(2000, 2, 20, 1, 2);
    assertNull( hasFormats.getTheDateAndTime() );
    hasFormats.setTheDateAndTime( value );
    assertEquals( value, hasFormats.getTheDateAndTime() );
    String actualValue = (String)hasFormats.getBindings().get( "TheDateAndTime" );
    assertEquals( value, LocalDateTime.parse( actualValue ) );

    assertNull( hasFormats.getAnotherDateTime() );
    hasFormats.setAnotherDateTime( value );
    assertEquals( value, hasFormats.getAnotherDateTime() );
    actualValue = (String)hasFormats.getBindings().get( "AnotherDateTime" );
    assertEquals( value, LocalDateTime.parse( actualValue ) );
  }
  public void testDateTimeArrayFormat()
  {
    HasFormats hasFormats = HasFormats.create();
    hasFormats.setTheDateAndTimeArray( Arrays.asList( LocalDateTime.of(2000, 2, 20, 1, 2) ) );
    hasFormats.getTheDateAndTimeArray().add( LocalDateTime.of(2000, 2, 20, 1, 3) );
    assertEquals( LocalDateTime.of(2000, 2, 20, 1, 2), hasFormats.getTheDateAndTimeArray().get( 0 ) );
    assertEquals( Arrays.asList( LocalDateTime.of(2000, 2, 20, 1, 2), LocalDateTime.of(2000, 2, 20, 1, 3) ), hasFormats.getTheDateAndTimeArray() );
  }
  public void testDateTimeArray2Format()
  {
    HasFormats hasFormats = HasFormats.create();
    HasFormats.MyObj scott = HasFormats.MyObj.builder().withName( "scott" ).build();
    hasFormats.setTheDateAndTimeArray2( Arrays.asList( scott ) );
    HasFormats.MyObj bob = HasFormats.MyObj.builder().withName( "bob" ).build();
    hasFormats.getTheDateAndTimeArray2().add( bob );
    assertEquals( scott, hasFormats.getTheDateAndTimeArray2().get( 0 ) );
    assertEquals( Arrays.asList( scott, bob ), hasFormats.getTheDateAndTimeArray2() );
  }
  public void testDateFormat()
  {
    HasFormats hasFormats = HasFormats.create();

    LocalDate value = LocalDate.of(1999, 5, 22);
    assertNull( hasFormats.getTheDate() );
    hasFormats.setTheDate( value );
    assertEquals( value, hasFormats.getTheDate() );
    String actualValue = (String)hasFormats.getBindings().get( "TheDate" );
    assertEquals( value, LocalDate.parse( actualValue ) );
  }
  public void testTimeFormat()
  {
    HasFormats hasFormats = HasFormats.create();

    LocalTime value = LocalTime.of( 1, 2, 3, 4 );
    assertNull( hasFormats.getTheTime() );
    hasFormats.setTheTime( value );
    assertEquals( value, hasFormats.getTheTime() );
    String actualValue = (String)hasFormats.getBindings().get( "TheTime" );
    assertEquals( value, LocalTime.parse( actualValue ) );
  }
  public void testUtiMillisec()
  {
    HasFormats hasFormats = HasFormats.create();

    Instant value = Instant.now();
    assertNull( hasFormats.getTheTimestamp() );
    hasFormats.setTheTimestamp( value );
    assertEquals( value, hasFormats.getTheTimestamp() );
    long actualValue = (long)hasFormats.getBindings().get( "TheTimestamp" );
    assertEquals( value, Instant.ofEpochMilli( actualValue ) );
  }
  public void testInt64()
  {
    HasFormats hasFormats = HasFormats.create();

    assertEquals( null, hasFormats.getInt64() );
    hasFormats.setInt64( (Long)Long.MAX_VALUE );
    assertEquals( (Long)Long.MAX_VALUE, hasFormats.getInt64() );
    assertTrue( hasFormats.getBindings().get( "int64" ) instanceof Long );
  }

  public void testBigInteger()
  {
    HasBigNumbers hasBig = HasBigNumbers.create();

    BigInteger value = new BigInteger( "1000000000000000000000" );
    assertNull( hasBig.getBigInt() );
    hasBig.setBigInt( value );
    assertEquals( value, hasBig.getBigInt() );
    String actualValue = (String)hasBig.getBindings().get( "bigInt" );
    assertEquals( value, new BigInteger( actualValue ) );

    assertNull( hasBig.getAnotherBigInt() );
    hasBig.setAnotherBigInt( value );
    assertEquals( value, hasBig.getAnotherBigInt() );
    actualValue = (String)hasBig.getBindings().get( "anotherBigInt" );
    assertEquals( value, new BigInteger( actualValue ) );
  }
  public void testBigDecimal()
  {
    HasBigNumbers hasBig = HasBigNumbers.create();

    BigDecimal value = new BigDecimal( "1000000000000000000000.0000000000000000000001" );
    assertNull( hasBig.getBigDec() );
    hasBig.setBigDec( value );
    assertEquals( value, hasBig.getBigDec() );
    String actualValue = (String)hasBig.getBindings().get( "bigDec" );
    assertEquals( value, new BigDecimal( actualValue ) );

    assertNull( hasBig.getAnotherBigDec() );
    hasBig.setAnotherBigDec( value );
    assertEquals( value, hasBig.getAnotherBigDec() );
    actualValue = (String)hasBig.getBindings().get( "anotherBigDec" );
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

  public void testBinary()
  {
    HasBinaryFormats hasBinary = HasBinaryFormats.create();

    String text = "The quick brown fox jumps over the lazy dog 0123456789 ;.,!@#$%^&*()_+=-\\|][{}\":?/><`~";
    String base64 = "VGhlIHF1aWNrIGJyb3duIGZveCBqdW1wcyBvdmVyIHRoZSBsYXp5IGRvZyAwMTIzNDU2Nzg5IDsuLCFAIyQlXiYqKClfKz0tXHxdW3t9Ijo/Lz48YH4=";
    hasBinary.setByteFormat( Base64Encoding.encoded( base64 ) );
    assertEquals( base64, hasBinary.getByteFormat().toString() );
    assertEquals( text, new String( hasBinary.getByteFormat().getBytes() ) );
    assertEquals( base64, hasBinary.getBindings().get( "byteFormat" ) );
    // repeat to test releasing of memory
    assertEquals( base64, hasBinary.getByteFormat().toString() );
    assertEquals( text, new String( hasBinary.getByteFormat().getBytes() ) );
    assertEquals( base64, hasBinary.getBindings().get( "byteFormat" ) );

    String octet = OctetEncoding.decoded( text.getBytes() ).toString();
    hasBinary.setBinaryFormat( OctetEncoding.encoded( octet ) );
    assertEquals( octet, hasBinary.getBinaryFormat().toString() );
    assertEquals( text, new String( hasBinary.getBinaryFormat().getBytes() ) );
    assertEquals( octet, hasBinary.getBindings().get( "binaryFormat" ) );
    // repeat to test releasing of memory
    assertEquals( octet, hasBinary.getBinaryFormat().toString() );
    assertEquals( text, new String( hasBinary.getBinaryFormat().getBytes() ) );
    assertEquals( octet, hasBinary.getBindings().get( "binaryFormat" ) );
  }

  public void testReadOnly()
  {
    HasReadOnlyEtc readOnlyEtc = HasReadOnlyEtc.builder().withIntegerValue_ReadOnly(0).build();
    readOnlyEtc.get("absent");
    readOnlyEtc.getIntegerValue_ReadOnly();
    readOnlyEtc.getStringValue_ReadOnly();
    readOnlyEtc.getNoAdditional();
    readOnlyEtc.getNoAdditionalPlusPatternProperties();
    readOnlyEtc.getTheDateAndTime_NotReadOnly();
    readOnlyEtc.getTheDateAndTime_ReadOnly();
    readOnlyEtc.getTheTimestamp_NotReadOnly();
    readOnlyEtc.getTheTimestamp_ReadOnly();
    readOnlyEtc.getBindings();
    readOnlyEtc.getClass();
    assertEquals( 11, Arrays.stream( HasReadOnlyEtc.class.getMethods() ).filter( m -> m.getName().startsWith("get") ).count() );

    readOnlyEtc.setInteger_WriteOnly(8);
    readOnlyEtc.setString_WriteOnly("hi");
    readOnlyEtc.setTheDateAndTime_NotReadOnly(LocalDateTime.now());
    readOnlyEtc.setTheTimestamp_NotReadOnly(Instant.now());
    assertEquals( 4, Arrays.stream( HasReadOnlyEtc.class.getMethods() ).filter( m -> m.getName().startsWith("set") ).count() );
  }

  public void testAdditionalProperties()
  {
    HasReadOnlyEtc readOnlyEtc = HasReadOnlyEtc.builder().withIntegerValue_ReadOnly(0).build();
    assertEquals( 0, readOnlyEtc.get( "IntegerValue_ReadOnly" ) );
    readOnlyEtc.put( "IntegerValue_ReadOnly", 50 );
    assertEquals( 50, readOnlyEtc.get( "IntegerValue_ReadOnly" ) );
    assertNull( ReflectUtil.method( HasReadOnlyEtc.NoAdditional.class, "get", String.class ) );
    assertNull( ReflectUtil.method( HasReadOnlyEtc.NoAdditional.class, "put", String.class, Object.class ) );
    assertNotNull( ReflectUtil.method( HasReadOnlyEtc.NoAdditionalPlusPatternProperties.class, "get", String.class ) );
    assertNotNull( ReflectUtil.method( HasReadOnlyEtc.NoAdditionalPlusPatternProperties.class, "put", String.class, Object.class ) );
  }

  public void testBuilders()
  {
    FootballPlayer footballPlayer =
            FootballPlayer.builder("Joe", "Smith",
                    FootballPlayer.football_team.builder("east", "lions").build())
                    .withAge(25).build();

    // Since the person interface is an inner class of FootballPlayer, FootballPlayer cannot extend it.  Here we
    // test that FootballPlayer still implements person structurally.
    FootballPlayer.person castPerson = (FootballPlayer.person)footballPlayer;
    assertEquals( "Joe", castPerson.getFirst_name() );
    assertEquals( "Smith", castPerson.getLast_name() );
    assertEquals( 25, castPerson.getAge() );

    FootballPlayer.person person = FootballPlayer.person.create("scott", "mckinney");
    person = FootballPlayer.person.builder("scott", "mckinney").withAge(29).build();

//    angular.create(1).setSchematics(abc.angular.schematicOptions.create());
//    angular.create().
//
//    apple_app_site_association.create(apple_app_site_association.applinks.create(apple_app_site_association.applinks.apps.__, Arrays.asList(apple_app_site_association.applinks.details.builder().withAppID("foo").build())));
//    resume.builder().withBasics(
//            resume.basics.builder().withProfiles(Arrays.asList(resume.basics.profiles.builder().build())).build() );
  }

  public void testDataBindingsEqual()
  {
    MyObj myObj = MyObj.builder("hi").withBar(LocalDate.of(2020, 12, 3)).build();
    MyObj myObj2 = MyObj.builder("hi").withBar(LocalDate.of(2020, 12, 3)).build();
    assertEquals(myObj, myObj2);
  }

  public void testUsesDefs()
  {
    UsesDefs usesDefs = UsesDefs.builder("hi")
      .withThing(UsesDefs.Thing.builder()
        .withField1("f1")
        .withField2(3)
        .build())
      .withStuff(OtherDefs.Stuff.builder("foo")
        .withBar(4)
        .build())
      .withMore(OtherDefs.Stuff.builder("fu")
        .withBar(5)
        .build())
      .build();
    assertEquals( "f1", usesDefs.getThing().getField1() );
    assertEquals( 3, usesDefs.getThing().getField2() );
    assertEquals( "foo", usesDefs.getStuff().getFoo() );
    assertEquals( 4, usesDefs.getStuff().getBar() );
    assertEquals( "fu", usesDefs.getMore().getFoo() );
    assertEquals( 5, usesDefs.getMore().getBar() );
  }

  public void testUsesDefsEquality()
  {
    UsesDefs usesDefs = UsesDefs.builder("hi")
      .withThing(UsesDefs.Thing.builder()
        .withField1("f1")
        .withField2(3)
        .build())
      .withStuff(OtherDefs.Stuff.builder("foo")
        .withBar(4)
        .build())
      .withMore(OtherDefs.Stuff.builder("fu")
        .withBar(5)
        .build())
      .build();
    UsesDefs usesDefs2 = UsesDefs.builder("hi")
      .withThing(UsesDefs.Thing.builder()
        .withField1("f1")
        .withField2(3)
        .build())
      .withStuff(OtherDefs.Stuff.builder("foo")
        .withBar(4)
        .build())
      .withMore(OtherDefs.Stuff.builder("fu")
        .withBar(5)
        .build())
      .build();
    assertEquals( usesDefs, usesDefs2 );
  }

  public void testNestedDefinitions()
  {
    HasNestedDefinitions nestedDefs = HasNestedDefinitions.create();
    nestedDefs.setTheNestedObject( HasNestedDefinitions.MyObject.MyNestedObject.create() );
    nestedDefs.getTheNestedObject().setNestedThing( "hi" );
    nestedDefs.getTheNestedObject().getNestedThing();
    nestedDefs.getTheNestedString();
  }

  public void testRefVariations()
  {
    RefVariations.Car car = RefVariations.Car.create();
    RefVariations.Color_rgb color_rgb = RefVariations.Color_rgb.builder().withBlue(1).withGreen(2).withRed(3).build();
    RefVariations.Color2_rgb color2_rgb = RefVariations.Color2_rgb.builder().withBlue(1).withGreen(2).withRed(3).build();
    car.setColor1(color_rgb);
    car.getColor1().getRed();
    car.getColor1().setAlphaAsColor2_rgb(color2_rgb);
    car.getColor1().getAlphaAsColor2_rgb().getBlue();
    car.getColor1().getAlphaAsString();
    String inner = car.getColor1().getInner();

    car.setColor2(color_rgb);
    car.getColor2().getRed();
    car.getColor2().getAlphaAsColor2_rgb().getBlue();
    car.getColor2().getAlphaAsString();
    inner = car.getColor2().getInner();

    car.setInner3("inner3");

    car.setInner4("inner3");

    car.setColor5(color_rgb);
    car.getColor5().getRed();
    car.getColor5().getAlphaAsColor2_rgb().getBlue();
    car.getColor5().getAlphaAsString();
    inner = car.getColor5().getInner();

    car.setColor6(color_rgb);
    car.getColor6().getRed();
    car.getColor6().getAlphaAsColor2_rgb().getBlue();
    car.getColor6().getAlphaAsString();
    inner = car.getColor6().getInner();

    car.setColor7(color_rgb);
    car.getColor7().getRed();
    car.getColor7().getAlphaAsColor2_rgb().getBlue();
    car.getColor7().getAlphaAsString();
    inner = car.getColor7().getInner();

    car.setInner8("inner3");

    car.setAlphaAsColor2_rgb(color2_rgb);

    car.setColor1_1(color2_rgb);
    car.getColor1_1().getRed();
    inner = car.getColor1_1().getInner();

    car.setColor1_2(color2_rgb);
    car.getColor1_2().getRed();
    inner = car.getColor1_2().getInner();

    inner = car.getInner1_3();

    inner = car.getInner1_4();

    car.setColor1_5(color2_rgb);
    car.getColor1_5().getRed();
    inner = car.getColor1_5().getInner();

    inner = car.getInner1_6();
  }

  public void testPrimitiveNumericLists()
  {
    PrimitiveLists good = PrimitiveLists.create("id", 1.0, 1);
    good.setArray_integers( Arrays.asList(1, 2, 3) );
    good.setArray_numbers( Arrays.asList(1.1, 2.2, 3.2) );
    assertEquals(
      "{\n" +
      "  \"string\": \"id\",\n" +
      "  \"number\": 1.0,\n" +
      "  \"integer\": 1,\n" +
      "  \"array_integers\": [\n" +
      "    1,\n" +
      "    2,\n" +
      "    3\n" +
      "  ],\n" +
      "  \"array_numbers\": [\n" +
      "    1.1,\n" +
      "    2.2,\n" +
      "    3.2\n" +
      "  ]\n" +
      "}",
      good.write().toJson() );
  }

  public void testInnerClassExtension()
  {
    Person person = Person.fromSource();
    assertEquals("hi", person.getHobby().hi());

    Map<String, String> map = new HashMap<>();
    map.put( "hi", "bye" );
    assertEquals( "fuebar", map.fuebar() );
    for( Map.Entry<String, String> s: map.entrySet() ) {
      assertEquals("innerFoo", s.innerFoo());
    }
  }
}
