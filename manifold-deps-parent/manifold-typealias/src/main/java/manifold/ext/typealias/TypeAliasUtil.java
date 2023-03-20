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

package manifold.ext.typealias;

import com.sun.tools.javac.code.Symbol;
import com.sun.tools.javac.code.Type;
import com.sun.tools.javac.util.List;
import manifold.ext.typealias.rt.api.TypeAliasProvider;

public class TypeAliasUtil {

  private static final String TYPE_ALIAS_PROVIDER = TypeAliasProvider.class.getTypeName();

  static Type getAliasTypeFromInterface(Symbol.ClassSymbol sym) {
    return getAliasTypeFromInterface( sym.getInterfaces() );
  }

  static Type getAliasTypeFromInterface( List<Type> interfaces ) {
    for (Type type : interfaces) {
      if (TYPE_ALIAS_PROVIDER.equals(type.tsym.getQualifiedName().toString()) && type.isParameterized()) {
        return type.getTypeArguments().get(0);
      }
    }
    return null;
  }
}
