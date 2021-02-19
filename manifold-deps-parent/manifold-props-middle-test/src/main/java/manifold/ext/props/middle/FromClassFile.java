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

import manifold.ext.props.rt.api.get;
import manifold.ext.props.rt.api.val;
import manifold.ext.props.rt.api.var;
import manifold.ext.props.rt.api.set;

import java.util.HashMap;
import java.util.Map;

public class FromClassFile
{
  @var static String staticReadwriteBackingProp = "staticReadwriteBackingProp";
  @val static final String staticFinalBackingProp = "staticFinalBackingProp";
  @val static String staticReadonlyBackingProp = "staticReadonlyBackingProp";

  public static void updateStaticReadonlyBackingProp()
  {
    // can modify read-only field internally, just no getter is made available
    staticReadonlyBackingProp = "updated";
  }

  static private Map<String, Integer> _staticMap = new HashMap<>();
  @var static int staticNonbackingProp;
  {
    _staticMap.put( "staticNonbackingProp", 8 );
  }
  public static int getStaticNonbackingProp()
  {
    return _staticMap.get( "staticNonbackingProp" );
  }
  public static void setStaticNonbackingProp( int value )
  {
    _staticMap.put( "staticNonbackingProp", value );
  }

  @var String readwriteBackingProp = "readwriteBackingProp";
  @var int int_readwriteBackingProp = 1;
  @get @set String readwriteBackingProp2 = "readwriteBackingProp2";
  @val String readonlyBackingProp = "readonlyBackingProp";
  @set String writeonlyBackingProp;

  @val final String finalBackingProp = "finalBackingProp";

  private Map<String, Integer> _map = new HashMap<>();
  @var int nonbacking;
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
