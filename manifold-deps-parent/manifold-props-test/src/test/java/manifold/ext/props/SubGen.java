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

package manifold.ext.props;

import manifold.ext.props.rt.api.override;
import manifold.ext.props.rt.api.val;
import manifold.ext.props.rt.api.var;

import java.util.List;

public class SubGen extends BaseGen<String, Integer>
{
  @override @var List<String> names;
  @override @var Integer result;
  @override @var String abstractProp;

// cannot override final prop
//  @override @var String finalProp;


  public SubGen( List<String> names, Integer result )
  {
    this.names = names;
    this.result = result;
  }
}
