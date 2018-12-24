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

package manifold.io.extensions.java.io.File;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.FileSystemException;
import java.nio.file.NoSuchFileException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.BiFunction;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import manifold.ext.api.Extension;
import manifold.ext.api.This;
import manifold.io.FilePathComponents;
import manifold.io.FileTreeWalk;

/**
 * A collection of useful extensions to java.io.File.
 * Partially adapted from kotlin.io.files.Utils.kt
 */
@Extension
public class ManFileExt
{
  public final static int DEFAULT_BUFFER_SIZE = 8192;

  /**
   * Creates an empty directory in the specified {@code directory}, using the given {@code prefix} and {@code suffix} to generate its name.
   * <p>
   * If {@code prefix} is not specified then some unspecified name will be used.
   * If {@code suffix} is not specified then ".tmp" will be used.
   * If {@code directory} is not specified then the default temporary-file directory will be used.
   *
   * @return a file object corresponding to a newly-created directory.
   *
   * @throws IOException              in case of input/output error.
   * @throws IllegalArgumentException if {@code } is shorter than three symbols.
   */
  @Extension
  public static File createTempDir( String prefix, String suffix, File directory ) throws IOException
  {
    File dir = File.createTempFile( prefix, suffix, directory );
    //noinspection ResultOfMethodCallIgnored
    dir.delete();
    if( dir.mkdir() )
    {
      return dir;
    }
    else
    {
      throw new IOException( "Unable to create temporary directory " + dir );
    }
  }
  /**
   * Same as {@code createTempDir("tmp", null, null)}
   * <p>
   * @see #createTempDir(String, String, File)
   */
  @Extension
  public static File createTempDir() throws IOException
  {
    return createTempDir( "tmp", null, null );
  }

  /**
   * Returns the extension of this file (not including the dot), or an empty string if it doesn't have one.
   */
  public static String getExtension( @This File thiz )
  {
    String ext = thiz.getName().substringAfterLast( '.' );
    return ext == null ? "" : ext;
  }

  /**
   * Returns {@code path} of this File using the invariant separator '/' to
   * separate the names in the name sequence.
   */
  public static String slashPath( @This File thiz )
  {
    return File.separatorChar != '/' ? thiz.getPath().replace( File.separatorChar, '/' ) : thiz.getPath();
  }

  /**
   * Returns file's name without an extension.
   */
  public static String nameWithoutExtension( @This File thiz )
  {
    String name = thiz.getName().substringBeforeLast( "." );
    return name == null ? "" : name;
  }

  /**
   * Calculates the relative path for this file from {@code base} file.
   * Note that the {@code base} file is treated as a directory.
   * If this file matches the {@code base} file, then an empty string will be returned.
   *
   * @return relative path from {@code base} to this.
   *
   * @throws IllegalArgumentException if this and base paths have different roots.
   */
  public static String toRelativeString( @This File thiz, File base )
  {
    String rel = toRelativeStringOrNull( thiz, base );
    if( rel == null )
    {
      throw new IllegalArgumentException( "this and base files have different roots: " + thiz + " and " + base );
    }
    return rel;
  }

  /**
   * Calculates the relative path for this file from {@code base} file.
   * Note that the {@code base} file is treated as a directory.
   * If this file matches the {@code base} file, then a {@code File} with empty path will be returned.
   *
   * @return File with relative path from {@code base} to this.
   *
   * @throws IllegalArgumentException if this and base paths have different roots.
   */
  public static File relativeTo( @This File thiz, File base )
  {
    return new File( toRelativeString( thiz, base ) );
  }

  /**
   * Calculates the relative path for this file from {@code base} file.
   * Note that the {@code base} file is treated as a directory.
   * If this file matches the {@code base} file, then a {@code File} with empty path will be returned.
   *
   * @return File with relative path from {@code base} to this, or {@code this} if this and base paths have different roots.
   */
  public static File relativeToOrSelf( @This File thiz, File base )
  {
    String rel = toRelativeStringOrNull( thiz, base );
    return rel == null ? thiz : new File( rel );
  }

