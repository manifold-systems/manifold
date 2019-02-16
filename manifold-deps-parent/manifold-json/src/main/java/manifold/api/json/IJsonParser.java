/*
 * Copyright (c) 2018 - Manifold Systems LLC
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

import javax.script.ScriptException;

/**
 */
public interface IJsonParser
{
  /**
   * Parse Json text as a standard javax.script.Bindings object.
   *
   * @param jsonText Any Json text, can be an object, a list, or simple value.
   * @param withBigNumbers Parse decimal numbers as BigDecimals and integers and BigIntegers,
   *                       otherwise they are Double and Integer.
   * @param withTokens Store tokens for Json name value pairs.  The token contains positional
   *                   information for tooling e.g., to facilitate navigation in an IDE.  This
   *                   parameter should be false for normal use-cases.
   * @return A JSON value (primitive/boxed type, String, List of JSON values, or Bindings of String/JSON value)
   * @throws ScriptException
   */
  Object parseJson( String jsonText, boolean withBigNumbers, boolean withTokens ) throws ScriptException;

  static IJsonParser getDefaultParser()
  {
    return DefaultParser.instance();
    //return NashornJsonParser.instance();
  }
}
