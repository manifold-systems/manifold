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

package manifold.science.util;

import java.math.BigDecimal;
import java.math.BigInteger;
import org.junit.Test;
import static org.junit.Assert.assertEquals;

import static manifold.science.util.CoercionConstants.*;

public class CoercionConstantsTest
{
  @Test
  public void testSimple()
  {
    assertEquals( Rational.get( "1.2" ), 1.2f r );
    assertEquals( Rational.get( "1.2" ), 1.2d r );
    assertEquals( Rational.get( "1.2" ), 1.2 r );

    assertEquals( new BigDecimal( "1.2" ), 1.2f bd );
    assertEquals( new BigDecimal( "1.2" ), 1.2d bd );
    assertEquals( new BigDecimal( "1.2" ), 1.2 bd );

    assertEquals( new BigInteger( "22" ), 22 bi );
  }

  @Test
  public void testPreservePrecisionBeyondDoubleLiteral()
  {
    // first, establish that double loses precision
    assertEquals( "3.0080111026763916", "" + 3.0080111026763916015 );
    // now test that rational coercion preserves precision even though coded as a double literal 
    assertEquals( "3.0080111026763916015" r, 3.0080111026763916015 r );
    assertEquals( "3.0080111026763916015" r, 3.0080111026763916015d r );
    assertEquals( "3.0080111026763916015", (3.0080111026763916015 r).toDecimalString() );

    // first, establish that double loses precision
    assertEquals( "3.0080111026763916", "" + 3.0080111026763916015 );
    // now test that rational coercion preserves precision even though coded as a double literal
    assertEquals( "3.0080111026763916015" bd, 3.0080111026763916015 bd );
    assertEquals( "3.0080111026763916015" bd, 3.0080111026763916015d bd );
    assertEquals( "3.0080111026763916015", (3.0080111026763916015 bd).toPlainString() );
  }

  @Test
  public void testPreservePrecisionBeyondFloatLiteral()
  {
    assertEquals( "3.0080111026763916015" r, 3.0080111026763916015f r );
    assertEquals( "3.0080111026763916015", (3.0080111026763916015f r).toDecimalString() );
  }
}
