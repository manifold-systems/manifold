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

package manifold.api.util.fingerprint;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;

public final class Fingerprint
{
  private long _fp;

  /**
   * Initializes this object to the fingerprint of the empty string.
   */
  public Fingerprint()
  {
    _fp = IrredPoly;
  }

  /**
   * Initializes this fingerprint to a <em>copy</em> of <code>fp</code>,
   * which must be non-null.
   */
  public Fingerprint( Fingerprint fp )
  {
    _fp = fp._fp;
  }

  /**
   * Initializes this object to the fingerprint of the String
   * <code>s</code>, which must be non-null.
   */
  public Fingerprint( String s )
  {
    this();
    extend( s );
  }

  /**
   * Initializes this object to the fingerprint of the character
   * array <code>chars</code>, which must be non-null.
   */
  public Fingerprint( char[] chars )
  {
    this();
    extend( chars, 0, chars.length );
  }

  /**
   * Initializes this object to the fingerprint of the characters
   * <code>chars[start]..chars[start+length-1]</code>.
   */
  public Fingerprint( char[] chars, int start, int length )
  {
    this();
    extend( chars, start, length );
  }

  /**
   * Initializes this object to the fingerprint of the byte
   * array <code>bytes</code>, which must be non-null.
   */
  public Fingerprint( byte[] bytes )
  {
    this();
    extend( bytes, 0, bytes.length );
  }

  /**
   * Initializes this object to the fingerprint of the bytes
   * <code>bytes[start]..bytes[start+length-1]</code>.
   */
  public Fingerprint( byte[] bytes, int start, int length )
  {
    this();
    extend( bytes, start, length );
  }

  /**
   * Initializes this object to the fingerprint of the bytes
   * in <code>stream</code>, which must be non-null.
   *
   * @throws IOException if an error is encountered reading <code>stream</code>.
   */
  public Fingerprint( InputStream stream ) throws IOException
  {
    this();
    extend( stream );
  }

  /**
   * Initializes this object to the fingerprint of the bytes
   * in <code>buffer</code>, which must be non-null.
   *
   * @throws IOException if an error is encountered reading <code>stream</code>.
   */
  public Fingerprint( ByteBuffer buffer ) throws IOException
  {
    this();
    extend( buffer );
  }

  /**
   * Returns the value of this fingerprint as a newly-allocated array
   * of 8 bytes.<p>
   *
   * <b>Important:</b> If the output of this function is subsequently
   * fingerprinted, the probabilistic guarantee is lost. That is,
   * there is a much higher liklihood of fingerprint collisions if
   * fingerprint values are themselves fingerprinted in any way.
   */
  public byte[] toBytes()
  {
    return toBytes( new byte[8] );
  }

  /**
   * Returns the value of this fingerprint as an 8-byte array.
   * Unlike {@link #toBytes()}, this variant does not perform
   * an allocation. Instead, the client passes in a buffer into
   * which the fingerprint value is written. This can be used
   * to get the values of a set of fingerprints without having
   * to perform an allocation for each one.
   *
   * @param buff The buffer into which the bytes will be written. This
   *             array is required to be non-null and exactly 8 bytes in
   *             length. This buffer is returned.
   */
  public byte[] toBytes(/*INOUT*/ byte[] buff )
  {
    assert buff != null;
    assert (buff.length == 8) : "buff argument not an array of length 8";
    long val = _fp;
    for( int i = 0; i < buff.length; i++ )
    {
      buff[i] = (byte)(val & 0xFF);
      val >>>= 8;
    }
    return buff;
  }

  private static final String LEADING_ZEROS = "0000000000000000";

  /**
   * Returns the value of this fingerprint as an unsigned integer encoded
   * in base 16 (hexideicmal), padded with leading zeros to a total length
   * of 16 characters.<p>
   *
   * <b>Important:</b> If the output of this function is subsequently
   * fingerprinted, the probabilistic guarantee is lost. That is,
   * there is a much higher liklihood of fingerprint collisions if
   * fingerprint values are themselves fingerprinted in any way.
   */
  public String toHexString()
  {
    String res = Long.toHexString( _fp );
    int len = res.length();
    if( len < 16 )
    {
      res = LEADING_ZEROS.substring( len ) + res;
      assert res.length() == 16;
    }
    return res;
  }

