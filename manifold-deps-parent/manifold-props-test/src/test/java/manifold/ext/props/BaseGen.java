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

import manifold.ext.props.rt.api.val;
import manifold.ext.props.rt.api.var;

import java.util.List;

public abstract class BaseGen<T, R>
{
  abstract @var List<T> names;
  abstract @var R result;
  abstract @var String abstractProp;

  final @var String finalProp;

//todo:  @val String valProp; // error for no init

  public void asdf() {}
}
