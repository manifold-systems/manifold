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

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.math.BigInteger;
import org.junit.Test;


import static manifold.science.util.CoercionConstants.r;
import static org.junit.Assert.*;

public class RationalTest
{
  @Test
  public void testSimple()
  {
    Rational x = 4.2r;
    assertEquals( "0 / 1", (0r).toString() );
    assertEquals( "1 / 1", (1r).toString() );
    assertEquals( "2 / 1", (2r).toString() );
    assertEquals( "11 / 10", (1.1r).toString() );
    assertEquals( Rational.get( "123456789012345678901234567890 / 1" ), new BigInteger( "123456789012345678901234567890" )r );
    assertEquals( Rational.get( "-123456789012345678901234567890 / 1" ), -(new BigInteger( "123456789012345678901234567890" )r) );

    assertEquals( "1/1", Rational.get( "1.3/1.3" ).toFractionString() );
    assertEquals( "1/3", Rational.get( ".1/.3" ).toFractionString() );
    assertEquals( "1/20", Rational.get( ".1/2" ).toFractionString() );
    assertEquals( "-1/20", Rational.get( "-.1/2" ).toFractionString() );
    assertEquals( "20/1", Rational.get( "2/.1" ).toFractionString() );
    assertEquals( "-20/1", Rational.get( "-2/.1" ).toFractionString() );
    assertEquals( "1/1", Rational.get( ".1/.1" ).toFractionString() );
    assertEquals( "1/1", Rational.get( ".1/.1" ).toFractionString() );
    assertEquals( "1/20", Rational.get( "5/100" ).toFractionString() );
    assertEquals( "-1/20", Rational.get( "-5/100" ).toFractionString() );
    try
    {
      Rational.get( "1/0" );
      fail();
    }
    catch( Exception e )
    {
    }
  }

//  @Test
//  public void testSequence() {
//    StringBuilder sb = new StringBuilder();
//    for( rat : (-2r..2r).step( 1r/3 ) ) {
//      sb.append( rat.toMixedString() ).append( ", " );
//    }
//    assertEquals( "-2, -1 2/3, -1 1/3, -1, -2/3, -1/3, 0, 1/3, 2/3, 1, 1 1/3, 1 2/3, 2, ", sb.toString() );
//  }

  @Test
  public void testSerialization()
  {
    Rational rat = 2r / 3;

    ByteArrayOutputStream ba = new ByteArrayOutputStream();
    try( ObjectOutputStream out = new ObjectOutputStream( ba ) )
    {
      out.writeObject( rat );
    }
    catch( Exception e )
    {
      fail();
    }
    try( ObjectInputStream inp = new ObjectInputStream( new ByteArrayInputStream( ba.toByteArray() ) ) )
    {
      Rational rat2 = (Rational)inp.readObject();
      assertTrue( rat == rat2 ); // tests with comparableWith equality
    }
    catch( Exception e )
    {
      fail();
    }
  }

  public void testParens()
  {
    RationalTest blah = new RationalTest();
    foo( (3 r) );
    blah.foo( (3r).toString() );
    new RationalTest().foo( (3r).toString() );

    int t = 5;
    Rational numb = (t r);
    assertEquals( 5r, numb );
  }

  public void testUnary()
  {
    Rational x = 3r;
    assertEquals( Rational.get( -3 ), -x );
    x = 3r;
    assertEquals( Rational.get( 3 ), x-- );
    x = 3r;
    assertEquals( Rational.get( 2 ), --x );
    x = 3r;
    assertEquals( Rational.get( 3 ), x++ );
    x = 3r;
    assertEquals( Rational.get( 4 ), ++x );
  }

  private void foo( Object s ) {}
}