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

package manifold.sql.schema.postgres;

import manifold.ext.rt.api.auto;
import manifold.sql.DdlServerTest;
import manifold.sql.rt.api.DbConfig;
import manifold.sql.rt.api.Dependencies;
import manifold.sql.rt.api.TxScope;
import manifold.sql.schema.simple.postgres.PostgresSakila;
import manifold.sql.schema.simple.postgres.PostgresSakila.*;
import org.junit.Ignore;
import org.junit.Test;

import java.sql.SQLException;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public class CrudTest extends PostgresDdlServerTest
{
  @Test
  public void testCreate() throws SQLException
  {
    TxScope txScope = PostgresSakila.newScope();

    Country hi = Country.create( txScope, "mycountry" );
    txScope.commit();
    // test that country_id was assigned after the insert
    assertTrue( hi.getCountryId() > 0 );

    auto row = "[.sql:PostgresSakila/] SELECT country_id FROM Country where country = 'mycountry'"
      .fetchOne( txScope );
    assertEquals( row.getCountryId(), (long)hi.getCountryId() );
  }

  @Test
  public void testRead() throws SQLException
  {
    TxScope txScope = PostgresSakila.newScope();
    Country hi = Country.create( txScope, "mycountry" );
    txScope.commit();

    Country readHi = Country.fetch( txScope, hi.getCountryId() );
    assertEquals( readHi.getCountryId(), hi.getCountryId() );
  }

  @Test
  public void testUpdate() throws SQLException
  {
    TxScope txScope = PostgresSakila.newScope();
    Country hi = Country.create( txScope, "mycountry" );
    txScope.commit();
    // test that country_id was assigned after the insert
    assertTrue( hi.getCountryId() > 0 );

    hi.setCountry( "mycountry2" );
    txScope.commit();

    Country readHi = Country.fetch( txScope, hi.getCountryId() );
    assertEquals( "mycountry2", hi.getCountry() );
  }

  @Test
  public void testDelete() throws SQLException
  {
    TxScope txScope = PostgresSakila.newScope();
    Country hi = Country.create( txScope, "mycountry" );
    txScope.commit();
    // test that country_id was assigned after the insert
    assertTrue( hi.getCountryId() > 0 );

    int countryId = hi.getCountryId();

    hi.delete( true );
    txScope.commit();
    Country readHi = Country.fetch( txScope, countryId );
    assertNull( readHi );
  }
}