  /**
   * Calculates the relative path for this file from {@code base} file.
   * Note that the {@code base} file is treated as a directory.
   * If this file matches the {@code base} file, then a {@code File} with empty path will be returned.
   *
   * @return File with relative path from {@code base} to this, or {@code null} if this and base paths have different roots.
   */
  public static File relativeToOrNull( @This File thiz, File base )
  {
    String rel = toRelativeStringOrNull( thiz, base );
    return rel == null ? null : new File( rel );
  }

  private static String toRelativeStringOrNull( File thiz, File base )
  {
    // Check roots
    FilePathComponents thisComponents = thiz.toComponents().normalize();
    FilePathComponents baseComponents = base.toComponents().normalize();
    if( thisComponents.root != baseComponents.root )
    {
      return null;
    }

    int baseCount = baseComponents.size();
    int thisCount = thisComponents.size();

    int sameCount = getSameCount( thisComponents, baseComponents, baseCount, thisCount );
    // Annihilate differing base components by adding required number of .. parts
    StringBuilder res = new StringBuilder();
    for( int i = baseCount - 1; i >= sameCount; i-- )
    {
      if( baseComponents.segments.get( i ).getName().equals( ".." ) )
      {
        return null;
      }

      res.append( ".." );

      if( i != sameCount )
      {
        res.append( File.separatorChar );
      }
    }

    // Add remaining this components
    if( sameCount < thisCount )
    {
      // If some .. were appended
      if( sameCount < baseCount )
      {
        res.append( File.separatorChar );
      }

      thisComponents.segments.subList( sameCount ).joinTo( res, File.separator );
    }

    return res.toString();
  }

  private static int getSameCount( FilePathComponents thisComponents, FilePathComponents baseComponents, int baseCount, int thisCount )
  {
    int i = 0;
    int maxSameCount = Math.min( thisCount, baseCount );
    while( i < maxSameCount && thisComponents.segments.get( i ).equals( baseComponents.segments.get( i ) ) )
    {
      i++;
    }
    return i;
  }

  /**
   * Splits the file into path components (the names of containing directories and the name of the file
   * itself) and returns the resulting collection of components.
   */
  public static FilePathComponents toComponents( @This File thiz )
  {
    String path = thiz.getPath();
    int rootLength = getRootLength( path );
    String rootName = path.substring( 0, rootLength );
    String subPath = path.substring( rootLength );
    List<File> list = subPath.isEmpty() ? new ArrayList<>() : Arrays.stream( subPath.split( File.separator ) ).map( File::new ).collect( Collectors.toList() );
    return new FilePathComponents( new File( rootName ), list );
  }

  /**
   * Estimation of a root name by a given file name.
   * <p>
   * This implementation is able to find /, Drive:/, Drive: or
   * //network.name/root as possible root names.
   * / denotes File.separator here so \ can be used instead.
   * All other possible roots cannot be identified by this implementation.
   * It's also not guaranteed (but possible) that function will be able to detect a root
   * which is incorrect for current OS. For instance, in Unix function cannot detect
   * network root names like //network.name/root, but can detect Windows roots like C:/.
   *
   * @return length or a substring representing the root for this path, or zero if this file name is relative.
   */
  private static int getRootLength( String path )
  {
    // Note: separators should be already replaced to system ones
    int first = path.indexOf( File.separatorChar, 0 );
    if( first == 0 )
    {
      if( path.length() > 1 && path.charAt( 1 ) == File.separatorChar )
      {
        // Network names like //my.host/home/something ? => //my.host/home/ should be root
        // NB: does not work in Unix because //my.host/home is converted into /my.host/home there
        // So in Windows we'll have root of //my.host/home but in Unix just /
        first = path.indexOf( File.separatorChar, 2 );
        if( first >= 0 )
        {
          first = path.indexOf( File.separatorChar, first + 1 );
          if( first >= 0 )
          {
            return first + 1;
          }
          else
          {
            return path.length();
          }
        }
      }
      return 1;
    }
    // C:\
    if( first > 0 && path.charAt( first - 1 ) == ':' )
    {
      first++;
      return first;
    }
    // C:
    if( first == -1 && path.endsWith( ":" ) )
    {
      return path.length();
    }
    return 0;
  }