  /**
   * Extends this fingerprint by the characters of the String
   * <code>s</code>, which must be non-null.
   *
   * @return the resulting fingerprint.
   */
  public Fingerprint extend( String s )
  {
    final int len = s.length();
    for( int i = 0; i < len; i++ )
    {
      extend( s.charAt( i ) );
    }
    return this;
  }

  /**
   * Extends this fingerprint by the characters
   * <code>chars[start]..chars[start+length-1]</code>.
   *
   * @return the resulting fingerprint.
   */
  public Fingerprint extend( char[] chars )
  {
    extend( chars, 0, chars.length );
    return this;
  }

  /**
   * Extends this fingerprint by the characters
   * <code>chars[start]..chars[start+length-1]</code>.
   *
   * @return the resulting fingerprint.
   */
  public Fingerprint extend( char[] chars, int start, int len )
  {
    int end = start + len;
    for( int i = start; i < end; i++ )
    {
      extend( chars[i] );
    }
    return this;
  }

  /**
   * Extends this fingerprint by the bytes
   * <code>bytes[offset]..bytes[offset+length-1]</code>.
   *
   * @return the resulting fingerprint.
   */
  public Fingerprint extend( byte[] bytes, int start, int len )
  {
    int end = start + len;
    for( int i = start; i < end; i++ )
    {
      extend( bytes[i] );
    }
    return this;
  }

  /**
   * Extends this fingerprint by the bytes
   *
   * @return the resulting fingerprint.
   */
  public Fingerprint extend( byte[] bytes )
  {
    for( byte aByte: bytes )
    {
      extend( aByte );
    }
    return this;
  }

  /**
   * Extends this fingerprint by the integer <code>i</code>.
   *
   * @return the resulting fingerprint.
   */
  public Fingerprint extend( int i )
  {
    extend( (byte)((i >>> 24) & 0xFF) );
    extend( (byte)((i >>> 16) & 0xFF) );
    extend( (byte)((i >>> 8) & 0xFF) );
    extend( (byte)((i) & 0xFF) );
    return this;
  }

  /**
   * Extends this fingerprint by the integer <code>i</code>.
   *
   * @return the resulting fingerprint.
   */
  public Fingerprint extend( long i )
  {
    extend( (byte)((i >>> 52) & 0xFF) );
    extend( (byte)((i >>> 48) & 0xFF) );
    extend( (byte)((i >>> 40) & 0xFF) );
    extend( (byte)((i >>> 32) & 0xFF) );
    extend( (byte)((i >>> 24) & 0xFF) );
    extend( (byte)((i >>> 16) & 0xFF) );
    extend( (byte)((i >>> 8) & 0xFF) );
    extend( (byte)((i) & 0xFF) );
    return this;
  }

  /**
   * Extends this fingerprint by the character <code>c</code>.
   *
   * @return the resulting fingerprint.
   */
  public Fingerprint extend( char c )
  {
    byte b1 = (byte)(c & 0xff);
    extend( b1 );
    byte b2 = (byte)(c >>> 8);
    // NOTE pdalbora 23-Jul-2009 -- The following check is intentional. We don't extend the high order byte when it's
    // zero, in order to avoid "weakening" the fingerprint of primarily ASCII data by adding unnecessary bits. The
    // tradeoff for doing this is that two characters in the low order byte range (<= 0x00FF) will be
    // indistinguishable from the corresponding character in the high order byte range (> 0x00FF). For example, the
    // character sequence (0x0022, 0x0021) will have the same fingerprint as the character sequence (0x2122). However,
    // it would be highly unlikely for this to happen, as ASCII data and non-ASCII data are unlikely to mix together.
    // Even in the case where such a sequence pair occurred in two strings, the likelihood of it yielding a collision
    // would be very low, when there other characters in the strings.
    if( b2 != 0 )
    {
      extend( b2 );
    }
    return this;
  }

