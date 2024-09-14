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

package manifold.csv;


import abc.csv.Nnndss;
import abc.csv.Nnndss.NnndssItem;
import abc.csv.CsvTestSchema;
import abc.csv.CsvTestSchema.CsvTestSchemaItem;
import abc.csv.insurance_sample_comma;
import abc.csv.insurance_sample_comma.insurance_sample_commaItem;
import abc.csv.Cake;
import abc.csv.Cake.CakeItem;
import abc.csv.MissingLastColumn;
import java.io.IOException;
import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.net.URISyntaxException;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

import org.junit.Test;

import static org.junit.Assert.*;

public class CsvTest
{
  @Test
  public void testSimpleCsvManifold()
  {
    insurance_sample_comma items = insurance_sample_comma.fromSource();
    assertEquals( 17, items.size() );
    for( insurance_sample_commaItem item: items )
    {
      assertNotNull( item.getPolicyID() );
      assertNotNull( item.getTiv_2012() );
    }
  }

  @Test
  public void testAdvancedFormat() throws IOException, URISyntaxException
  {
    Nnndss nnndss = Nnndss.fromSource();
    for( NnndssItem item: nnndss )
    {
      Integer value = item.getInvasive_pneumococcal_disease__age___5___Confirmed__Current_week();
    }
  }

  @Test
  public void testCsvLoadedFromJsonSchema()
  {
    CsvTestSchema test = CsvTestSchema.load().fromCsvReader( new InputStreamReader( CsvTest.class.getResourceAsStream( "/abc/csv/CsvTestSchemaData.csv" ) ) );
    CsvTestSchemaItem item = test.get( 0 );
    assertEquals( "hello", item.getAString() );
    assertEquals( 5, item.getAInteger() );
    assertEquals( 3.14d, item.getANumber(), 0 );
    assertEquals( true, item.getABoolean() );
    assertEquals( CsvTestSchema.MyEnum.red, item.getAEnumRef() );
    assertEquals( 9223372036854775807L, (long)item.getALong() );
    assertEquals( LocalDateTime.parse( "2007-12-03T10:15:30" ), item.getADateTime() );
    assertEquals( LocalDate.parse( "2007-12-03" ), item.getADate() );
    assertEquals( LocalTime.parse( "10:15:30" ), item.getATime() );
    assertEquals( Instant.ofEpochMilli( 99999999 ), item.getAInstant() );
    assertEquals( new BigInteger( "9223372036854775807123" ), item.getABigInteger() );
    assertEquals( new BigDecimal( "9223372036854775807123.456" ), item.getABigDecimal() );
    assertEquals( "hello", new String( item.getAOctetEncoded().getBytes() ) );
    assertEquals( "scott", new String( item.getABase64Encoded().getBytes() ) );
  }

  @Test
  public void testIndentedFile()
  {
    Object[][] data =
    {
      {"scott",39,"chocolate"},
      {"bob",37,"strawberry"},
    };
    Cake cake = Cake.fromSource();
    for(int i = 0; i < cake.size(); i++) {
      CakeItem item = cake.get(i);
      assertEquals(data[i][0], item.getName());
      assertEquals(data[i][1], item.getAge());
      assertEquals(data[i][2], item.getCake());
    }
  }

  @Test
  public void testMissingLastColumn()
  {
    MissingLastColumn items = MissingLastColumn.fromSource();
    assertEquals( 3, items.size() );

    assertEquals( "", items.get( 0 ).getCol1() );
    assertEquals( 1, (int)items.get( 0 ).getCol2() );
    assertEquals( "", items.get( 0 ).getCol3() );

    assertEquals( "", items.get( 1 ).getCol1() );
    assertEquals( 2, (int)items.get( 1 ).getCol2() );
    assertEquals( "", items.get( 1 ).getCol3() );

    assertEquals( "", items.get( 2 ).getCol1() );
    assertNull( items.get( 2 ).getCol2() );
    assertEquals( "", items.get( 2 ).getCol3() );
  }
}
