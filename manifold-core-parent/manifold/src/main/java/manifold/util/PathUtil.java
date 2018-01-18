package manifold.util;

import com.sun.tools.javac.api.JavacTool;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.Charset;
import java.nio.file.CopyOption;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.FileSystem;
import java.nio.file.FileSystemNotFoundException;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.OpenOption;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.FileAttribute;
import java.nio.file.attribute.PosixFilePermission;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;
import manifold.api.fs.FileFactory;
import manifold.api.fs.IFile;

/**
 */
public class PathUtil
{
  public static boolean mkdirs( Path path )
  {
    try
    {
      Files.createDirectories( path );
      return true;
    }
    catch( Exception e )
    {
      return false;
    }
  }

  public static boolean mkdir( Path copy, FileAttribute... attrs )
  {
    try
    {
      return Files.createDirectory( copy, attrs ) != null;
    }
    catch( FileAlreadyExistsException faee )
    {
      return false;
    }
    catch( IOException e )
    {
      throw new RuntimeException( e );
    }
  }

  public static String getName( Path path )
  {
    return path.getFileName().toString();
  }

  public static boolean isFile( Path path, LinkOption... options )
  {
    return Files.isRegularFile( path, options );
  }

  public static boolean isDirectory( Path fileOrDir, LinkOption... options )
  {
    return Files.isDirectory( fileOrDir, options );
  }

  public static Path create( String first, String... more )
  {
    Path path = Paths.get( first, more );
    if( !path.isAbsolute() )
    {
      // Use the "user.dir" system property because we set this property to the experiment root.
      // Note there is no way to set the current working directory at the OS level in Java, so we
      // must use something like this.

      path = resolveRelativePath( first, more );
    }
    return path;
  }

  public static Path create( Path root, String createMe )
  {
    if( !root.isAbsolute() )
    {
      return resolveRelativePath( root.toString(), createMe );
    }
    return root.resolve( createMe );
  }

  public static Path create( URI uri )
  {
    try
    {
      return Paths.get( uri );
    }
    catch( FileSystemNotFoundException nfe )
    {
      try
      {
        Map<String, String> env = new HashMap<>();
        env.put( "create", "true" ); // creates zip/jar file if not already exists
        FileSystem fs = FileSystems.newFileSystem( uri, env );
        return fs.provider().getPath( uri );
      }
      catch( IOException e )
      {
        throw new RuntimeException( e );
      }
    }
  }

  public static String findToolsJar()
  {
    String javaHome = System.getProperty( "java.home" );
    String toolsJar = javaHome + File.separator + "lib" + File.separator + "tools.jar";
    if( !PathUtil.isFile( PathUtil.create( toolsJar ) ) )
    {
      try
      {
        URI toolsJarUri = JavacTool.class.getProtectionDomain().getCodeSource().getLocation().toURI();
        toolsJar = new File( toolsJarUri ).getAbsolutePath();
      }
      catch( URISyntaxException e )
      {
        System.out.println( "Could not find tools.jar" );
      }
    }
    return toolsJar;
  }

  interface IOConsumer<T>
  {
    void accept( T t ) throws IOException;
  }

  private static Path resolveRelativePath( String first, String... more )
  {
    String workingDirName = System.getProperty( "user.dir" );
    if( workingDirName == null )
    {
      throw new IllegalStateException( "Working directory yet defined" );
    }
    Path workingDir = Paths.get( workingDirName );
    Path path = workingDir.resolve( first );
    if( more != null )
    {
      for( String part : more )
      {
        path = path.resolve( part );
      }
    }
    return path;
  }

  public static String getAbsolutePathName( Path path )
  {
    return path.toAbsolutePath().toString();
  }

  public static String getAbsolutePathName( String path )
  {
    return create( path ).toAbsolutePath().toString();
  }

  public static Path getAbsolutePath( Path path )
  {
    return path.toAbsolutePath();
  }

  public static Path getAbsolutePath( String path )
  {
    return create( path ).toAbsolutePath();
  }

  public static Path[] listFiles( Path path )
  {
    try( Stream<Path> list = Files.list( path ) )
    {
      return list.toArray( Path[]::new );
    }
    catch( IOException e )
    {
      throw new RuntimeException( e );
    }
  }

  public static boolean createNewFile( Path file, FileAttribute... attrs )
  {
    try
    {
      return Files.createFile( file, attrs ) != null;
    }
    catch( FileAlreadyExistsException faee )
    {
      return false;
    }
    catch( IOException e )
    {
      throw new RuntimeException( e );
    }
  }

  public static BufferedReader createReader( Path file )
  {
    try
    {
      return Files.newBufferedReader( file, Charset.forName( "UTF-8" ) );
    }
    catch( IOException e )
    {
      throw new RuntimeException( e );
    }
  }

  public static BufferedWriter createWriter( Path path )
  {
    try
    {
      return Files.newBufferedWriter( path, Charset.forName( "UTF-8" ) );
    }
    catch( IOException e )
    {
      throw new RuntimeException( e );
    }
  }

  public static OutputStream createOutputStream( Path path, OpenOption... options )
  {
    try
    {
      return Files.newOutputStream( path, options );
    }
    catch( IOException e )
    {
      throw new RuntimeException( e );
    }
  }

  public static InputStream createInputStream( Path path, OpenOption... options )
  {
    try
    {
      return Files.newInputStream( path, options );
    }
    catch( IOException e )
    {
      throw new RuntimeException( e );
    }
  }

  public static IFile getIFile( Path classFile )
  {
    return FileFactory.instance().getIFile( classFile.toUri() );
  }

  public static void delete( Path path )
  {
    delete( path, false );
  }

  public static void delete( Path path, boolean bRecursive )
  {
    if( bRecursive && isDirectory( path ) )
    {
      for( Path child : listFiles( path ) )
      {
        delete( child, bRecursive );
      }
    }

    try
    {
      Files.delete( path );
    }
    catch( IOException e )
    {
      throw new RuntimeException( e );
    }
  }

  public static boolean exists( Path path, LinkOption... options )
  {
    return Files.exists( path, options );
  }

  public static long lastModified( Path path, LinkOption... options )
  {
    try
    {
      return Files.getLastModifiedTime( path, options ).toMillis();
    }
    catch( IOException e )
    {
      throw new RuntimeException( e );
    }
  }

  public static boolean renameTo( Path from, Path to, CopyOption... options )
  {
    try
    {
      return Files.move( from, to, options ) != null;
    }
    catch( IOException e )
    {
      return false;
    }
  }

  public static boolean canWrite( Path path )
  {
    if( !isFile( path ) )
    {
      return false;
    }

    try
    {
      //## todo: find a better way, Files.isWritable() does not work e.g., returns true for ZipPath, which is wrong
      return path.toFile().canWrite();
    }
    catch( UnsupportedOperationException e )
    {
      return false;
    }
  }

  public static boolean setWritable( Path file, boolean bWritable )
  {
    try
    {
      Set<PosixFilePermission> perms = Files.getPosixFilePermissions( file );
      if( bWritable )
      {
        perms.add( PosixFilePermission.OWNER_WRITE );
      }
      else
      {
        perms.remove( PosixFilePermission.OWNER_WRITE );
      }
      return Files.setPosixFilePermissions( file, perms ) != null;
    }
    catch( UnsupportedOperationException uoe )
    {
      try
      {
        return file.toFile().setWritable( bWritable );
      }
      catch( Exception e )
      {
        return false;
      }
    }
    catch( Exception e )
    {
      return false;
    }
  }
}
