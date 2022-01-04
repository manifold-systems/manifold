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

package manifold.ext.structural;

import junit.framework.TestCase;

import java.math.BigDecimal;

public class DefaultMethodTest extends TestCase
{
  public void testDefaultMethod()
  {
    // uses test BigDecimalExt and BigDecimal_To_SqlNumber IProxyFactory
    
    SqlNumber n1 = new BigDecimal("1");
    SqlNumber n2 = new BigDecimal("2");
    SqlNumber n3 = n1.plus(n2);
    assertEquals( new BigDecimal("3"), n3 );
    SqlNumber sum = n1 + n2;
    assertEquals( new BigDecimal("3"), sum );

    double result = n1.something( 4.5, 6 );
    assertEquals(11.5, result);

    SqlNumber f = (SqlNumber)new FooNumber("9");
    sum = f.plus(n2);
    assertEquals( new BigDecimal("11"), sum );
  }
}
