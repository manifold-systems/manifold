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

import manifold.sql.schema.sqlserver.SqlserverDdlServerTest;
import manifold.sql.schema.simple.sqlserver.SqlserverSakila;
import manifold.sql.schema.simple.sqlserver.SqlserverSakila.*;
import org.junit.Test;

import java.sql.SQLException;

import static org.junit.Assert.*;

public class FkDependencyTest extends SqlserverDdlServerTest
{
  @Test
  public void testOneDependency() throws SQLException
  {
    Country country = Country.builder( "westamerica" ).build();
    City city = City.builder( "flippinton", country ).build();

    // pk is initially null
    assertNull( country.getCountryId() );
    assertNull( city.getCityId() );

    SqlserverSakila.commit();

    // country's pk was written into country after the commit
    assertTrue( country.getCountryId() > 0 );
    // city's pk was written into city after the commit
    assertTrue( city.getCityId() > 0 );
    // country's pk was written into city's fk after the commit
    assertEquals( country.getCountryId(), city.getCountryId() );

    // check that the rows are in the db
    Country readCountry = Country.fetch( country.getCountryId() );
    assertNotNull( readCountry );
    // also sanity check from direct sql
    Country countryFromSql = "[.sql:SqlserverSakila/] select * from country where country_id = :country_id".fetchOne( country.getCountryId() );
    assertEquals( country.getCountryId(), countryFromSql.getCountryId() );

    assertEquals( country.getCountryId(), readCountry.getCountryId() );
    City readCity = City.fetch( city.getCityId() );
    assertNotNull( readCity );
    assertEquals( city.getCityId(), readCity.getCityId() );
    assertEquals( country.getCountryId(), readCity.getCountryId() );
  }
}
