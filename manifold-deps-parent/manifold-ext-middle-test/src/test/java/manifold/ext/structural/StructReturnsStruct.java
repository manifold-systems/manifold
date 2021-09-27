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

package manifold.ext.structural;

import manifold.ext.rt.api.Structural;

/**
 * Tests that a proxy implementing the interface relays the returned structurally
 * implemented type without incident. For example, Java's Proxy classes add a CHECKCAST
 * instruction on the return value from the invocation handler, which blows up. We have
 * our own Proxy impl that doesn't insert the CHECKCAST.
 */
@Structural
public interface StructReturnsStruct
{
  StructReturnsStruct returnStruct();

  class Impl
  {
    public StructReturnsStruct returnStruct()
    {
      return (StructReturnsStruct)new Impl();
    }
  }
}
