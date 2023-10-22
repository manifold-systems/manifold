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

import manifold.sql.schema.h2.base.H2DdlServerTest;
import org.junit.Test;

import manifold.sql.schema.simple.h2.H2Sakila;
import manifold.sql.schema.simple.h2.H2Sakila.*;

import java.sql.SQLException;

import static org.junit.Assert.*;

public class CommentFragmentTest extends H2DdlServerTest
{
  /**[ObjectJavadocCommentQuery.sql:H2Sakila/]
    Select * From country Where country_id = :country_id
   */
  @Test
  public void testObjectJavadocCommentQuery() throws SQLException
  {
    Country.create( "MyCountry" );
    H2Sakila.commit();
    
    Country country = ObjectJavadocCommentQuery.fetchOne( 1L );
    assertEquals( "MyCountry", country.getCountry() );
    assertEquals( 1, (long)country.getCountryId() );
    assertNotNull( country.getLastUpdate() );
  }

  /*[ObjectMultilineCommentQuery.sql:H2Sakila/]
    Select * From country Where country_id = :country_id
   */
  @Test
  public void testObjectMultilineCommentQuery() throws SQLException
  {
    Country.create( "MyCountry" );
    H2Sakila.commit();

    Country country = ObjectMultilineCommentQuery.fetchOne( 1L );
    assertEquals( "MyCountry", country.getCountry() );
    assertEquals( 1, (long)country.getCountryId() );
    assertNotNull( country.getLastUpdate() );
  }

  @Test
  public void testObjectLineCommentQuery() throws SQLException
  {
    Country.create( "MyCountry" );
    H2Sakila.commit();

    //[ObjectLineCommentQuery.sql:H2Sakila/] Select * From country Where country_id = :country_id
    Country country = ObjectLineCommentQuery.fetchOne( 1L );
    assertEquals( 1, (long)country.getCountryId() );
    assertEquals( "MyCountry", country.getCountry() );
    assertNotNull( country.getLastUpdate() );
  }

  /**[RowJavadocCommentQuery.sql:H2Sakila/]
    Select country From country Where country_id = :country_id
   */
  @Test
  public void testRowJavadocCommentQuery() throws SQLException
  {
    Country.create( "MyCountry" );
    H2Sakila.commit();

    RowJavadocCommentQuery.Row row = RowJavadocCommentQuery.fetchOne( 1L );
    assertEquals( "MyCountry", row.getCountry() );
  }

  /*[RowMultilineCommentQuery.sql:H2Sakila/]
    Select country From country Where country_id = :country_id
   */
  @Test
  public void testRowMultilineCommentQuery() throws SQLException
  {
    Country.create( "MyCountry" );
    H2Sakila.commit();

    RowMultilineCommentQuery.Row row = RowMultilineCommentQuery.fetchOne( 1L );
    assertEquals( "MyCountry", row.getCountry() );
  }

  @Test
  public void testRowLineCommentQuery() throws SQLException
  {
    Country.create( "MyCountry" );
    H2Sakila.commit();

    //[RowLineCommentQuery.sql:H2Sakila/] Select country From country Where country_id = :country_id
    RowLineCommentQuery.Row row = RowLineCommentQuery.fetchOne( 1L );
    assertEquals( "MyCountry", row.getCountry() );
  }
}
