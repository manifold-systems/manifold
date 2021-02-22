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

package manifold.ext.props.example;

import manifold.ext.props.rt.api.*;

import java.util.HashMap;
import java.util.Map;

import static manifold.ext.props.rt.api.PropOption.Private;

/**
 * FromSourceFile, as opposed to FromClassFile --
 * testing properties from .java files v. testing properties from .class files.
 */
public class FromSourceFile
{
  @var static String staticReadwriteBackingProp = "staticReadwriteBackingProp";
  @var @set(Private) static String staticReadonlyBackingProp = "staticReadonlyBackingProp";

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

  private Map<String, Integer> _map = new HashMap<>();

  @var String readwriteBackingProp = "readwriteBackingProp";
  @get @set public String readwriteBackingProp2 = "readwriteBackingProp2";
  @val String readonlyBackingProp = "readonlyBackingProp";
  @set public String writeonlyBackingProp;

  @val final String finalBackingProp = "finalBackingProp";

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
