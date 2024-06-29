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

import manifold.sql.schema.simple.duckdb.DuckdbSakila.*;
import manifold.sql.schema.duckdb.base.DuckdbDdlServerTest;
import org.duckdb.DuckDBStruct;
import org.junit.Test;

import java.io.IOException;
import java.sql.SQLException;
import java.util.Map;

import static java.lang.System.out;
import static org.junit.Assert.assertEquals;

public class ScratchTest_SakilaTest extends DuckdbDdlServerTest
{
  /* [TrainServices.sql:DuckdbSakila/]
    SELECT station_name, count(*) AS num_services
    FROM 'http://blobs.duckdb.org/train_services.parquet'
    WHERE monthname(date) = 'May'
    GROUP BY ALL
    ORDER BY num_services DESC
    LIMIT 3
  */
  @Test
  public void testTableFromUrl()
  {
    String expected = "Utrecht Centraal: 7663\n" +
      "Amsterdam Centraal: 7591\n" +
      "Zwolle: 5013\n";
    StringBuilder result = new StringBuilder();
    TrainServices.fetch().forEach( row ->
      result.append( row.getStationName() ).append( ": " )
        .append( row.getNumServices() ).append( '\n' ) );
    assertEquals(expected, result.toString());
  }

  @Test
  public void scratchTest() throws SQLException
  {
    /* [WithTest.sql:DuckdbSakila/]
      WITH constraint_columns AS (select * from duckdb_constraints where constraint_type = 'FOREIGN KEY')
      select * from constraint_columns
    */
    WithTest.fetch().iterator().next().getConstraintText();
  }

//todo: sakila data import
//
//  @Test
//  public void testSomeInterestingQueries() throws IOException
//  {
//    loadData( "/samples/data/duckdb-sakila-data.sql" );
//
//    Stores s = "[Stores.sql:DuckdbSakila/] Select * From store";
//    for( Store r : s.fetch() )
//    {
//      System.out.println( r.display() );
//      System.out.println( r.fetchAddressRef().display() );
//      System.out.println( r.fetchManagerStaffRef().display() );
//    }
//
//    /* [ActorWithMostFilms.sql:DuckdbSakila/]
//      SELECT first_name, last_name, count(*) films
//      FROM actor AS a
//      JOIN film_actor AS fa USING (actor_id)
//      GROUP BY a.actor_id, first_name, last_name
//      ORDER BY films DESC
//      LIMIT 1;
//    */
//    for (ActorWithMostFilms.Row row : ActorWithMostFilms.fetch()) {
//      System.out.println(row.display());
//    }
//
//    /* [CumulativeRevenueAllStores.sql:DuckdbSakila/]
//      SELECT payment_date, amount, sum(amount) OVER (ORDER BY payment_date)
//      FROM (
//        SELECT CAST(payment_date AS DATE) AS payment_date, SUM(amount) AS amount
//        FROM payment
//        GROUP BY CAST(payment_date AS DATE)
//      ) p
//      ORDER BY payment_date;
//    */
//    for (CumulativeRevenueAllStores.Row row : CumulativeRevenueAllStores.fetch()) {
//      System.out.println(row.getPaymentDate());
//      System.out.println(row.getSumAmount_Over_OrderByPaymentDate());
//      System.out.println(row.display());
//    }
//
//    for( Staff staff: "[.sql:DuckdbSakila/] select * from staff".fetch() )
//    {
//      System.out.println( staff.display() );
//    }
//  }
}
