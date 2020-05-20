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

package manifold.csv.rt;

import manifold.json.rt.api.IJsonBindingsCodec;

public class CsvBindingsCodec implements IJsonBindingsCodec
{
  @Override
  public String getName()
  {
    return "CSV";
  }

  @Override
  public String encode( Object jsonValue )
  {
    return Csv.toCsv( jsonValue );
  }

  @Override
  public void encode( Object jsonValue, StringBuilder target )
  {
    Csv.toCsv( jsonValue, "data", target, 0 );
  }

  @Override
  public void encode( Object jsonValue, String name, StringBuilder target, int indent )
  {
    Csv.toCsv( jsonValue, name, target, indent );
  }

  @Override
  public Object decode( String encoded )
  {
    return Csv.fromCsv( encoded );
  }

  @Override
  public Object decode( String encoded, boolean withTokens )
  {
    return Csv.fromCsv( encoded, withTokens );
  }

  @Override
  public Object decode( String encoded, boolean withBigNumbers, boolean withTokens )
  {
    return Csv.fromCsv( encoded, withTokens );
  }
}
