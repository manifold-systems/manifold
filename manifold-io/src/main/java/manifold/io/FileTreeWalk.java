package manifold.io;

import java.io.File;
import java.io.IOException;
import java.nio.file.AccessDeniedException;
import java.util.Iterator;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;
import manifold.util.AbstractIterator;
import manifold.util.Stack;

/**
 * This class is intended to implement different file traversal methods.
 * It allows to iterate through all files inside a given directory.
 * <p>
 * Use [File.walk], [File.walkTopDown] or [File.walkBottomUp] extension functions to instantiate a `FileTreeWalk` instance.
 * <p>
 * If the file path given is just a file, walker iterates only it.
 * If the file path given does not exist, walker iterates nothing, i.e. it's equivalent to an empty sequence.
 */
public class FileTreeWalk implements Iterable<File>
{
  private final File start;
  private final FileWalkDirection direction; //= FileWalkDirection.TOP_DOWN;
  private final Function<File, Boolean> onEnter;
  private final Consumer<File> onLeave;
  private final BiConsumer<File, IOException> onFail;
  private final int maxDepth; // = Integer.MAX_VALUE;

  public FileTreeWalk( File start, FileWalkDirection direction, Function<File, Boolean> onEnter, Consumer<File> onLeave, BiConsumer<File, IOException> onFail, int maxDepth )
  {
    this.start = start;
    this.direction = direction;
    this.onEnter = onEnter;
    this.onLeave = onLeave;
    this.onFail = onFail;
    this.maxDepth = maxDepth;
  }

  public FileTreeWalk( File start, FileWalkDirection direction )
  {
    this( start, direction, null, null, null, Integer.MAX_VALUE );
  }

  public FileTreeWalk( File start )
  {
    this( start, FileWalkDirection.TOP_DOWN );
  }


  /**
   * Returns an iterator walking through files.
   */
  public Iterator<File> iterator()
  {
    return new FileTreeWalkIterator();
  }

  /**
   * Abstract class that encapsulates file visiting in some order, beginning from a given [root]
   */
  private abstract class WalkState
  {
    final File root;

    WalkState( File root )
    {
      this.root = root;
    }

    /**
     * Call of this function proceeds to a next file for visiting and returns it
     */
    abstract public File step();
  }

  /**
   * Abstract class that encapsulates directory visiting in some order, beginning from a given [rootDir]
   */
  private abstract class DirectoryState extends WalkState
  {
    private File rootDir;

    DirectoryState( File rootDir )
    {
      super( rootDir );
      this.rootDir = rootDir;
    }
  }

  private class FileTreeWalkIterator extends AbstractIterator<File>
  {

    // Stack of directory states, beginning from the start directory
    private Stack<WalkState> state = new Stack<>();

    FileTreeWalkIterator()
    {
      if( start.isDirectory() )
      {
        state.push( directoryState( start ) );
      }
      else if( start.isFile() )
      {
        state.push( new SingleFileState( start ) );
      }
      else
      {
        done();
      }

    }

    @Override
    public void computeNext()
    {
      File nextFile = gotoNext();
      if( nextFile != null )
      {
        setNext( nextFile );
      }
      else
      {
        done();
      }
    }


    private DirectoryState directoryState( File root )
    {
      return direction == FileWalkDirection.TOP_DOWN
             ? new TopDownDirectoryState( root )
             : new BottomUpDirectoryState( root );
    }

    private File gotoNext()
    {
      if( state.isEmpty() )
      {
        // There is nothing in the state
        return null;
      }
      // Take next file from the top of the stack
      WalkState topState = state.peek();
      File file = topState.step();
      if( file == null )
      {
        // There is nothing more on the top of the stack, go back
        state.pop();
        return gotoNext();
      }
      else
      {
        // Check that file/directory matches the filter
        if( file == topState.root || !file.isDirectory() || state.size() >= maxDepth )
        {
          // Proceed to a root directory or a simple file
          return file;
        }
        else
        {
          // Proceed to a sub-directory
          state.push( directoryState( file ) );
          return gotoNext();
        }
      }
    }

    /**
     * Visiting in bottom-up order
     */
    private class BottomUpDirectoryState extends DirectoryState
    {
      private boolean rootVisited;
      private File[] fileList;
      private int fileIndex;
      private boolean failed;

      BottomUpDirectoryState( File rootDir )
      {
        super( rootDir );
      }

