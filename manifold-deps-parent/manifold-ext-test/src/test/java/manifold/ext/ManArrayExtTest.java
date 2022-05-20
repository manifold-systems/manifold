/*
 * Copyright (c) 2020 - Manifold Systems LLC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package manifold.ext;

import junit.framework.TestCase;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import static org.junit.Assert.assertArrayEquals;

public class ManArrayExtTest extends TestCase
{
  public void testExistingMembersStillWork()
  {
    int[] iArray = {1, 2, 3};
    assertEquals( 3, iArray.length );
    assertArrayEquals( iArray, iArray.clone() );
    assertNotSame( iArray, iArray.clone() );

    String[] strArray = {"a", "b", "c", "d"};
    assertEquals( 4, strArray.length );
    assertArrayEquals( strArray, strArray.clone() );
    assertNotSame( strArray, strArray.clone() );
  }

  public void testToList()
  {
    int[] iArray = {1, 2, 3};
    List<Integer> iList = iArray.toList();
    assertEquals( new ArrayList<Integer>() {{add(1); add(2); add(3);}}, iList );

    String[] strArray = {"a", "b", "c", "d"};
    List<String> strList = strArray.toList();
    assertEquals( Arrays.asList( strArray ), strList );
  }

  public void testCopy()
  {
    int[] iArray = {1, 2, 3};
    assertArrayEquals( iArray, iArray.copy() );
    assertNotSame( iArray, iArray.copy() );

    int[] iArrayCopy = iArray.copy( iArray.length + 1);
    assertArrayEquals( new int[]{1, 2, 3, 0}, iArrayCopy );
    iArrayCopy = iArray.copy( -1 );
    assertArrayEquals( new int[]{1, 2, 3}, iArrayCopy );

    String[] strArray = {"a", "b", "c", "d"};
    assertArrayEquals( strArray, strArray.copy() );
    assertNotSame( strArray, strArray.copy() );

    String[] strArrayCopy = strArray.copy( strArray.length + 1 );
    assertArrayEquals( new String[]{"a", "b", "c", "d", null}, strArrayCopy );
  }

  public void testCopyTo()
  {
    int[] iArray = {1, 2, 3, 4};
    assertArrayEquals( iArray, iArray.copyTo( new int[4] ) );

    String[] strArray = {"a", "b", "c", "d"};
    assertArrayEquals( strArray, strArray.copyTo( new String[4] ) );
    assertNotSame( strArray, strArray.copyTo( new String[4] ) );
  }

  public void testCopyRange()
  {
    int[] iArray = {1, 2, 3, 4};
    assertArrayEquals( Arrays.copyOfRange( iArray, 1, 3 ), iArray.copyRange( 1, 3 ) );
    assertArrayEquals( Arrays.copyOfRange( iArray, 1, 4 ), iArray.copyRange( 1, 4) );
    assertArrayEquals( Arrays.copyOfRange( iArray, 1, 4 ), iArray.copyRange( 1, -1 ) );

    String[] strArray = {"a", "b", "c", "d"};
    assertArrayEquals( Arrays.copyOfRange( strArray, 1, 3 ), strArray.copyRange( 1, 3 ) );
    assertArrayEquals( Arrays.copyOfRange( strArray, 1, 4 ), strArray.copyRange( 1, 4) );
    assertArrayEquals( Arrays.copyOfRange( strArray, 1, 4 ), strArray.copyRange( 1, -1 ) );
  }

  public void testCopyRangeTo()
  {
    int[] iArray = {1, 2, 3, 4};
    assertArrayEquals( new int[]{0, 2, 3}, iArray.copyRangeTo( 1, 3, new int[3], 1 ) );

    String[] strArray = {"a", "b", "c", "d"};
    assertArrayEquals( new String[]{null, "b", "c"}, strArray.copyRangeTo( 1, 3, new String[3], 1 ) );
  }

  public void testStream()
  {
    int[] iArray = {1, 2, 3, 4};
    try
    {
      iArray.stream();
      fail(); // primitives not support with stream()
    }
    catch( IllegalArgumentException ignore )
    {
    }

    String[] strArray = {"a", "b", "c", "d"};
    List<String> result = strArray.stream().map( e -> e + e ).collect( Collectors.toList() );
    assertEquals( new ArrayList<String>() {{add("aa"); add("bb"); add("cc"); add("dd");}}, result );
  }

  public void testForEach()
  {
    int[] iArray = {1, 2, 3, 4};
    try
    {
      iArray.forEach( (i, e) -> {} );
      fail(); // primitives not support with forEach()
    }
    catch( IllegalArgumentException ignore )
    {
    }

    String[] strArray = {"a", "b", "c", "d"};
    strArray.forEach( (i, e) -> {strArray[i] = e + e;} );
    assertArrayEquals( new String[] {"aa", "bb", "cc", "dd"}, strArray );
  }

  public void testBinarySearch()
  {
    int[] intArr = {2, 5, 11, 23};
    assertEquals( 2, intArr.binarySearch( 11 ) );
    assertEquals( 2, intArr.binarySearch( 0, 4, 11 ) );

    String[] strArray = {"a", "b", "c", "d"};
    assertEquals( 2, strArray.binarySearch( "c", (s1, s2) -> s1.compareTo(s2) ) );
  }
  
  public void testEquals()
  {
    int[] intArr1 = {2, 5, 11, 23};
    int[] intArr2 = intArr1.copy();
    //noinspection SimplifiableJUnitAssertion,ArrayEquals
    assertTrue( intArr1.equals( intArr2 ) );
    int[] intArr3 = intArr1.copy();
    intArr3[0] = 0;
    //noinspection SimplifiableJUnitAssertion,ArrayEquals
    assertFalse( intArr1.equals( intArr3 ) );

    String[] strArr1 = {"a", "b", "c", "d"};
    String[] strArr2 = strArr1.copy();
    //noinspection SimplifiableJUnitAssertion,ArrayEquals
    assertTrue( strArr1.equals( strArr2 ) );
    String[] strArr3 = strArr1.copy();
    strArr3[0] = null;
    //noinspection SimplifiableJUnitAssertion,ArrayEquals
    assertFalse( strArr1.equals( strArr3 ) );
  }

  public void testToString()
  {
    int[] nullArray = null;
    assertEquals( "null", nullArray.toString() );

    int[] iArray = {1, 2, 3, 4};
    assertEquals( "[1, 2, 3, 4]", iArray.toString() );

    int[][] iArrayArray = {{1}, {2}, null, {4}};
    assertEquals( "[[1], [2], null, [4]]", iArrayArray.toString() );

    String[] strArray = {"1", "2", null, "4"};
    assertEquals( "[1, 2, null, 4]", strArray.toString() );

    String[][] strArrayArray = {{"1"}, {"2"}, {null}, {"4"}};
    assertEquals( "[[1], [2], [null], [4]]", strArrayArray.toString() );
  }
}