  /**
   * Estimation of a root name for this file.
   * <p>
   * This implementation is able to find /, Drive:/, Drive: or
   * //network.name/root as possible root names.
   * / denotes File.separator here so \ can be used instead.
   * All other possible roots cannot be identified by this implementation.
   * It's also not guaranteed (but possible) that function will be able to detect a root
   * which is incorrect for current OS. For instance, in Unix function cannot detect
   * network root names like //network.name/root, but can detect Windows roots like C:/.
   *
   * @return string representing the root for this file, or empty string is this file name is relative.
   */
  static String rootName( File thiz )
  {
    return thiz.getPath().substring( 0, getRootLength( thiz.getPath() ) );
  }

  /**
   * Returns root component of this abstract name, like / from /home/user, or C:\ from C:\file.tmp,
   * or //my.host/home for //my.host/home/user
   */
  static File root( File thiz )
  {
    return new File( rootName( thiz ) );
  }

  /**
   * Determines whether this file has a root or it represents a relative path.
   * <p>
   * Returns {@code true} when this file has non-empty root.
   */
  public static boolean isRooted( @This File thiz )
  {
    return getRootLength( thiz.getPath() ) > 0;
  }

  /**
   * Copies this file to the given {@code target} file.
   * <p>
   * If some directories on a way to the {@code target} are missing, they will be created.
   * If the {@code target} file already exists, this function will fail unless {@code overwrite} argument is set to {@code true}.
   * <p>
   * When {@code overwrite} is {@code true} and {@code target} is a directory, it is replaced only if it is empty.
   * <p>
   * If this file is a directory, it is copied without its content, i.e. an empty {@code target} directory is created.
   * If you want to copy directory including its contents, use {@code copyRecursively}.
   * <p>
   * The operation doesn't preserve copied file attributes such as creation/modification date, permissions, etc.
   *
   * @param overwrite  {@code true} if destination overwrite is allowed.
   * @param bufferSize the buffer size to use when copying.
   *
   * @return the {@code target} file.
   *
   * @throws NoSuchFileException        if the source file doesn't exist.
   * @throws FileAlreadyExistsException if the destination file already exists and 'rewrite' argument is set to {@code false}.
   * @throws IOException                if any errors occur while copying.
   */
  public static File copyTo( @This File thiz, File target, boolean overwrite, int bufferSize )
  {
    if( !thiz.exists() )
    {
      throw new RuntimeException( new NoSuchFileException( thiz.toString(), null, "The source file doesn't exist." ) );
    }

    if( target.exists() )
    {
      boolean stillExists = !overwrite || !target.delete();

      if( stillExists )
      {
        throw new RuntimeException( new FileAlreadyExistsException( thiz.toString(), target.toString(), "The destination file already exists." ) );
      }
    }

    if( thiz.isDirectory() )
    {
      if( !target.mkdirs() )
      {
        throw new RuntimeException( new FileSystemException( thiz.toString(), target.toString(), "Failed to create target directory." ) );
      }
    }
    else
    {
      File parentFile = target.getParentFile();
      if( parentFile != null )
      {
        //noinspection ResultOfMethodCallIgnored
        parentFile.mkdirs();
      }

      try( InputStream input = thiz.inputStream();
           OutputStream output = target.outputStream() )
      {
        input.copyTo( output, bufferSize );
      }
      catch( IOException e )
      {
        throw new RuntimeException( e );
      }
    }

    return target;
  }
  /**
   * Same as {@code copyTo(File, File, false, #DEFAULT_BUFFER_SIZE)}
   * <p>
   * @see #copyTo(File, File, boolean, int) 
   */
  public static File copyTo( @This File thiz, File target )
  {
    return thiz.copyTo( target, false, DEFAULT_BUFFER_SIZE );
  }

