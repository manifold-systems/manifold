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

package manifold.api.properties;

import static org.assertj.core.api.Assertions.*;

import abc.MyProperties;
import gw.lang.SystemProperties;
import org.junit.jupiter.api.Test;

class PropertiesTest
{
  @Test
  void testProperties()
  {
    assertThat( MyProperties.MyProperty.toString() ).isEqualTo( "Hello" );
    assertThat( MyProperties.MyProperty.Sub ).isEqualTo( "Sub Property" );
    assertThat( SystemProperties.java.version ).isNotNull();
  }

  @Test
  void testFragment()
  {
    //[MyPropertiesRightHere.properties/] Foo=bar
    assertThat( MyPropertiesRightHere.Foo ).isEqualTo( "bar" );
  }
}
