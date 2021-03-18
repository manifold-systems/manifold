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

package manifold.internal.javac;

import com.sun.source.tree.CompilationUnitTree;
import com.sun.source.tree.Tree;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;

/**
 * A mapping of child to parent for every child tree in a CompilationUnitTree. The tree node in the Java AST does not
 * maintain a reference to its parent, hence the need for this mapping.
 */
public class ParentMap
{
  private final Supplier<CompilationUnitTree> _compilationUnitSupplier;
  private final Map<CompilationUnitTree, Map<Tree, Tree>> _parents;

  public ParentMap( Supplier<CompilationUnitTree> compilationUnitSupplier )
  {
    _compilationUnitSupplier = compilationUnitSupplier;
    _parents = new HashMap<>();
  }

  public Tree getParent( Tree child )
  {
    Map<Tree, Tree> parents = _parents.computeIfAbsent( _compilationUnitSupplier.get(), cu -> {
      Map<Tree, Tree> map = new HashMap<>();
      new ParentTreePathScanner( map ).scan( cu, null );
      return map;
    } );
    return parents.get( child );
  }
}