  /**
   * Enum that can be used to specify behaviour of the `copyRecursively()` function
   * in exceptional conditions.
   */
  public enum OnErrorAction
  {
    /**
     * Skip this file and go to the next.
     */
    SKIP,

    /**
     * Terminate the evaluation of the function.
     */
    TERMINATE
  }

  /**
   * Private exception class, used to terminate recursive copying.
   */
  private static class TerminateException extends FileSystemException
  {
    TerminateException( String file )
    {
      super( file );
    }
  }

  /**
   * Copies this file with all its children to the specified destination {@code target} path.
   * If some directories on the way to the destination are missing, they will be created.
   * <p>
   * If this file path points to a single file, it will be copied to a file with the path {@code target}.
   * If this file path points to a directory, its children will be copied to a directory with the path {@code target}.
   * <p>
   * If the {@code target} already exists, it will be deleted before copying when the {@code overwrite} parameter permits so.
   * <p>
   * The operation doesn't preserve copied file attributes such as creation/modification date, permissions, etc.
   * <p>
   * If any errors occur during the copying, further actions will depend on the result of the call
   * to `onError(File, IOException)` function, that will be called with arguments,
   * specifying the file that caused the error and the exception itself.
   * By default this function rethrows exceptions.
   * <p>
   * Exceptions that can be passed to the {@code onError} function:
   * <p>
   * - NoSuchFileException - if there was an attempt to copy a non-existent file
   * - FileAlreadyExistsException - if there is a conflict
   * - AccessDeniedException - if there was an attempt to open a directory that didn't succeed.
   * - IOException - if some problems occur when copying.
   * <p>
   * Note that if this function fails, partial copying may have taken place.
   *
   * @return {@code false} if the copying was terminated, {@code true} otherwise.
   */
  public static boolean copyRecursively( @This File thiz, File target, boolean overwrite,
                                         BiFunction<File, IOException, OnErrorAction> onError,
                                         Predicate<File> filter)
  {
    if( !thiz.exists() )
    {
      return OnErrorAction.TERMINATE != onError.apply( thiz, new NoSuchFileException( thiz.toString(), null, "The source file doesn't exist." ) );
    }

    // We cannot break for loop from inside a lambda, so we have to use an exception here
    for( File src : walkTopDown( thiz ).onFail( ( f, e ) ->
                                                {
                                                  if( onError.apply( f, e ) == OnErrorAction.TERMINATE )
                                                  {
                                                    throw new RuntimeException( new TerminateException( f.toString() ) );
                                                  }
                                                } ) )
    {
      if (!filter.test(src)) {
        continue;
      }
      if( !src.exists() )
      {
        if( OnErrorAction.TERMINATE == onError.apply( src, new NoSuchFileException( src.toString(), null, "The source file doesn't exist." ) ) )
        {
          return false;
        }
      }
      else
      {
        String relPath = src.toRelativeString( thiz );
        File dstFile = new File( target, relPath );
        if( dstFile.exists() && !(src.isDirectory() && dstFile.isDirectory()) )
        {
          boolean stillExists;
          if( !overwrite )
          {
            stillExists = true;
          }
          else
          {
            if( dstFile.isDirectory() )
            {
              stillExists = !dstFile.deleteRecursively();
            }
            else
            {
              stillExists = !dstFile.delete();
            }
          }

          if( stillExists )
          {
            if( OnErrorAction.TERMINATE == onError.apply( dstFile, new FileAlreadyExistsException( src.toString(), dstFile.toString(), "The destination file already exists." ) ) )
            {
              return false;
            }

            continue;
          }
        }

        if( src.isDirectory() )
        {
          //noinspection ResultOfMethodCallIgnored
          dstFile.mkdirs();
        }
        else
        {
          if( src.copyTo( dstFile, overwrite, DEFAULT_BUFFER_SIZE ).length() != src.length() )
          {
            if( OnErrorAction.TERMINATE == onError.apply( src, new IOException( "Source file wasn't copied completely, length of destination file differs." ) ) )
            {
              return false;
            }
          }
        }
      }
    }
    return true;
  }
  /**
   * @see #copyRecursively(File, File, boolean, BiFunction, Predicate)
   */
  public static boolean copyRecursively( @This File thiz, File target )
  {
    return copyRecursively( thiz, target, false, ( t, u ) ->
    {
      throw new RuntimeException( u );
    } );
  }
  /**
   * @see #copyRecursively(File, File, boolean, BiFunction, Predicate)
   */
  public static boolean copyRecursively( @This File thiz, File target, Predicate<File> filter )
  {
    return copyRecursively( thiz, target, false, ( t, u ) ->
    {
      throw new RuntimeException( u );
    }, filter);
  }
  /**
   * @see #copyRecursively(File, File, boolean, BiFunction, Predicate) 
   */
  public static boolean copyRecursively( @This File thiz, File target, boolean overwrite, BiFunction<File, IOException, OnErrorAction> onError ) {
    return copyRecursively(thiz, target, overwrite, onError, file -> true);
  }
  
