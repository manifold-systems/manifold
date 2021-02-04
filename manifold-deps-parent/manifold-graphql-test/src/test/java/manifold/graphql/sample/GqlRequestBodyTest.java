/*
 * Copyright (c) 2021 - Manifold Systems LLC
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

package manifold.graphql.sample;

import junit.framework.TestCase;
import manifold.graphql.rt.api.Config;
import manifold.graphql.rt.api.request.GqlRequestBody;
import manifold.json.rt.api.DataBindings;
import manifold.rt.api.Bindings;

import java.util.Arrays;
import java.util.List;

public class GqlRequestBodyTest extends TestCase
{
  @Override
  protected void setUp() throws Exception
  {
    super.setUp();
    Config.instance().setRemoveNullConstraintValues( true );
  }

  @Override
  protected void tearDown() throws Exception
  {
    super.tearDown();
    Config.instance().setRemoveNullConstraintValues( false );
  }

  public void testIsRemoveNullConstraintValues()
  {
    Bindings variables = new DataBindings();
    variables.put( "name", "scott" );
    variables.put( "age", null );
    Bindings address = new DataBindings();
    address.put( "street", "main" );
    address.put( "city", "cupertino" );
    address.put( "state", null );
    Bindings address2 = new DataBindings();
    address2.put( "street", "foo" );
    address2.put( "city", "bar" );
    address2.put( "state", null );
    address.put( "list", Arrays.asList( null, address2, variables/*test cycle short-circuit*/ ) );
    variables.put( "address", address );

    GqlRequestBody<Bindings> req = GqlRequestBody.create( "hi", variables );
    Bindings reqVariables = req.getVariables();
    assertEquals( 2, reqVariables.size() );
    assertEquals( "scott", reqVariables.get( "name" ) );
    Bindings reqAddress = (Bindings)reqVariables.get( "address" );
    assertEquals( 3, reqAddress.size() );
    assertEquals( "main", reqAddress.get( "street" ) );
    assertEquals( "cupertino", reqAddress.get( "city" ) );
    List reqList = (List)reqAddress.get( "list" );
    assertEquals( 3, reqList.size() );
    assertNull( reqList.get( 0 ) );
    Bindings listAddress = (Bindings)reqList.get( 1 );
    assertEquals( 2, listAddress.size() );
    assertEquals( "foo", listAddress.get( "street" ) );
    assertEquals( "bar", listAddress.get( "city" ) );
  }
}