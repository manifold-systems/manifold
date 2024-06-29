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
import manifold.sql.schema.simple.duckdb.DuckdbSakila.Category;
import manifold.sql.schema.simple.duckdb.DuckdbSakila.Film;
import manifold.sql.schema.simple.duckdb.DuckdbSakila.FilmCategory;
import manifold.sql.schema.simple.duckdb.DuckdbSakila.Language;
import org.junit.Test;

import java.sql.SQLException;
import java.util.List;

import static org.junit.Assert.assertEquals;

public class ManyToManyTest extends DuckdbDdlServerTest
{
  @Test
  public void testManyToMany() throws SQLException
  {
    Language myLanguage = Language.create("My Language");
    Film myFilm = Film.create("My Film", myLanguage );
    Film myOtherFilm = Film.create("My Other Film", myLanguage );
    Category myCat = Category.create( "My Category" );
    Category myOtherCat = Category.create( "My Other Category" );
    FilmCategory myFilmCategory = FilmCategory.create( myFilm, myCat );
    FilmCategory myFilmOtherCategory = FilmCategory.create( myFilm, myOtherCat );
    FilmCategory myOtherFilmCategory = FilmCategory.create( myOtherFilm, myCat );

    DuckdbSakila.commit();

    List<Category> categories = myFilm.fetchCategoryRefs();
    assertEquals( 2, categories.size() );
    Category c = categories.get( 0 );
    assertEquals( "My Category", c.getName() );
    c = categories.get( 1 );
    assertEquals( "My Other Category", c.getName() );

    categories = myOtherFilm.fetchCategoryRefs();
    assertEquals( 1, categories.size() );
    c = categories.get( 0 );
    assertEquals( "My Category", c.getName() );

    List<Film> films = myCat.fetchFilmRefs();
    assertEquals( 2, films.size() );
    Film f = films.get( 0 );
    assertEquals( "My Film", f.getTitle() );
    f = films.get( 1 );
    assertEquals( "My Other Film", f.getTitle() );
  }
}
