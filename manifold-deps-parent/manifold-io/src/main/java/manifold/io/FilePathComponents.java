package manifold.io;

import manifold.io.extensions.java.io.File.ManFileExt;
import java.io.File;
import java.util.List;

/**
 * Represents the path to a file as a collection of directories.
 *
 * @property root the [File] object representing root of the path (for example, `/` or `C:` or empty for relative paths).
 * @property segments the list of [File] objects representing every directory in the path to the file,
 * up to an including the file itself.
 */
public class FilePathComponents
{
  public final File root;
  public final List<File> segments;

  public FilePathComponents( File root, List<File> segments )
  {
    this.root = root;
    this.segments = segments;
  }

  /**
   * Returns a string representing the root for this file, or an empty string is this file name is relative.
   */
  public String rootName()
  {
    return root.getPath();
  }

  /**
   * Returns `true` when the [root] is not empty.
   */
  public boolean isRooted()
  {
    return !root.getPath().isEmpty();
  }

  /**
   * Returns the number of elements in the path to the file.
   */
  public int size()
  {
    return segments.size();
  }

  /**
   * Returns a sub-path of the path, starting with the directory at the specified [beginIndex] and up
   * to the specified [endIndex].
   */
  public File subPath( int beginIndex, int endIndex )
  {
    if( beginIndex < 0 || beginIndex > endIndex || endIndex > size() )
    {
      throw new IllegalArgumentException();
    }

    return new File( segments.subList( beginIndex, endIndex ).joinToString( File.separator ) );
  }

  public FilePathComponents normalize()
  {
    return new FilePathComponents( root, ManFileExt.normalize( segments ) );
  }

  @Override
  public boolean equals( Object o )
  {
    if( this == o )
    {
      return true;
    }
    if( o == null || getClass() != o.getClass() )
    {
      return false;
    }

    FilePathComponents that = (FilePathComponents)o;

    if( !root.equals( that.root ) )
    {
      return false;
    }
    return segments.equals( that.segments );
  }

  @Override
  public int hashCode()
  {
    int result = root.hashCode();
    result = 31 * result + segments.hashCode();
    return result;
  }
}
