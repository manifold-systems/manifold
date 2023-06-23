/*
 * Copyright (c) 2023 - Manifold Systems LLC
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

package manifold.sql.schema.simple;

import junit.framework.TestCase;
import manifold.ext.rt.api.auto;

import static java.lang.System.out;

public class TestSimple extends TestCase
{
  public void testSimple()
  {
    manifold.sql.schema.simple.SakilaSqlite.city city = null;
    Iterable<manifold.sql.queries.Foo.Result> result = manifold.sql.queries.Foo.run();
    for( manifold.sql.queries.Foo.Result r : result )
    {
      // just make sure the results can be navigated
      assertNotNull( r.getCity_id() + " " + r.getCity() + " " + r.getCountry_id() + " " + r.getLast_update() );
    }
  }

  public void testCommentQueryWithParameters()
  {
    /*[>MyQuery.sql<] Select * From city Where country_id = :country_id */
    String expected =
      "Akron 103\n" +
      "Arlington 103\n" +
      "Augusta-Richmond County 103\n" +
      "Aurora 103\n" +
      "Bellevue 103\n" +
      "Brockton 103\n" +
      "Cape Coral 103\n" +
      "Citrus Heights 103\n" +
      "Clarksville 103\n" +
      "Compton 103\n" +
      "Dallas 103\n" +
      "Dayton 103\n" +
      "El Monte 103\n" +
      "Fontana 103\n" +
      "Garden Grove 103\n" +
      "Garland 103\n" +
      "Grand Prairie 103\n" +
      "Greensboro 103\n" +
      "Joliet 103\n" +
      "Kansas City 103\n" +
      "Lancaster 103\n" +
      "Laredo 103\n" +
      "Lincoln 103\n" +
      "Manchester 103\n" +
      "Memphis 103\n" +
      "Peoria 103\n" +
      "Roanoke 103\n" +
      "Rockford 103\n" +
      "Saint Louis 103\n" +
      "Salinas 103\n" +
      "San Bernardino 103\n" +
      "Sterling Heights 103\n" +
      "Sunnyvale 103\n" +
      "Tallahassee 103\n" +
      "Warren 103\n";

    StringBuilder actual = new StringBuilder();
    for( MyQuery.Result row : MyQuery.run( "103" ) )
    {
      actual.append( row.getCity() ).append( " " ).append( row.getCountry_id() ).append( "\n" );
    }
    assertEquals( expected, actual.toString() );
  }

  public void testStringQueryWithParameters()
  {
    auto query = "[>.sql<] Select * From city Where country_id = :country_id or country_id = :country_id2";
    StringBuilder actual = new StringBuilder();
    for( auto row : query.run("103", "90") )
    {
      actual.append( row.getCity() ).append( " " ).append( row.getCountry_id() ).append( "\n" );
    }

    String expected =
      "Malm 90\n" +
      "Akron 103\n" +
      "Arlington 103\n" +
      "Augusta-Richmond County 103\n" +
      "Aurora 103\n" +
      "Bellevue 103\n" +
      "Brockton 103\n" +
      "Cape Coral 103\n" +
      "Citrus Heights 103\n" +
      "Clarksville 103\n" +
      "Compton 103\n" +
      "Dallas 103\n" +
      "Dayton 103\n" +
      "El Monte 103\n" +
      "Fontana 103\n" +
      "Garden Grove 103\n" +
      "Garland 103\n" +
      "Grand Prairie 103\n" +
      "Greensboro 103\n" +
      "Joliet 103\n" +
      "Kansas City 103\n" +
      "Lancaster 103\n" +
      "Laredo 103\n" +
      "Lincoln 103\n" +
      "Manchester 103\n" +
      "Memphis 103\n" +
      "Peoria 103\n" +
      "Roanoke 103\n" +
      "Rockford 103\n" +
      "Saint Louis 103\n" +
      "Salinas 103\n" +
      "San Bernardino 103\n" +
      "Sterling Heights 103\n" +
      "Sunnyvale 103\n" +
      "Tallahassee 103\n" +
      "Warren 103\n";
    assertEquals( expected, actual.toString() );
  }

}
