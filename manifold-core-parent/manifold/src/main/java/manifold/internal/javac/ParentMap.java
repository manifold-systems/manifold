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
import com.sun.tools.javac.tree.JCTree;
import manifold.rt.api.util.Pair;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

/**
 * A mapping of child to parent for every child tree in a CompilationUnitTree. The tree node in the Java AST does not
 * maintain a reference to its parent, hence the need for this mapping.
 */
public class ParentMap
{
  private final Supplier<CompilationUnitTree> _compilationUnitSupplier;
  private final Map<CompilationUnitTree, Pair<Map<Tree, Tree>, Map<Integer, List<Tree>>>> _parents;

  public ParentMap( Supplier<CompilationUnitTree> compilationUnitSupplier )
  {
    _compilationUnitSupplier = compilationUnitSupplier;
    _parents = new HashMap<>();
  }

  public Tree getParent( Tree child )
  {
    return getParent( child, _compilationUnitSupplier.get() );
  }

  public Tree getParent( Tree child, CompilationUnitTree compilationUnitTree )
  {
    Pair<Map<Tree, Tree>, Map<Integer, List<Tree>>> parents = _parents.computeIfAbsent( compilationUnitTree, cu -> {
      Map<Tree, Tree> map = new HashMap<>();
      Map<Integer, List<Tree>> map2 = new HashMap<>();
      new ParentTreePathScanner( map, map2 ).scan( cu, null );
      return new Pair<>( map, map2 );
    } );
    Tree tree = parents.getFirst().get( child );
    if( tree == null && child instanceof JCTree )
    {
      tree = getParentByPos( (JCTree)child, parents );
    }
    return tree;
  }

  // sometimes, such as during speculative attribution, a tree is copied and worked on independently,
  // as a consequence the copied tree won't match a tree in the map, we are forced to find the parent
  // based on some other means like position in file
  //
  // todo: maybe add an alternative public method here to access the list directly? For now, returning outermost parent.
  private static Tree getParentByPos( JCTree child, Pair<Map<Tree, Tree>, Map<Integer, List<Tree>>> parents )
  {
    Tree tree = null;
    List<Tree> posParents = parents.getSecond().get( child.pos );
    if( posParents != null )
    {
      if( posParents.size() == 1 )
      {
        tree = posParents.get( 0 );
      }
      else
      {
        tree = posParents.stream()
          .filter( t -> t instanceof JCTree && ((JCTree)t).pos != child.pos )
          .findFirst().orElse( null );
      }
    }
    return tree;
  }
}
