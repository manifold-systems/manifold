/*
 * Copyright (c) 2018 - Manifold Systems LLC
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

import com.sun.source.tree.Tree;
import com.sun.source.util.TreeScanner;
import java.util.Map;
import manifold.api.util.Stack;

public class ParentTreePathScanner extends TreeScanner<Tree, Void>
{
  private final Map<Tree, Tree> _parents;
  private Stack<Tree> _parent;

  ParentTreePathScanner( Map<Tree, Tree> parents )
  {
    _parents = parents;
    _parent = new Stack<>();
    _parent.push( null );
  }

  /**
   * build a map of child tree to parent tree
   */
  public Tree scan( Tree path, Void p )
  {
    if( path == null )
    {
      return null;
    }

    _parents.put( path, _parent.peek() );
    _parent.push( path );
    try
    {
      return super.scan( path, null );
    }
    finally
    {
      _parent.pop();
    }
  }
}

