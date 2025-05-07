/*
 * Copyright (c) 2019 - Manifold Systems LLC
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

package manifold.api.host;

import junit.framework.TestCase;

import abc.TopLevelArray;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

public class TopLevelArrayTest extends TestCase
{
  public void testCreate()
  {
    TopLevelArray array = TopLevelArray.create();
    array.add( TopLevelArray.TopLevelArrayItem.create() );
    TopLevelArray.TopLevelArrayItem item = array.get( 0 );
    item.setFoo( "hi" );
    assertEquals( "hi", item.getFoo() );
  }

  public void testLoadJson()
  {
    TopLevelArray array = TopLevelArray.load().fromJson( makeJsonArray() );
    assertEquals( 2, array.size() );
    assertEquals( "hi", array.get(0).getFoo() );
    assertEquals( "bye", array.get(1).getFoo() );
  }

  public void testLoadYaml()
  {
    TopLevelArray array = TopLevelArray.load().fromYaml( makeYamlArray() );
    assertEquals( 2, array.size() );
    assertEquals( "hi", array.get(0).getFoo() );
    assertEquals( "bye", array.get(1).getFoo() );
  }

  public void testWriteJson()
  {
    String jsonArray = makeJsonArray();
    TopLevelArray array = TopLevelArray.load().fromJson( jsonArray );
    assertEquals( jsonArray, array.write().toJson() );
  }

  public void testWriteYaml()
  {
    TopLevelArray array = TopLevelArray.load().fromJson( makeJsonArray() );
    assertEquals( makeYamlArray(), array.write().toYaml() );
  }

  public void testSerializable() throws IOException, ClassNotFoundException
  {
    TopLevelArray array = TopLevelArray.load().fromJson( makeJsonArray() );
    ByteArrayOutputStream out = new ByteArrayOutputStream();
    try( ObjectOutputStream oos = new ObjectOutputStream( out ); )
    {
      oos.writeObject( array );
    }
    try( ObjectInputStream ois = new ObjectInputStream( new ByteArrayInputStream( out.toByteArray() ) ) )
    {
      array = (TopLevelArray)ois.readObject();
    }
    assertEquals( 2, array.size() );
    assertEquals( "hi", array.get(0).getFoo() );
    assertEquals( "bye", array.get(1).getFoo() );
  }

  private String makeJsonArray()
  {
    return
       "[\n" +
       "  {\n" +
       "    \"foo\": \"hi\"\n" +
       "  },\n" +
       "  {\n" +
       "    \"foo\": \"bye\"\n" +
       "  }\n" +
       "]";
  }

  private String makeYamlArray()
  {
    return
      "- foo: hi\n" +
      "- foo: bye\n";
  }
}
