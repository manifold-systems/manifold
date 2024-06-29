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
import manifold.sql.schema.simple.duckdb.DuckdbSakila.City;
import manifold.sql.schema.simple.duckdb.DuckdbSakila.Country;
import org.junit.Test;

import java.sql.SQLException;
import java.util.List;
import java.util.stream.Collectors;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class OneToManyTest extends DuckdbDdlServerTest
{
  @Test
  public void testOneToMany() throws SQLException
  {
    Country myCountry = Country.create( "My Country" );
    City myCity = City.create( "My City", myCountry );
    City otherCity = City.create( "Other City", myCountry );

    DuckdbSakila.commit();

    List<City> cities = myCountry.fetchCityRefs();
    assertEquals( 2, cities.size() );

    List<String> cityNames = cities.stream().map( City::getCity ).collect( Collectors.toList() );
    assertTrue( cityNames.contains( myCity.getCity() ) );
    assertTrue( cityNames.contains( otherCity.getCity() ) );
  }
}
