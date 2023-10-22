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
import manifold.sql.schema.h2.base.H2DdlServerTest;
import manifold.sql.schema.simple.h2.H2Sakila;
import manifold.sql.schema.simple.h2.H2Sakila.*;
import org.junit.Test;

import java.sql.SQLException;

import static org.junit.Assert.*;

public class DefaultScopeCrudTest extends H2DdlServerTest
{
  @Test
  public void testCreate() throws SQLException
  {
    Country hi = Country.create( "mycountry" );
    H2Sakila.commit();
    // test that country_id was assigned after the insert
    assertTrue( hi.getCountryId() > 0 );

    auto row = "[.sql:H2Sakila/] SELECT country_id FROM Country where country = 'mycountry'"
      .fetchOne();
    assertEquals( row.getCountryId(), (long)hi.getCountryId() );
  }

  @Test
  public void testRead() throws SQLException
  {
    Country hi = Country.create( "mycountry" );
    H2Sakila.commit();

    Country readHi = Country.fetch( hi.getCountryId() );
    assertEquals( readHi.getCountryId(), hi.getCountryId() );
  }

  @Test
  public void testUpdate() throws SQLException
  {
    Country hi = Country.create( "mycountry" );
    H2Sakila.commit();
    // test that country_id was assigned after the insert
    assertTrue( hi.getCountryId() > 0 );

    hi.setCountry( "mycountry2" );
    H2Sakila.commit();

    Country readHi = Country.fetch( hi.getCountryId() );
    assertEquals( "mycountry2", hi.getCountry() );
  }

  @Test
  public void testDelete() throws SQLException
  {
    Country hi = Country.create( "mycountry" );
    H2Sakila.commit();
    // test that country_id was assigned after the insert
    assertTrue( hi.getCountryId() > 0 );

    long countryId = hi.getCountryId();

    hi.delete( true );
    H2Sakila.commit();
    Country readHi = Country.fetch( countryId );
    assertNull( readHi );
  }
}
