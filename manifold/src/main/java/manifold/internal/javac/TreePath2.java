package manifold.internal.javac;

import com.sun.source.tree.Tree;
import java.util.Iterator;

/**
 */
public class TreePath2 implements Iterable<Tree>
{
  private Tree _root;
  private Tree _leaf;
  private TreePath2 _parent;
  
  /**
   * Gets a tree path for a tree node within a compilation unit.
   *
   * @return null if the node is not found
   */
  public static TreePath2 getPath( Tree root, Tree target )
  {
    return getPath( new TreePath2( root ), target );
  }

  /**
   * Gets a tree path for a tree node within a subtree identified by a TreePath object.
   *
   * @return null if the node is not found
   */
  public static TreePath2 getPath( TreePath2 path, Tree target )
  {
    path.getClass();
    target.getClass();

    class Result extends Error
    {
      static final long serialVersionUID = -5942088234594905625L;
      TreePath2 path;

      Result( TreePath2 path )
      {
        this.path = path;
      }
    }

    class PathFinder extends TreePathScanner2
    {
      public TreePath2 scan( Tree tree, Tree target )
      {
        if( tree == target )
        {
          throw new Result( new TreePath2( getCurrentPath(), target ) );
        }
        return super.scan( tree, target );
      }
    }

    if( path.getLeaf() == target )
    {
      return path;
    }

    try
    {
      new PathFinder().scan( path, target );
    }
    catch( Result result )
    {
      return result.path;
    }
    return null;
  }

  /**
   * Creates a TreePath for a root node.
   */
  public TreePath2( Tree root )
  {
    _root = root;
    _leaf = root;
    _parent = null;
  }

  /**
   * Creates a TreePath for a child node.
   */
  public TreePath2( TreePath2 parent, Tree t )
  {
    _root = parent._root;
    _parent = parent;
    _leaf = t;
  }

  /**
   * Get root associated with this path.
   */
  public Tree getRoot()
  {
    return _root;
  }

  /**
   * Get the leaf node for this path.
   */
  public Tree getLeaf()
  {
    return _leaf;
  }

  /**
   * Get the path for the enclosing node, or null if there is no enclosing node.
   */
  public TreePath2 getParentPath()
  {
    return _parent;
  }

  /**
   * Iterates from leaves to root.
   */
  @Override
  public Iterator<Tree> iterator()
  {
    return new Iterator<Tree>()
    {
      @Override
      public boolean hasNext()
      {
        return next != null;
      }

      @Override
      public Tree next()
      {
        Tree t = next._leaf;
        next = next._parent;
        return t;
      }

      @Override
      public void remove()
      {
        throw new UnsupportedOperationException();
      }

      private TreePath2 next = TreePath2.this;
    };
  }

}

