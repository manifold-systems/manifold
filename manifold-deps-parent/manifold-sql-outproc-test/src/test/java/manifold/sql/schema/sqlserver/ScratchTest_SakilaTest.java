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

package manifold.sql.schema.sqlserver;

import manifold.rt.api.util.StreamUtil;
import manifold.sql.rt.api.ConnectionProvider;
import manifold.sql.rt.api.Dependencies;
import manifold.sql.rt.util.SqlScriptRunner;
import manifold.sql.schema.simple.sqlserver.SqlserverSakila.*;
import org.junit.Test;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.sql.Connection;
import java.sql.SQLException;

public class ScratchTest_SakilaTest extends SqlserverDdlServerTest
{
  @Test
  public void testSomeInterestingQueries() throws IOException
  {
    loadData( "/samples/data/sqlserver-sakila-data.sql" );

    Stores s = "[Stores.sql:SqlserverSakila/] Select * From store";
    for( Store r : s.fetch() )
    {
      System.out.println( r.display() );
      System.out.println( r.fetchAddressRef().display() );
      System.out.println( r.fetchManagerStaffRef().display() );
    }

    /* [ActorWithMostFilms.sql:SqlserverSakila/]
      SELECT TOP (1) first_name, last_name, count(*) films
      FROM actor AS a
      JOIN film_actor AS fa ON (a.actor_id = fa.actor_id)
      GROUP BY a.actor_id, a.first_name, a.last_name
      ORDER BY films DESC;
    */
    for (ActorWithMostFilms.Row row : ActorWithMostFilms.fetch()) {
      System.out.println(row.display());
    }

    /* [CumulativeRevenueAllStores.sql:SqlserverSakila/]
      SELECT payment_date, amount, sum(amount) OVER (ORDER BY payment_date)
      FROM (
        SELECT CAST(payment_date AS DATE) AS payment_date, SUM(amount) AS amount
        FROM payment
        GROUP BY CAST(payment_date AS DATE)
      ) p
      ORDER BY payment_date;
    */
    for (CumulativeRevenueAllStores.Row row : CumulativeRevenueAllStores.fetch()) {
      System.out.println(row.getPaymentDate());
//      System.out.println(row.getSumAmount_Over_OrderByPaymentDate());
      System.out.println(row.display());
    }

//    auto one = "[.sql:SqlserverSakila/] select 1 from dual".fetchOne();
//    System.out.println(one.display());
  }
}
