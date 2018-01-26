package manifold.internal.javac;

import com.sun.source.tree.Tree;
import com.sun.source.util.TreeScanner;
import java.util.Map;
import manifold.util.Stack;

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