      /**
       * First all children, then root directory
       */
      @Override
      public File step()
      {
        if( !failed && fileList == null )
        {
          if( onEnter != null && !onEnter.apply( root ) )
          {
            return null;
          }

          fileList = root.listFiles();
          if( fileList == null )
          {
            if( onFail != null )
            {
              onFail.accept( root, new AccessDeniedException( root.toString(), null, "Cannot list files in a directory" ) );
            }
            failed = true;
          }
        }
        if( fileList != null && fileIndex < fileList.length )
        {
          // First visit all files
          return fileList[fileIndex++];
        }
        else if( !rootVisited )
        {
          // Then visit root
          rootVisited = true;
          return root;
        }
        else
        {
          // That's all
          if( onLeave != null )
          {
            onLeave.accept( root );
          }
          return null;
        }
      }
    }

    /**
     * Visiting in top-down order
     */
    private class TopDownDirectoryState extends DirectoryState
    {
      private boolean rootVisited;
      private File[] fileList;
      private int fileIndex;

      TopDownDirectoryState( File rootDir )
      {
        super( rootDir );
      }

      /**
       * First root directory, then all children
       */
      @Override
      public File step()
      {
        if( !rootVisited )
        {
          // First visit root
          if( onEnter != null )
          {
            if( !onEnter.apply( root ) )
            {
              return null;
            }
          }
          rootVisited = true;
          return root;
        }
        else if( fileList == null || fileIndex < fileList.length )
        {
          if( fileList == null )
          {
            // Then read an array of files, if any
            fileList = root.listFiles();
            if( fileList == null )
            {
              if( onFail != null )
              {
                onFail.accept( root, new AccessDeniedException( root.toString(), null, "Cannot list files in a directory" ) );
              }
            }
            if( fileList == null || fileList.length == 0 )
            {
              if( onLeave != null )
              {
                onLeave.accept( root );
              }
              return null;
            }
          }
          // Then visit all files
          return fileList[fileIndex++];
        }
        else
        {
          // That's all
          if( onLeave != null )
          {
            onLeave.accept( root );
          }
          return null;
        }
      }
    }

    private class SingleFileState extends WalkState
    {
      private boolean visited;

      SingleFileState( File root )
      {
        super( root );
      }

      @Override
      public File step()
      {
        if( visited )
        {
          return null;
        }
        visited = true;
        return root;
      }
    }

  }

  /**
   * Sets a predicate [function], that is called on any entered directory before its files are visited
   * and before it is visited itself.
   * <p>
   * If the [function] returns `false` the directory is not entered and neither it nor its files are visited.
   */
  public FileTreeWalk onEnter( Function<File, Boolean> function )
  {
    return new FileTreeWalk( start, direction, function, onLeave, onFail, maxDepth );
  }

  /**
   * Sets a callback [function], that is called on any left directory after its files are visited and after it is visited itself.
   */
  public FileTreeWalk onLeave( Consumer<File> function )
  {
    return new FileTreeWalk( start, direction, onEnter, function, onFail, maxDepth );
  }

  /**
   * Set a callback [function], that is called on a directory when it's impossible to get its file list.
   * <p>
   * [onEnter] and [onLeave] callback functions are called even in this case.
   */
  public FileTreeWalk onFail( BiConsumer<File, IOException> function )
  {
    return new FileTreeWalk( start, direction, onEnter, onLeave, function, maxDepth );
  }

  /**
   * Sets the maximum [depth] of a directory tree to traverse. By default there is no limit.
   * <p>
   * The value must be positive and [Int.MAX_VALUE] is used to specify an unlimited depth.
   * <p>
   * With a value of 1, walker visits only the origin directory and all its immediate children,
   * with a value of 2 also grandchildren, etc.
   */
  public FileTreeWalk maxDepth( int depth )
  {
    if( depth <= 0 )
    {
      throw new IllegalArgumentException( "depth must be positive, but was $depth." );
    }
    return new FileTreeWalk( start, direction, onEnter, onLeave, onFail, depth );
  }

  /**
   * An enumeration to describe possible walk directions.
   * There are two of them: beginning from parents, ending with children,
   * and beginning from children, ending with parents. Both use depth-first search.
   */
  public enum FileWalkDirection
  {
    /**
     * Depth-first search, directory is visited BEFORE its files
     */
    TOP_DOWN,
    /**
     * Depth-first search, directory is visited AFTER its files
     */
    BOTTOM_UP
    // Do we want also breadth-first search?
  }
}
