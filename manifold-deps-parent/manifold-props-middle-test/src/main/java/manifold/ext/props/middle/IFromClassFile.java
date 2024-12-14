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

import manifold.ext.props.rt.api.var;

public interface IFromClassFile
{
// interfaces prohibit writable static property; cannot have initializer
//     java.lang.ClassFormatError: Illegal field modifiers in class manifold/ext/props/middle/IFromClassFile: 0xA
//  can't do this:
//  @var static String staticNonFinalBackingProp = "";

// writable abstract property cannot have initializer
//  @var String defaultGetter = "hi";

// this must be inferred:
//  @var static int staticNonbackingProp;

  static int getStaticNonbackingProp()
  {
    return 5;
  }
  static void setStaticNonbackingProp( int value )
  {
    throw new UnsupportedOperationException( String.valueOf( value ) );
  }
}
