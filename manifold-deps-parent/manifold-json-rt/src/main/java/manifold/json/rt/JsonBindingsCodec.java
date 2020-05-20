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

package manifold.json.rt;

import manifold.json.rt.api.IJsonBindingsCodec;

public class JsonBindingsCodec implements IJsonBindingsCodec
{
  @Override
  public String getName()
  {
    return "JSON";
  }

  @Override
  public String encode( Object jsonValue )
  {
    return Json.toJson( jsonValue );
  }

  @Override
  public void encode( Object jsonValue, StringBuilder target )
  {
    Json.toJson( target, 0, jsonValue );
  }

  @Override
  public void encode( Object jsonValue, String name, StringBuilder target, int indent )
  {
    Json.toJson( target, indent, jsonValue );
  }

  @Override
  public Object decode( String encoded )
  {
    return Json.fromJson( encoded );
  }

  @Override
  public Object decode( String encoded, boolean withTokens )
  {
    return Json.fromJson( encoded, false, withTokens );
  }

  @Override
  public Object decode( String encoded, boolean withBigNumbers, boolean withTokens )
  {
    return Json.fromJson( encoded, withBigNumbers, withTokens );
  }

  @Override
  public StringBuilder appendValue( StringBuilder sb, Object comp )
  {
    return Json.appendValue( sb, comp );
  }
}