  /**
   * Gets an iterable for visiting this directory and all its content.
   *
   * @param direction walk direction, top-down (by default) or bottom-up.
   */
  public static FileTreeWalk walk( @This File thiz, FileTreeWalk.FileWalkDirection direction )
  {
    return new FileTreeWalk( thiz, direction );
  }

  /**
   * Gets a sequence for visiting this directory and all its content in top-down order.
   * Depth-first search is used and directories are visited before all their files.
   */
  public static FileTreeWalk walkTopDown( @This File thiz )
  {
    return walk( thiz, FileTreeWalk.FileWalkDirection.TOP_DOWN );
  }

  /**
   * Gets a sequence for visiting this directory and all its content in bottom-up order.
   * Depth-first search is used and directories are visited after all their files.
   */
  public static FileTreeWalk walkBottomUp( @This File thiz )
  {
    return walk( thiz, FileTreeWalk.FileWalkDirection.BOTTOM_UP );
  }

  /**
   * Delete this file with all its children.
   * Note that if this operation fails then partial deletion may have taken place.
   *
   * @return {@code true} if the file or directory is successfully deleted, {@code false} otherwise.
   */
  public static boolean deleteRecursively( @This File thiz )
  {
    return thiz
      .walkBottomUp()
      .fold( true, ( res, it ) -> (it.delete() || !it.exists()) && res );
  }

  /**
   * Determines whether this file belongs to the same root as {@code other}
   * and starts with all components of {@code other} in the same order.
   * So if {@code other} has N components, first N components of {@code this} must be the same as in {@code other}.
   *
   * @return {@code true} if this path starts with {@code other} path, {@code false} otherwise.
   */
  public static boolean startsWith( @This File thiz, File other )
  {
    FilePathComponents components = thiz.toComponents();
    FilePathComponents otherComponents = other.toComponents();
    if( components.root != otherComponents.root )
    {
      return false;
    }
    return components.size() >= otherComponents.size() &&
           components.segments.subList( 0, otherComponents.size() ).equals( otherComponents.segments );
  }

  /**
   * Determines whether this file belongs to the same root as {@code other}
   * and starts with all components of {@code other} in the same order.
   * So if {@code other} has N components, first N components of {@code this} must be the same as in {@code other}.
   *
   * @return {@code true} if this path starts with {@code other} path, {@code false} otherwise.
   */
  public static boolean startsWith( @This File thiz, String other )
  {
    return thiz.startsWith( new File( other ) );
  }

  /**
   * Determines whether this file path ends with the path of {@code other} file.
   * <p>
   * If {@code other} is rooted path it must be equal to this.
   * If {@code other} is relative path then last N components of {@code this} must be the same as all components in {@code other},
   * where N is the number of components in {@code other}.
   *
   * @return {@code true} if this path ends with {@code other} path, {@code false} otherwise.
   */
  public static boolean endsWith( @This File thiz, File other )
  {
    FilePathComponents components = thiz.toComponents();
    FilePathComponents otherComponents = other.toComponents();
    if( otherComponents.isRooted() )
    {
      return thiz.equals( other );
    }
    int shift = components.size() - otherComponents.size();
    return shift >= 0 &&
           components.segments.subList( shift, components.size() ).equals( otherComponents.segments );
  }

