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

import abc.Person;

/**
 */
public class FragmentTest extends TestCase
{
  public void testFromSource()
  {
    assertEquals( 39, (int)Person.fromSource().getAge() );
  }

  public void testFragmentFromSource()
  {
    /*[MyObject.json/]
    {
      "name": "Scott",
      "location": {
        "planet": "fubar",
        "coordinates": 123456
      }
    }
    */
    MyObject sample = MyObject.fromSource();
    assertEquals( "Scott", sample.getName() );
    MyObject.location location = sample.getLocation();
    assertEquals( "fubar", location.getPlanet() );
    assertEquals( 123456, (int)location.getCoordinates() );
  }
}
