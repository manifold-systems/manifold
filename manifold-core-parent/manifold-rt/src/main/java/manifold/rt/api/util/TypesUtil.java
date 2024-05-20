/*
 * Copyright (c) 2023 - Manifold Systems LLC
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

package manifold.rt.api.util;

import com.sun.tools.javac.code.Attribute;
import com.sun.tools.javac.code.Symbol;
import com.sun.tools.javac.code.Type;
import com.sun.tools.javac.code.Types;

public interface TypesUtil
{
  String STRUCTURAL = "manifold.ext.rt.api.Structural";

  static boolean isStructuralInterface( Types types, Symbol sym )
  {
    if( sym == null )
    {
      return false;
    }

    if( (!sym.isInterface() || !sym.hasAnnotations()) && !(sym instanceof Symbol.TypeVariableSymbol) )
    {
      return false;
    }

    // use the raw type
    Type type = types.erasure( sym.type );
    sym = type.tsym;
    if( !sym.isInterface() || !sym.hasAnnotations() )
    {
      return false;
    }

    for( Attribute.Compound annotation : sym.getAnnotationMirrors() )
    {
      if( annotation.type != null && annotation.type.toString().equals( STRUCTURAL ) )
      {
        return true;
      }
    }
    return false;
  }
}
