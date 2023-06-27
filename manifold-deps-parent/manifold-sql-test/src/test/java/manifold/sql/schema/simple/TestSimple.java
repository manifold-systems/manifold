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

package manifold.sql.schema.simple;

import junit.framework.TestCase;
import manifold.ext.rt.api.auto;
import manifold.sql.rt.api.ConnectionProvider;

public class TestSimple extends TestCase
{
  @Override
  protected void tearDown() throws Exception
  {
    super.tearDown();

    ConnectionProvider.PROVIDERS.get().forEach( p -> p.closeAll() );
    ConnectionProvider.PROVIDERS.clear();
  }

  public void testSimple()
  {
    //manifold.sql.schema.simple.H2Sales.PurchaseOrder po = null;
    Iterable<manifold.sql.queries.Foo.Result> result = manifold.sql.queries.Foo.run();
    StringBuilder sb = new StringBuilder();
    for( manifold.sql.queries.Foo.Result r : result )
    {
      // just make sure the results can be navigated
      assertNotNull( r.getId() + " " + r.getCustomerId() + " " + r.getOrderDate() );
      sb.append( r.getId() + " " + r.getCustomerId() + " " + r.getOrderDate() );
    }
    assertTrue( sb.length() > 0 );
  }

  public void testCommentQueryWithParameters()
  {
    /*[>MyQuery.sql<] Select * From purchase_order Where customer_id = :c_id */
    String expected =
      "1,2,2023-11-10\n" +
      "3,2,2023-09-08\n";

    StringBuilder actual = new StringBuilder();
    for( MyQuery.Result row : MyQuery.run( 2 ) )
    {
      actual.append( row.getId() ).append( "," ).append( row.getCustomerId() ).append( "," ).append( row.getOrderDate() ).append( "\n" );
    }
    assertEquals( expected, actual.toString() );
  }

  public void testStringQueryWithParameters()
  {
    auto query = "[>.sql<] Select * From purchase_order Where customer_id = :c_id";
    String expected =
      "1,2,2023-11-10\n" +
        "3,2,2023-09-08\n";

    StringBuilder actual = new StringBuilder();
    for( auto row : query.run( 2 ) )
    {
      actual.append( row.getId() ).append( "," ).append( row.getCustomerId() ).append( "," ).append( row.getOrderDate() ).append( "\n" );
    }
    assertEquals( expected, actual.toString() );
  }

}
