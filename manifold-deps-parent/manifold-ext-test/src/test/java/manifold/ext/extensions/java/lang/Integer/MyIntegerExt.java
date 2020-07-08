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

package manifold.ext.extensions.java.lang.Integer;

import manifold.ext.rt.api.Extension;
import manifold.ext.rt.api.This;

/**
 * This extension is use primarily to test that constant fields are initialized with the values from the extended class.
 * This is necessary because the Java compiler optimizes compiled code to inline the values where the fields are used.
 */
@Extension
public class MyIntegerExt
{
  public static String myMethod( @This Integer thiz )
  {
    return "myMethod";
  }
}
