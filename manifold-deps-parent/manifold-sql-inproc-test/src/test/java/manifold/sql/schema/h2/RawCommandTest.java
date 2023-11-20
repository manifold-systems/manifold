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

import manifold.ext.rt.api.auto;
import manifold.sql.rt.api.TxScope;
import manifold.sql.schema.h2.base.H2DdlServerTest;
import manifold.sql.schema.simple.h2.H2Sakila;
import manifold.sql.schema.simple.h2.H2Sakila.*;
import org.junit.Test;

import java.sql.SQLException;

import static org.junit.Assert.*;

public class RawCommandTest extends H2DdlServerTest
{
  @Test
  public void testRawCommand() throws SQLException
  {
    Country country = Country.create( "mycountry" );
    H2Sakila.commit();

    // delete
    H2Sakila.addSqlChange( ctx -> {
      "[.sql:H2Sakila/] DELETE FROM country WHERE country = 'mycountry'".execute( ctx );
    } );

    H2Sakila.commit();

    Country fetchCountry = Country.fetch( country.getCountryId() );
    assertNull( fetchCountry );
  }

  @Test
  public void testRawCommandParams() throws SQLException
  {
    Country country = Country.create( "mycountry" );
    H2Sakila.commit();

    // delete
    H2Sakila.addSqlChange( ctx -> {
      "[.sql:H2Sakila/] DELETE FROM country WHERE country = :country".execute( ctx, "mycountry" );
    } );

    H2Sakila.commit();

    Country fetchCountry = Country.fetch( country.getCountryId() );
    assertNull( fetchCountry );
  }

  @Test
  public void testMixSqlChangesAndEntityChanges() throws SQLException
  {
    Country country = Country.create( "mycountry" );
    Country country2 = Country.create( "mycountry2" );
    H2Sakila.commit();

    // next commit combines an entity change with city and an update raw command
    City city = City.create( "myCity", country );
    H2Sakila.addSqlChange( ctx -> {
      // [MyUpdate.sql:H2Sakila/] UPDATE country SET country = :country || country WHERE country LIKE :prefix || '%'
      MyUpdate.execute( ctx, "yourcountry", "mycountry" );
    } );

    H2Sakila.commit();

    assertNotNull( city.getCityId() );
    assertNotNull( City.fetch( city.getCityId() ) );
    Country fetchCountry = Country.fetch( country.getCountryId() );
    country = Country.fetch( country.getCountryId() );
    assertEquals( "yourcountrymycountry", country.getCountry() );
    country2 = Country.fetch( country2.getCountryId() );
    assertEquals( "yourcountrymycountry2", country2.getCountry() );
  }

  @Test
  public void testRawCommandExplicitScope() throws SQLException
  {
    TxScope txScope = H2Sakila.newScope();

    Country country = Country.create( txScope, "mycountry" );
    txScope.commit();

    // delete
    txScope.addSqlChange( ctx -> {
      "[.sql:H2Sakila/] DELETE FROM country WHERE country = 'mycountry'".execute( ctx );
    } );

    txScope.commit();

    Country fetchCountry = Country.fetch( txScope, country.getCountryId() );
    assertNull( fetchCountry );
  }
}
