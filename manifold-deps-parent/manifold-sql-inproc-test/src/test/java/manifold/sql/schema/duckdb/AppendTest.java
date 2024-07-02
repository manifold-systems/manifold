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

package manifold.sql.schema.duckdb;

import manifold.sql.schema.duckdb.base.DuckdbDdlServerTest;
import manifold.sql.schema.simple.duckdb.DuckdbSakila;
import manifold.sql.schema.simple.duckdb.DuckdbSakila.Country;
import org.junit.Test;

import java.sql.SQLException;
import java.time.LocalDateTime;

import static org.junit.Assert.*;

public class AppendTest extends DuckdbDdlServerTest
{
  @Test
  public void testAppendNoTransaction() throws SQLException
  {
    LocalDateTime now = LocalDateTime.now();
    Country.append( a -> {
      a.append( 1, "Canada", now );
      a.append( 2, "Mexico", now );
      a.append( 3, "Belize", now );
      a.append( 4, "Brazil", now );
    } );
    StringBuilder sb = new StringBuilder();
    for( Country country : Country.fetchAll() )
    {
      sb.append( country.getCountryId() ).append( ',' )
        .append( country.getCountry() ).append( ',' )
        .append( country.getLastUpdate() ).append( '\n' );
    }
    assertEquals(
      "1,Canada," + now + "\n" +
      "2,Mexico," + now + "\n" +
      "3,Belize," + now + "\n" +
      "4,Brazil," + now + "\n",
      sb.toString() );
  }

  @Test
  public void testAppendWithinTransaction() throws SQLException
  {
    LocalDateTime now = LocalDateTime.now();
    DuckdbSakila.commit( ctx -> {
      Country.builder( "first" ).withLastUpdate( now ).build();
      "[.sql:DuckdbSakila/] insert into country (country, last_update) values('second', :now)".execute( ctx, now );
      Country.append( a -> {
        a.append( 10, "Canada", now );
        a.append( 20, "Mexico", now );
        a.append( 30, "Belize", now );
        a.append( 40, "Brazil", now );
      } );
    } );
    StringBuilder sb = new StringBuilder();
    for( Country country : Country.fetchAll() )
    {
      sb.append( country.getCountryId() ).append( ',' )
        .append( country.getCountry() ).append( ',' )
        .append( country.getLastUpdate() ).append( '\n' );
    }
    assertEquals(
      "1,first," + now + "\n" +
      "2,second," + now + "\n" +
      "10,Canada," + now + "\n" +
      "20,Mexico," + now + "\n" +
      "30,Belize," + now + "\n" +
      "40,Brazil," + now + "\n",
      sb.toString() );
  }
}
