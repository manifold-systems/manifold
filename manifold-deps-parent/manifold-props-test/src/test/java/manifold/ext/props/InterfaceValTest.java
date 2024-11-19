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

package manifold.ext.props;

import junit.framework.TestCase;
import manifold.ext.props.rt.api.val;

public class InterfaceValTest extends TestCase
{
  public void testDefaults()
  {
    Person p = new Person(){};
    assertEquals( "Scott", p.name);
    assertEquals( 0, p.age);
    assertNotNull( p.gender );
    assertNull( p.address );
    assertNull( p.phone);
  }

  interface Person
  {
    class Gender {}

    @val String name = "Scott";
    @val int age = 0;
    @val Gender gender = new Gender();
    @val String address = null;
    @val String phone = null;
  }

}
