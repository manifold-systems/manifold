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

package manifold.sql.schema.crud;

import manifold.sql.H2SakilaTest;

import manifold.sql.schema.simple.H2Sakila;
import manifold.sql.schema.simple.H2Sakila.*;
import org.junit.Test;

import java.sql.SQLException;

import static org.junit.Assert.*;

public class FkDependencyTest extends H2SakilaTest
{
  @Test
  public void testOneDependency() throws SQLException
  {
    Country country = Country.builder( "westamerica" ).build();
    City city = City.builder( country, "flippinton" ).build();

    H2Sakila.commit();

    // country's pk was written into country after the commit
    assertTrue( country.getCountryId() > 0 );
    // city's pk was written into city after the commit
    assertTrue( country.getCountryId() > 0 );
    // country's pk was written into city's fk after the commit
    assertEquals( country.getCountryId(), city.getCountryRef().getCountryId() );

    // check that the rows are in the db
    Country readCountry = Country.read( country.getCountryId() );
    assertNotNull( readCountry );
    assertEquals( country.getCountryId(), readCountry.getCountryId() );
    City readCity = City.read( city.getCityId() );
    assertNotNull( readCity );
    assertEquals( city.getCityId(), readCity.getCityId() );
    assertEquals( country.getCountryId(), readCity.getCountryRef().getCountryId() );
  }
}
