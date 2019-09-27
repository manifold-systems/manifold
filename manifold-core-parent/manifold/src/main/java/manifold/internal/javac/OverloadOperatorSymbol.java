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

package manifold.internal.javac;

import com.sun.tools.javac.code.Symbol;
import com.sun.tools.javac.jvm.ByteCodes;
import manifold.util.ReflectUtil;

public class OverloadOperatorSymbol extends Symbol.OperatorSymbol
{
  private final MethodSymbol _methodSymbol;
  private final boolean _swapped;

  OverloadOperatorSymbol( MethodSymbol m, boolean swapped )
  {
    super( m.name, m.type, ByteCodes.nop, m.owner );
    ReflectUtil.field( this, "flags_field" ).set( m.flags() );
    _methodSymbol = m;
    _swapped = swapped;
  }

  public MethodSymbol getMethod()
  {
    return _methodSymbol;
  }

  public boolean isSwapped()
  {
    return _swapped;
  }
}
