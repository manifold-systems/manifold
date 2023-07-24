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

import manifold.ext.rt.api.auto;
import manifold.rt.api.util.StreamUtil;
import manifold.sql.rt.api.ConnectionProvider;
import manifold.sql.rt.api.TxScope;
import manifold.sql.rt.api.TxScopeProvider;
import manifold.sql.rt.api.OperableTxScope;
import manifold.sql.schema.simple.ScratchTest;
import org.junit.*;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.sql.SQLException;
import java.time.LocalDate;

import static manifold.rt.api.util.TempFileUtil.makeTempFile;
import static org.junit.Assert.*;

import manifold.sql.schema.simple.H2Sales;
import manifold.sql.schema.simple.H2Sales.*;

public class CrudTest
{
  private static final String DB_RESOURCE = "/manifold/sql/db/Sales.mv.db";

  @Before
  public void setup()
  {
    // copy database to temp dir, the url in DbConfig uses it from there
    File tempDbFile = makeTempFile( DB_RESOURCE );
    try( InputStream in = ScratchTest.class.getResourceAsStream( DB_RESOURCE );
         FileOutputStream out = new FileOutputStream( tempDbFile ) )
    {
      StreamUtil.copy( in, out );
    }
    catch( IOException e )
    {
      throw new RuntimeException( e );
    }
  }

  @After
  public void cleanup()
  {
    // close db connections
    ConnectionProvider.PROVIDERS.get().forEach( p -> p.closeAll() );
    ConnectionProvider.PROVIDERS.clear();

    // delete temp db
    //noinspection ResultOfMethodCallIgnored
    makeTempFile( DB_RESOURCE ).delete();
  }

  @Test
  public void testCreate() throws SQLException
  {
    TxScope txScope = TxScopeProvider.newScope( H2Sales.class );
    PurchaseOrder po = PurchaseOrder.create( txScope );
    po.setCustomerId( 2L );
    po.setOrderDate( LocalDate.now() );

    txScope.commit();
    assertTrue( po.getId() > 0 );
  }

  @Test
  public void testRead()
  {
    TxScope txScope = TxScopeProvider.newScope( H2Sales.class );
    PurchaseOrder po = PurchaseOrder.read( txScope, 1L );

    String expected = "1, 2, 2023-11-10";
    assertEquals( expected, po.display() );
  }

  @Test
  public void testUpdate() throws SQLException
  {
    TxScope txScope = TxScopeProvider.newScope( H2Sales.class );
    PurchaseOrder po = PurchaseOrder.read( txScope, 1L );
    assertEquals( 2L, (long)po.getCustomerId() );
    po.setCustomerId( 1L );
    LocalDate now = LocalDate.now();
    po.setOrderDate( now );

    txScope.commit();
    String expected = "1, 1, " + now;
    assertEquals( expected, po.display() );
  }

  @Test
  public void testDelete() throws SQLException
  {
    TxScope txScope = TxScopeProvider.newScope( H2Sales.class );
    Item po = Item.read( txScope, 10L );
    po.delete( true );
    txScope.commit();
    po = Item.read( txScope, 10L );
    assertNull( po );
  }

  @Test
  public void testUpdateFromQuery() throws SQLException
  {
    auto query = "[>.sql<] Select * From purchase_order Where id = :id";
    StringBuilder actual = new StringBuilder();
    LocalDate now = LocalDate.now();
    TxScope txScope = TxScopeProvider.newScope( H2Sales.class );
    for( PurchaseOrder po : query.run( txScope, 1L ) )
    {
      actual.append( po.getId() ).append( "," ).append( po.getCustomerId() ).append( "," ).append( po.getOrderDate() ).append( "\n" );
      Customer customer = po.fetchCustomerId();
      System.out.println( customer.display() );

      // make changes
      po.setOrderDate( now );
    }

    String expected = "1,2,2023-11-10\n";
    assertEquals( expected, actual.toString() );

    // commit changes
    txScope.commit();
    assertTrue( ((OperableTxScope)txScope).getRows().isEmpty() );

    // requery and test that changes were committed
    actual = new StringBuilder();
    for( PurchaseOrder po : query.run( txScope, 1L ) )
    {
      actual.append( po.getId() ).append( "," ).append( po.getCustomerId() ).append( "," ).append( po.getOrderDate() ).append( "\n" );
    }

    expected =
      "1,2," + now + "\n";
    assertEquals( expected, actual.toString() );
  }
}
