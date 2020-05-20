/*
 * Copyright (c) 2020 - Manifold Systems LLC
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

package manifold.json.rt.api;

import java.util.Base64;

/**
 * Corresponds with the "byte" format.  See {@code BinaryFormatResolver}.
 */
public class Base64Encoding
{
  private String _encoded;
  private byte[] _bytes;

  @SuppressWarnings("WeakerAccess")
  public static Base64Encoding encoded( String encoded )
  {
    return new Base64Encoding( encoded, null );
  }
  public static Base64Encoding decoded( byte[] bytes )
  {
    return new Base64Encoding( null, bytes );
  }

  private Base64Encoding( String encoded, byte[] decoded )
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
    // not storing in _bytes because the string is in the bindings
    return Base64.getDecoder().decode( _encoded );
  }

  public String toString()
  {
    if( _encoded != null )
    {
      return _encoded;
    }

    String encoded = new String( Base64.getEncoder().encode( _bytes ) );
    _bytes = null; // release potentially large array
    return _encoded = encoded;
  }
}
