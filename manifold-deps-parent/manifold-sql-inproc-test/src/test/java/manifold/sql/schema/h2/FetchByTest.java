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
import java.util.List;
import java.util.stream.Collectors;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class FetchByTest extends H2DdlServerTest
{
  @Test
  public void fetchByTest() throws SQLException
  {
    Country myCountry = Country.create( "My Country" );
    Country otherCountry = Country.create( "Other Country" );
    City myCityMyCountry = City.create( "My City", myCountry );
    City otherCityMyCountry = City.create( "Other City", myCountry );
    City otherCityOtherCountry = City.create( "Other City", otherCountry );

    Address address = Address.builder("111 Main St.", "MyDistrict", myCityMyCountry, "555-5555" )
            .withPostalCode("11111")
            .build();

    H2Sakila.commit();

    List<Country> countries = Country.fetchByCountry( "My Country" );
    assertEquals( 1, countries.size() );
    assertEquals( myCountry, countries.get( 0 ) );

    List<City> cities = City.fetchByCity( "Other City" );
    assertEquals( 2, cities.size() );
    assertEquals( otherCityMyCountry, cities.get( 0 ) );
    assertEquals( otherCityOtherCountry, cities.get( 1 ) );

    // test non-required column, postal_code
    List<Address> addresses = Address.fetchByPostalCode( "11111" );
    assertEquals( 1, addresses.size() );
    assertEquals( "11111", addresses.get( 0 ).getPostalCode() );
  }
}
