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

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import javax.script.Bindings;
import junit.framework.TestCase;
import manifold.ext.DataBindings;

public class ManBindingsExtTest extends TestCase
{
  public void testDeepCopy()
  {
    DataBindings bindings = new DataBindings();
    bindings.put( "name", "Scott" );
    bindings.put( "age", 32 );
    bindings.put( "bool", true );
    bindings.put( "list", Arrays.asList( "a", "b", "c" ) );
    bindings.put( "list2", Arrays.asList( bindings.deepCopy(), bindings.deepCopy() ) );
    bindings.put( "bindings", bindings.deepCopy() );

    assertIdenticalDeepCopies( bindings.deepCopy(), bindings );
  }

  private void assertIdenticalDeepCopies( Object p1, Object p2 )
  {
    if( p1 instanceof List )
    {
      assertTrue( p2 instanceof List );
      assertNotSame( p1, p2 );
      assertEquals( ((List)p1).size(), ((List)p2).size() );
      for( int i = 0; i < ((List)p1).size(); i++ )
      {
        assertIdenticalDeepCopies( ((List)p1).get( i ), ((List)p2).get( i ) );
      }
    }
    else if( p1 instanceof Bindings )
    {
      assertTrue( p2 instanceof Bindings );
      assertNotSame( p1, p2 );
      assertEquals( ((Bindings)p1).size(), ((Bindings)p2).size() );
      for( Map.Entry<String, Object> entry: ((Bindings)p1).entrySet() )
      {
        assertIdenticalDeepCopies( entry.getValue(), ((Bindings)p2).get( entry.getKey() ) );
      }
    }
    else
    {
      assertEquals( p1, p2 );
    }
  }
}
