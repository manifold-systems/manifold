/*
 * Copyright (c) 2021 - Manifold Systems LLC
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

package manifold.ext.props.middle;

import manifold.ext.props.rt.api.PropOption;
import manifold.ext.props.rt.api.get;
import manifold.ext.props.rt.api.prop;
import manifold.ext.props.rt.api.set;

import java.util.HashMap;
import java.util.Map;

public class FromClassFile
{
  private Map<String, Integer> _map = new HashMap<>();

  @prop public String readwriteBackingProp = "a1";
  @get @set public String readwriteBackingProp2 = "a2";
  @get public String readonlyBackingProp = "a3";
  @set public String writeonlyBackingProp;

  @prop public int nonbacking;
  {
    _map.put( "nonbacking", 8 );
  }

  public int getNonbacking()
  {
    return _map.get( "nonbacking" );
  }
  public void setNonbacking( int value )
  {
    _map.put( "nonbacking", value );
  }
}
