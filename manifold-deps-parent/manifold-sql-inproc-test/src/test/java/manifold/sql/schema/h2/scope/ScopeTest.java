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

package manifold.sql.schema.h2.scope;

import manifold.ext.rt.api.auto;
import manifold.sql.rt.api.OperableTxScope;
import manifold.sql.rt.api.TxScope;
import manifold.sql.schema.h2.base.H2DdlServerTest;
import org.junit.Test;

import static org.junit.Assert.*;

import manifold.sql.schema.simple.h2.H2Sakila;
import manifold.sql.schema.simple.h2.H2Sakila.*;

import java.sql.SQLException;

public class ScopeTest extends H2DdlServerTest
{
  @Test
  public void testChangesMatchingPersistedStateAvoidsUpdate() throws SQLException
  {
    TxScope txScope = H2Sakila.newScope();

    Language myLanguage = Language.create(txScope, "My Language");
    Film myFilm = Film.create(txScope, "My Film", myLanguage );
    myFilm.setDescription( "hi" );
    txScope.commit();
    myFilm.setDescription( "hi" );
    assertTrue( ((OperableTxScope)txScope).getRows().isEmpty() );
    myFilm.setDescription( "hiya" );
    assertFalse( ((OperableTxScope)txScope).getRows().isEmpty() );
  }
}
