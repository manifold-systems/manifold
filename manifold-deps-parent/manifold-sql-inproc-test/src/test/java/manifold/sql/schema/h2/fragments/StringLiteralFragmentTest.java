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

package manifold.sql.schema.h2.fragments;

import manifold.ext.rt.api.auto;
import manifold.sql.schema.h2.base.H2DdlServerTest;
import manifold.sql.schema.simple.h2.H2Sakila;
import manifold.sql.schema.simple.h2.H2Sakila.*;
import org.junit.Test;

import java.sql.SQLException;

import static org.junit.Assert.*;

public class StringLiteralFragmentTest extends H2DdlServerTest
{
  @Test
  public void testAnonymousObjectStringLiteralQuery() throws SQLException
  {
    Country.create( "MyCountry" );
    H2Sakila.commit();

    auto query = "[.sql:H2Sakila/] Select * From country Where country = :country";
    Country country = query.fetchOne( "MyCountry" );
    assertEquals( "MyCountry", country.getCountry() );
    assertEquals( 1, (long)country.getCountryId() );
    assertNotNull( country.getLastUpdate() );
  }

  @Test
  public void testNamedObjectStringLiteralQuery() throws SQLException
  {
    Country.create( "MyCountry" );
    H2Sakila.commit();

    NamedObjectStringLiteralQuery query =
      "[NamedObjectStringLiteralQuery.sql:H2Sakila/] Select * From country Where country = :country";
    Country country = query.fetchOne( "MyCountry" );
    assertEquals( "MyCountry", country.getCountry() );
    assertEquals( 1, (long)country.getCountryId() );
    assertNotNull( country.getLastUpdate() );
  }

  @Test
  public void testAnonymousRowStringLiteralQuery() throws SQLException
  {
    Country.create( "MyCountry" );
    H2Sakila.commit();

    auto query = "[.sql:H2Sakila/] Select country From country Where country = :country";
    auto row = query.fetchOne( "MyCountry" );
    assertEquals( "MyCountry", row.getCountry() );
  }

  @Test
  public void testNamedRowStringLiteralQuery() throws SQLException
  {
    Country.create( "MyCountry" );
    H2Sakila.commit();

    NamedRowStringLiteralQuery query =
      "[NamedRowStringLiteralQuery.sql:H2Sakila/] Select country From country Where country = :country";
    NamedRowStringLiteralQuery.Row row = query.fetchOne( "MyCountry" );
    assertEquals( "MyCountry", row.getCountry() );
  }

  @Test
  public void testStringLiteralQueryInForStmt() throws SQLException
  {
    Country.create( "MyCountry" );
    Country.create( "YourCountry" );
    H2Sakila.commit();

    int count = 0;
    for( Country country : "[.sql:H2Sakila/] Select * From country".fetch() )
    {
      assertTrue( country.getCountryId() > 0 );
      count++;
    }
    assertEquals( 2, count );
    assertEquals( "[.sql:H2Sakila/] Select Count(country_id) From country".fetchOne().getCountCountryId(), count );
  }
}
