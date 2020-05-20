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

package manifold.api.yaml.rt;

import manifold.json.rt.api.IJsonBindingsCodec;

public class YamlBindingsCodec implements IJsonBindingsCodec
{
  @Override
  public String getName()
  {
    return "YAML";
  }

  @Override
  public String encode( Object jsonValue )
  {
    StringBuilder target = new StringBuilder();
    Yaml.toYaml( jsonValue, target );
    return target.toString();
  }

  @Override
  public void encode( Object jsonValue, StringBuilder target )
  {
    Yaml.toYaml( jsonValue, target );
  }

  @Override
  public void encode( Object jsonValue, String name, StringBuilder target, int indent )
  {
    Yaml.toYaml( jsonValue, target );
  }

  @Override
  public Object decode( String encoded )
  {
    return Yaml.fromYaml( encoded );
  }

  @Override
  public Object decode( String encoded, boolean withTokens )
  {
    return Yaml.fromYaml( encoded, false, withTokens );
  }

  @Override
  public Object decode( String encoded, boolean withBigNumbers, boolean withTokens )
  {
    return Yaml.fromYaml( encoded, withBigNumbers, withTokens );
  }
}