  /**
   * Extends this fingerprint by the byte <code>b</code>.
   *
   * @return the resulting fingerprint.
   */
  public Fingerprint extend( byte b )
  {
    _fp = (_fp >>> 8) ^ ByteModTable[(b ^ (int)_fp) & 0xFF];
    return this;
  }

  /**
   * Extends this fingerprint by the bytes of the stream
   * <code>stream</code>, which must be non-null.
   *
   * @return the resulting fingerprint.
   *
   * @throws IOException if an error is encountered reading <code>stream</code>.
   */
  public Fingerprint extend( InputStream stream ) throws IOException
  {
    int b;
    while( (b = stream.read()) != -1 )
    {
      extend( (byte)b );
    }
    return this;
  }

  public Fingerprint extend( ByteBuffer buffer ) throws IOException
  {
    if( buffer.hasArray() )
    {
      byte[] bytes = buffer.array();
      return extend( bytes, 0, bytes.length );
    }
    else
    {
      while( buffer.hasRemaining() )
      {
        extend( buffer.get() );
      }
    }
    return this;
  }

  @Override
  public int hashCode()
  {
    return ((int)_fp) ^ ((int)(_fp >>> 32));
  }

  @Override
  public boolean equals( Object obj )
  {
    if( this == obj )
    {
      return true;
    }
    if( !(obj instanceof Fingerprint) )
    {
      return false;
    }
    return _fp == ((Fingerprint)obj)._fp;
  }

  /* This class provides methods that construct fingerprints of
     strings of bytes via operations in GF[2^64].  GF[2^64] is represented
     as the set polynomials of degree 64 with coefficients in Z(2),
     modulo an irreducible polynomial P of degree 64.  The internal
     representation is a 64-bit Java value of type "long".

     Let g(S) be the string obtained from S by prepending the byte 0x80
     and appending eight 0x00 bytes.  Let f(S) be the polynomial
     associated to the string g(S) viewed as a polynomial with
     coefficients in the field Z(2). The fingerprint of S is simply
     the value f(S) modulo P.

     The irreducible polynomial p used as a modulus is

            3    7    11    13    16    19    20    24    26    28
       1 + x  + x  + x   + x   + x   + x   + x   + x   + x   + x

          29    30    36    37    38    41    42    45    46    48
       + x   + x   + x   + x   + x   + x   + x   + x   + x   + x

          50    51    52    54    56    57    59    61    62    64
       + x   + x   + x   + x   + x   + x   + x   + x   + x   + x

     IrredPoly is its representation. */

  // implementation constants
  // polynomials are represented with the coefficient for x^0
  // in the most significant bit
  private static final long Zero = 0L;
  private static final long One = 0x8000000000000000L;
  private static final long IrredPoly = 0x911498AE0E66BAD6L;
  private static final long X63 = 0x1L; // coefficient of x^63

  /* This is the table used for extending fingerprints.  The
   * value ByteModTable[i] is the value to XOR into the finger-
   * print value when the byte with value "i" is shifted from
   * the top-most byte in the fingerprint. */
  private static long[] ByteModTable;

  // Initialization code
  static
  {
    // Maximum power needed == 64 + 8
    int plength = 72;
    long[] powerTable = new long[plength];

    long t = One;
    for( int i = 0; i < plength; i++ )
    {
      powerTable[i] = t;
      //System.out.println("pow[" + i + "] = " + Long.toHexString(t));

      // t = t * x
      long mask = ((t & X63) != 0) ? IrredPoly : 0;
      t = (t >>> 1) ^ mask;
    }

    // group bit-wise overflows into bytes
    ByteModTable = new long[256];
    for( int j = 0; j < ByteModTable.length; j++ )
    {
      long v = Zero;
      for( int k = 0; k < 9; k++ )
      {
        if( (j & (1L << k)) != 0 )
        {
          v ^= powerTable[(plength - 1) - k];
        }
      }
      ByteModTable[j] = v;
      //System.out.println("ByteModTable[" + j + "] = " + Long.toHexString(v));
    }
  }

  public long getRawFingerprint()
  {
    return _fp;
  }

  public String toString()
  {
    return "" + _fp;
  }
}

