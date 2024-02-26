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

import manifold.sql.schema.h2.base.H2DdlServerTest;
import manifold.sql.schema.simple.h2.H2Sakila;
import manifold.sql.schema.simple.h2.H2Sakila.*;
import org.junit.Test;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertEquals;

public class FetchAllTest extends H2DdlServerTest
{
  @Test
  public void fetchAllTest() throws SQLException
  {
    Country myCountry = Country.create( "My Country" );
    Country otherCountry = Country.create( "Other Country" );

    H2Sakila.commit();

    Iterable<Country> result = Country.fetchAll();
    List<Country> countries = new ArrayList<>();
    result.forEach(countries::add);
    assertEquals( 2, countries.size() );
    assertEquals( myCountry, countries.get( 0 ) );
    assertEquals( otherCountry, countries.get( 1 ) );
  }
}
