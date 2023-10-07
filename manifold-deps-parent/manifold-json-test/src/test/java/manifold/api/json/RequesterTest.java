/*
 * Copyright (c) 2020 - Manifold Systems LLC
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

package manifold.api.json;

import manifold.json.rt.api.Requester;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import abc.Dummy;
import spark.Spark;

import static org.junit.Assert.assertTrue;

public class RequesterTest
{
  @BeforeClass
  public static void init()
  {
    TestServer.main(new String[0]);
    Spark.awaitInitialization();
  }

  @AfterClass
  public static void destroy() {
    TestServer.stop();
  }

  @Test
  public void httpPostRequestWithParams()
  {
    Requester<Dummy> req = Dummy.request( "http://localhost:4567/" )
      .withParam( "foo", "bar" )
      .withParam( "abc", "8" );
    Object queryString = req.postOne( "testPost_QueryString", Dummy.create(), Requester.Format.Text );
    assertTrue("foo=bar&abc=8".equals(queryString) || "abc=8&foo=bar".equals(queryString));
  }

  @Test
  public void httpGetRequestWithParams()
  {
    Requester<Dummy> req = Dummy.request( "http://localhost:4567/" )
      .withParam( "foo", "bar" )
      .withParam( "abc", "8" );
    Object queryString = req.getOne( "testGet_QueryString", Dummy.create(), Requester.Format.Text );
    assertTrue("foo=bar&abc=8".equals(queryString) || "abc=8&foo=bar".equals(queryString));
  }

  @Test
  public void httpPostRequestWithParameterizedUrlSuffixWithParams()
  {
    Requester<Dummy> req = Dummy.request( "http://localhost:4567/" )
      .withParam( "foo", "bar" )
      .withParam( "abc", "8" );
    Object queryString = req.postOne( "testPost_QueryString?firstParam=firstValue", Dummy.create(), Requester.Format.Text );
    assertTrue( "firstParam=firstValue&foo=bar&abc=8".equals(queryString) || "firstParam=firstValue&abc=8&foo=bar".equals(queryString));
  }

  @Test
  public void httpGetRequestWithParameterizedUrlSuffixWithParams()
  {
    Requester<Dummy> req = Dummy.request( "http://localhost:4567/" )
      .withParam( "foo", "bar" )
      .withParam( "abc", "8" );
    Object queryString = req.getOne( "testGet_QueryString?firstParam=firstValue", Dummy.create(), Requester.Format.Text );
    assertTrue( "firstParam=firstValue&foo=bar&abc=8".equals(queryString) || "firstParam=firstValue&abc=8&foo=bar".equals(queryString));
  }
}
