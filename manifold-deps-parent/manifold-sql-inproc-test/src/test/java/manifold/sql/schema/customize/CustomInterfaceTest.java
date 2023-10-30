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

package manifold.sql.schema.customize;

import manifold.ext.rt.api.auto;
import manifold.sql.schema.simple.h2.H2Sakila.*;
import manifold.sql.schema.h2.base.H2DdlServerTest;
import org.junit.Test;

import java.io.IOException;

import static org.junit.Assert.assertEquals;

public class CustomInterfaceTest extends H2DdlServerTest
{
  @Test
  public void testCustomInterface() throws IOException
  {
    loadData( "/samples/data/h2-sakila-data.sql" );

    auto query = "[.sql:H2Sakila/] Select * From store where store_id = :store_id";
    Store store = query.fetchOne( 1L );
    assertEquals( 1L, store.myCustomMethod() ); // from neighboring CustomStore
    assertEquals( "myCustomBaseMethod", store.myCustomBaseMethod() ); // from dbconfig "customBaseClass"
  }
}
