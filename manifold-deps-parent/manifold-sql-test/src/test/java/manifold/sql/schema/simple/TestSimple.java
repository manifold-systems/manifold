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
import manifold.rt.api.util.StreamUtil;
import manifold.sql.rt.api.ConnectionProvider;
import manifold.sql.queries.Foo;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

import static manifold.rt.api.util.TempFileUtil.makeTempFile;
import static org.junit.Assert.*;

public class TestSimple
{
  private static final String DB_RESOURCE = "/manifold/sql/db/Sales.mv.db";

  @BeforeClass
  public static void setup()
  {
    // copy database to temp dir, the url in DbConfig uses it from there
    File tempDbFile = makeTempFile( DB_RESOURCE );
    try( InputStream in = TestSimple.class.getResourceAsStream( DB_RESOURCE );
         FileOutputStream out = new FileOutputStream( tempDbFile ) )
    {
      StreamUtil.copy( in, out );
    }
    catch( IOException e )
    {
      throw new RuntimeException( e );
    }
  }

  @AfterClass
  public static void cleanup()
  {
    // close db connections
    ConnectionProvider.PROVIDERS.get().forEach( p -> p.closeAll() );
    ConnectionProvider.PROVIDERS.clear();

    // delete temp db
    //noinspection ResultOfMethodCallIgnored
    makeTempFile( DB_RESOURCE ).delete();
  }

  @Test
  public void testSimple()
  {
    StringBuilder sb = new StringBuilder();
    for( Foo.Row r : Foo.run() )
    {
      // just make sure the results can be navigated
      assertNotNull( r.getId() + " " + r.getCustomerId() + " " + r.getOrderDate() );
      sb.append( r.getId() + " " + r.getCustomerId() + " " + r.getOrderDate() );
    }
    assertTrue( sb.length() > 0 );
  }

  @Test
  public void testCommentQueryWithParameters()
  {
    /*[>MyQuery.sql<] Select * From purchase_order Where customer_id = :c_id */
    StringBuilder actual = new StringBuilder();
    for( MyQuery.Row row : MyQuery.run( 2 ) )
    {
      actual.append( row.getId() ).append( "," ).append( row.getCustomerId() ).append( "," ).append( row.getOrderDate() ).append( "\n" );
    }
    String expected =
      "1,2,2023-11-10\n" +
      "3,2,2023-09-08\n";
    assertEquals( expected, actual.toString() );
  }

  @Test
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

  @Test
  public void testStringWithUnhandledExtResolvesToPlainString()
  {
    String y = "[>.nope<] Select * From purchase_order Where customer_id = :c_id";
    assertEquals( "[>.nope<] Select * From purchase_order Where customer_id = :c_id", y );
  }

  @Test
  public void testCommentWithUnhandledExtDoesNothing()
  {
    /*[>Foo.nope<] Select * From purchase_order Where customer_id = :c_id */
  }
}
