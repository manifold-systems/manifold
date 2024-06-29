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

package manifold.sql.schema.h2;

import manifold.sql.rt.api.TxScope;
import manifold.sql.schema.h2.base.H2DdlServerTest;
import manifold.sql.schema.simple.h2.H2Sakila;
import manifold.sql.schema.simple.h2.H2Sakila.*;
import org.junit.Test;

import java.sql.SQLException;

import static java.lang.System.out;
import static org.junit.Assert.*;

public class BatchExecutionTest extends H2DdlServerTest
{
  @Test
  public void testMixingBatchedStatementsWithEntityCrud() throws SQLException
  {
    H2Sakila.addBatchChange( c -> {
      H2Sakila.Country.create( "Hi" );
      "[.sql:H2Sakila/] insert into country (country) values ('mycountry1')".execute( c );
      H2Sakila.Country.create( "A" );
      "[.sql:H2Sakila/] insert into country (country) values ('mycountry2')".execute( c );
      H2Sakila.Country.create( "B" );
      "[.sql:H2Sakila/] insert into country (country) values ('mycountry3')".execute( c );
      H2Sakila.Country.create( "Bye" );
      for( int i = 0; i < 3; i++ )
      {
        "[.sql:H2Sakila/] insert into country (country) values (:value)".execute( c, "hey" + i );
      }
    } );
    H2Sakila.commit();

    StringBuilder result = new StringBuilder();
    "[.sql:H2Sakila/] select * from country".fetch()
      .forEach( e -> result.append( e.getCountry() ).append( '\n' ) );

    // note, entity crud is not batched
    String actual =
      "Hi\n" +
      "A\n" +
      "B\n" +
      "Bye\n" +
      "mycountry1\n" +
      "mycountry2\n" +
      "mycountry3\n" +
      "hey0\n" +
      "hey1\n" +
      "hey2\n";
    assertEquals( actual, result.toString() );
  }
}
