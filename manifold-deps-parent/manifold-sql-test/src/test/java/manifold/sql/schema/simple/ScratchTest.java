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

import manifold.ext.rt.api.auto;
import manifold.sql.H2SalesTest;
import manifold.sql.queries.Foo;
import org.junit.*;

import manifold.sql.schema.simple.H2Sales.*;

import static org.junit.Assert.*;

public class ScratchTest extends H2SalesTest
{
  @Test
  public void testSimple()
  {
    StringBuilder sb = new StringBuilder();
    for( PurchaseOrder po : Foo.run() )
    {
      // just make sure the results can be navigated
      assertNotNull( po.getId() + " " + po.getCustomerRef().getId() + " " + po.getOrderDate() );
      sb.append( po.getId() + " " + po.getCustomerRef().getId() + " " + po.getOrderDate() );
    }
    assertTrue( sb.length() > 0 );
  }

  @Test
  public void testCommentQueryWithParameters()
  {
    /*[MyQuery.sql/]
      Select * From purchase_order Where customer_id = :c_id
    */
    StringBuilder actual = new StringBuilder();
    for( PurchaseOrder po : MyQuery.run( 2L ) )
    {
      actual.append( po.getId() ).append( "," ).append( po.getCustomerRef().getId() ).append( "," ).append( po.getOrderDate() ).append( "\n" );
    }
    String expected =
      "1,2,2023-11-10\n" +
      "3,2,2023-09-08\n";
    assertEquals( expected, actual.toString() );
  }

  @Test
  public void testStringQueryWithParameters()
  {
    auto query = "[.sql/] Select * From purchase_order Where customer_id = :c_id";
    String expected =
      "1,2,2023-11-10\n" +
      "3,2,2023-09-08\n";

    StringBuilder actual = new StringBuilder();
    actual = new StringBuilder();
    for( PurchaseOrder po : query.run( 2L ) )
    {
      actual.append( po.getId() ).append( "," ).append( po.getCustomerRef().getId() ).append( "," ).append( po.getOrderDate() ).append( "\n" );
    }
    assertEquals( expected, actual.toString() );
  }

  @Test
  public void testStringJoinQueryWithParameters()
  {
    auto query = "[.sql/] Select purchase_order.id, purchase_order.customer_id, purchase_order.order_date, c.name From purchase_order Join customer c on purchase_order.customer_id = c.id Where purchase_order.customer_id = :c_id";
    String expected =
      "1,2,2023-11-10,Cheryl Dunno\n" +
      "3,2,2023-09-08,Cheryl Dunno\n";

    StringBuilder actual = new StringBuilder();
    for( auto row : query.run( 2L ) )
    {
      auto flatRow = row.flatRow();
      actual.append( flatRow.getId() ).append( "," )
        .append( flatRow.getCustomerId() ).append( "," )
        .append( flatRow.getOrderDate() ).append( "," )
        .append( flatRow.getName() ).append( "\n" );
    }
    assertEquals( expected, actual.toString() );

    actual = new StringBuilder();
    for( auto row : query.run( 2L ) )
    {
      actual.append( row.getPurchaseOrder().getId() ).append( "," )
        .append( row.getPurchaseOrder().getCustomerRef().getId() ).append( "," )
        .append( row.getPurchaseOrder().getOrderDate() ).append( "," )
        .append( row.getName() ).append( "\n" );
    }
    assertEquals( expected, actual.toString() );
  }

  @Test
  public void testStringWithUnhandledExtResolvesToPlainString()
  {
    String y = "[.nope/] Select * From purchase_order Where customer_id = :c_id";
    assertEquals( "[.nope/] Select * From purchase_order Where customer_id = :c_id", y );
  }

  @Test
  public void testCommentWithUnhandledExtDoesNothing()
  {
    /*[Foo.nope/] Select * From purchase_order Where customer_id = :c_id */
  }
}
