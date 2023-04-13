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
import com.sun.tools.javac.tree.JCTree;
import com.sun.tools.javac.tree.TreeInfo;
import com.sun.tools.javac.util.List;
import com.sun.tools.javac.util.Name;

public class Util {

  static Type getAliasTypeFromInterface(Symbol.ClassSymbol sym, String fqn) {
    return getAliasTypeFromInterface(sym.getInterfaces(), fqn);
  }

  static Type getAliasTypeFromInterface(List<Type> interfaces, String fqn) {
    for (Type type : interfaces) {
      if (fqn.equals(type.tsym.getQualifiedName().toString()) && type.isParameterized()) {
        return type.getTypeArguments().get(0);
      }
    }
    return null;
  }

  static List<JCTree.JCExpression> copyTypeArguments(List<JCTree.JCExpression> oldArguments, List<Type> oldArgumentTypes, List<JCTree.JCExpression> newArguments, List<Type> newArgumentTypes) {
    // is a raw parameterized.
    if (oldArguments.isEmpty()) {
      return oldArguments;
    }
    JCTree.JCExpression[] resolvedArguments = new JCTree.JCExpression[newArgumentTypes.size()];
    for (int i = 0; i <  resolvedArguments.length; ++i) {
      JCTree.JCExpression argument = newArguments[i];
      int index = oldArgumentTypes.indexOf(newArgumentTypes[i]);
      if (index >= 0 && index < oldArguments.size()) {
        argument = oldArguments.get(index);
      }
      resolvedArguments[i] = argument;
    }
    return List.from(resolvedArguments);
  }

  static String fullName(JCTree tree) {
    Name name = TreeInfo.fullName(tree);
    if (name != null) {
      return name.toString();
    }
    return "";
  }
}
