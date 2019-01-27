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

package manifold.api.json;

import java.net.URL;
import javax.script.Bindings;
import manifold.json.extensions.java.net.URL.ManUrlExt;

/**
 * This class is used as part of the JSON API. It defines methods to send this JSON object
 * via HTTP POST to a specified URL and receive content in various formats.
 */
public class Poster
{
  private final Bindings _bindings;

  public Poster( Bindings bindings )
  {
    _bindings = bindings;
  }

  /**
   * Send this JSON object via HTTP POST to {@code url}
   * @param url A URL expecting a post of this JSON object
   * @return The resulting JSON content as a JSON bindings object.
   * ou can directly cast this bindings as a JSON API interface.
   */
  public Bindings toJsonUrl( java.net.URL url )
  {
    return ManUrlExt.postForJsonContent( url, _bindings );
  }

  /**
   * Send this JSON object via HTTP POST to {@code url}
   * @param url A URL expecting a post of this JSON object
   * @return The resulting YAML content as a JSON bindings object
   * You can directly cast this bindings as a JSON API interface.
   */
  public Bindings toYamlUrl( java.net.URL url )
  {
    return ManUrlExt.postForYamlContent( url, _bindings );
  }

  /**
   * Send this JSON object via HTTP POST to {@code url}
   * @param url A URL expecting a post of this JSON object
   * @return The resulting raw text content
   */
  public String toTextUrl( URL url )
  {
    return ManUrlExt.postForTextContent( url, _bindings );
  }
}
