/*
 * Copyright (c) 2019 - Manifold Systems LLC
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

package manifold.js;

import org.mozilla.javascript.Context;
import org.mozilla.javascript.ScriptableObject;

class SharedScope
{
  private static final ThreadLocal<ScriptableObject> SHARED_SCOPE =
    ThreadLocal.withInitial( () -> Context.enter().initStandardObjects() );

  private static ScriptableObject get()
  {
    return SHARED_SCOPE.get();
  }

  /**
   * Static scope applies to a program or class and is analogous to Java's Class scope.  An instance scope is created
   * when an object is constructed via Context.newObject(), which is called from the manifold generated constructor for
   * a js class.
   */
  static ScriptableObject newStaticScope()
  {
    ScriptableObject sharedGlobalScope = SharedScope.get();
    ScriptableObject programScope = (ScriptableObject)Context.getCurrentContext().newObject( sharedGlobalScope );
    programScope.setPrototype( sharedGlobalScope );
    programScope.setParentScope( null );
    return programScope;
  }
}
