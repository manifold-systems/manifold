package manifold.internal.javac;

import com.sun.source.tree.Tree;
import com.sun.source.util.TreeScanner;

public class TreePathScanner2 extends TreeScanner<TreePath2, Tree>
{
  private TreePath2 _path;
  
  /**
   * Scan a tree from a position identified by a TreePath2.
   */
  public TreePath2 scan( TreePath2 path, Tree p )
  {
    _path = path;
    try
    {
      return path.getLeaf().accept( this, p );
    }
    finally
    {
      _path = null;
    }
  }

  /**
   * Scan a single node.
   * The current path is updated for the duration of the scan.
   */
  @Override
  public TreePath2 scan( Tree tree, Tree p )
  {
    if( tree == null )
    {
      return null;
    }

    TreePath2 prev = _path;
    _path = new TreePath2( _path, tree );
    try
    {
      return tree.accept( this, p );
    }
    finally
    {
      _path = prev;
    }
  }

  /**
   * Get the current path for the node, as built up by the currently
   * active set of scan calls.
   */
  public TreePath2 getCurrentPath()
  {
    return _path;
  }
}

