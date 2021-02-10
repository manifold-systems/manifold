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

import java.util.List;

public class NonbackingWithPropOptions
{
  @prop( PropOption.Final ) public String name;

  @prop( PropOption.Final ) public String nonbackingFinal;
  final public String getNonbackingFinal()
  {
    return "hi";
  }
  final public void setNonbackingFinal( String value )
  {
    // whatever
  }
}
