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

import manifold.sql.H2SakilaTest;
import manifold.sql.schema.simple.h2.H2Sakila.*;
import org.junit.Test;

import static org.junit.Assert.*;

public class StringLiteralFragmentTest extends H2SakilaTest
{
  @Test
  public void testAnonymousObjectStringLiteralQuery()
  {
    auto query = "[.sql:H2Sakila/] Select * From country Where country_id = :country_id";
    Country country = query.fetchOne( 1L );
    assertEquals( 1, (long)country.getCountryId() );
    assertEquals( "Afghanistan", country.getCountry() );
    assertNotNull( country.getLastUpdate() );
  }

  @Test
  public void testNamedObjectStringLiteralQuery()
  {
    NamedObjectStringLiteralQuery query =
      "[NamedObjectStringLiteralQuery.sql:H2Sakila/] Select * From country Where country_id = :country_id";
    Country country = query.fetchOne( 1L );
    assertEquals( 1, (long)country.getCountryId() );
    assertEquals( "Afghanistan", country.getCountry() );
    assertNotNull( country.getLastUpdate() );
  }

  @Test
  public void testAnonymousRowStringLiteralQuery()
  {
    auto query = "[.sql:H2Sakila/] Select country From country Where country_id = :country_id";
    auto row = query.fetchOne( 1L );
    assertEquals( "Afghanistan", row.getCountry() );
  }

  @Test
  public void testNamedRowStringLiteralQuery()
  {
    NamedRowStringLiteralQuery query =
      "[NamedRowStringLiteralQuery.sql:H2Sakila/] Select country From country Where country_id = :country_id";
    NamedRowStringLiteralQuery.Row row = query.fetchOne( 1L );
    assertEquals( "Afghanistan", row.getCountry() );
  }

  @Test
  public void testStringLiteralQueryInForStmt()
  {
    int count = 0;
    for( Country country : "[.sql:H2Sakila/] Select * From country".fetch() )
    {
      assertTrue( country.getCountryId() > 0 );
      count++;
    }
    assertTrue( count > 0 );
    assertEquals( "[.sql:H2Sakila/] Select Count(country_id) From country".fetchOne().getCountCountryId(), count );
  }
}