  /**
   * Determines whether this file belongs to the same root as {@code other}
   * and ends with all components of {@code other} in the same order.
   * So if {@code other} has N components, last N components of {@code this} must be the same as in {@code other}.
   * For relative {@code other}, {@code this} can belong to any root.
   *
   * @return {@code true} if this path ends with {@code other} path, {@code false} otherwise.
   */
  public static boolean endsWith( @This File thiz, String other )
  {
    return thiz.endsWith( new File( other ) );
  }

  /**
   * Removes all . and resolves all possible .. in this file name.
   * For instance, `File("/foo/./bar/gav/../baaz").normalize()` is `File("/foo/bar/baaz")`.
   *
   * @return normalized pathname with . and possibly .. removed.
   */
  public static File normalize( @This File thiz )
  {
    FilePathComponents comps = toComponents( thiz );
    return comps.root.resolve( normalize( comps.segments ).joinToString( File.separator ) );
  }

  public static List<File> normalize( List<File> segments )
  {
    List<File> list = new ArrayList<>( segments.size() );
    for( File file : segments )
    {
      switch( file.getName() )
      {
        case ".":
          break;
        case "..":
          if( !list.isEmpty() && !list.last().getName().equals( ".." ) )
          {
            list.remove( list.size() - 1 );
          }
          else
          {
            list.add( file );
          }
          break;
        default:
          list.add( file );
      }
    }
    return list;
  }

  /**
   * Adds {@code relative} file to this, considering this as a directory.
   * If {@code relative} has a root, {@code relative} is returned back.
   * For instance, `File("/foo/bar").resolve(File("gav"))` is `File("/foo/bar/gav")`.
   * This function is complementary with {@code relativeTo},
   * so `f.resolve(g.relativeTo(f)) == g` should be always {@code true} except for different roots case.
   *
   * @return concatenated this and {@code relative} paths, or just {@code relative} if it's absolute.
   */
  public static File resolve( @This File thiz, File relative )
  {
    if( relative.isRooted() )
    {
      return relative;
    }
    String baseName = thiz.toString();
    return baseName.isEmpty() || baseName.endsWith( File.separator )
           ? new File( baseName + relative )
           : new File( baseName + File.separatorChar + relative );
  }

  /**
   * Adds {@code relative} name to this, considering this as a directory.
   * If {@code relative} has a root, {@code relative} is returned back.
   * For instance, `File("/foo/bar").resolve("gav")` is `File("/foo/bar/gav")`.
   *
   * @return concatenated this and {@code relative} paths, or just {@code relative} if it's absolute.
   */
  public static File resolve( @This File thiz, String relative )
  {
    return thiz.resolve( new File( relative ) );
  }

  /**
   * Adds {@code relative} file to this parent directory.
   * If {@code relative} has a root or this has no parent directory, {@code relative} is returned back.
   * For instance, `File("/foo/bar").resolveSibling(File("gav"))` is `File("/foo/gav")`.
   *
   * @return concatenated this.parent and {@code relative} paths, or just {@code relative} if it's absolute or this has no parent.
   */
  public static File resolveSibling( @This File thiz, File relative )
  {
    FilePathComponents components = thiz.toComponents();
    File parentSubPath = components.size() == 0
                         ? new File( ".." )
                         : components.subPath( 0, components.size() - 1 );
    return components.root.resolve( parentSubPath ).resolve( relative );
  }

  /**
   * Adds {@code relative} name to this parent directory.
   * If {@code relative} has a root or this has no parent directory, {@code relative} is returned back.
   * For instance, `File("/foo/bar").resolveSibling("gav")` is `File("/foo/gav")`.
   *
   * @return concatenated this.parent and {@code relative} paths, or just {@code relative} if it's absolute or this has no parent.
   */
  public static File resolveSibling( @This File thiz, String relative )
  {
    return thiz.resolveSibling( new File( relative ) );
  }
}
