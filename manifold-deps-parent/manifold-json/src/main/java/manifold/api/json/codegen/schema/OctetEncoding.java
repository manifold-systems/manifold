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

package manifold.api.json.codegen.schema;

/**
 * Corresponds with the "binary" format.  See {@link BinaryFormatResolver}.
 */
public class OctetEncoding
{
  private final static char[] hexArray = "0123456789ABCDEF".toCharArray();

  private String _encoded;
  private byte[] _bytes;

  @SuppressWarnings("WeakerAccess")
  public static OctetEncoding encoded( String encoded )
  {
    return new OctetEncoding( encoded, null );
  }
  public static OctetEncoding decoded( byte[] bytes )
  {
    return new OctetEncoding( null, bytes );
  }

  private OctetEncoding( String encoded, byte[] decoded )
  {
    _encoded = encoded;
    _bytes = decoded;
  }

  @SuppressWarnings("unused")
  public byte[] getBytes()
  {
    if( _bytes != null )
    {
      return _bytes;
    }
    // not storing in _bytes because the string is stored in the bindings
    return decode( _encoded );
  }

  public String toString()
  {
    if( _encoded != null )
    {
      return _encoded;
    }

    char[] hexChars = new char[_bytes.length * 2];
    for( int j = 0; j < _bytes.length; j++ )
    {
      int v = _bytes[j] & 0xFF;
      hexChars[j * 2] = hexArray[v >>> 4];
      hexChars[j * 2 + 1] = hexArray[v & 0x0F];
    }
    _bytes = null; // release potentially large array
    return _encoded = new String( hexChars );
  }

  private byte[] decode( String encoded )
  {
    int len = encoded.length();
    byte[] data = new byte[len / 2];
    for( int i = 0; i < len; i += 2 )
    {
      data[i / 2] = (byte)((Character.digit( encoded.charAt( i ), 16 ) << 4)
                           + Character.digit( encoded.charAt( i + 1 ), 16 ));
    }
    return data;
  }
}
