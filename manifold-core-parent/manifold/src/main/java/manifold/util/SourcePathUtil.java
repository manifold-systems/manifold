package manifold.util;

import java.io.File;

/**
 */
public class SourcePathUtil
{
  public static boolean excludeFromSourcePath( String p )
  {
    warnIfRoot( p );
    String path = p.replace( File.separatorChar, '/' ).toLowerCase();
    return isJrePath( path ) ||
           path.contains( "/idea_rt.jar" );
  }

  public static boolean excludeFromTestPath( String p )
  {
    warnIfRoot( p );
    String path = p.replace( File.separatorChar, '/' ).toLowerCase();
    return
      // necessary since java 9
      // (surefile creates a classpath jar where the classpaths are formatted as URLs e.g., file://c:/blah, which blows up java 9)
      // since we don't need this jar anyway, we omit it from the path
      path.contains( "/surefire/" );
  }

  private static boolean isJrePath( String path )
  {
    if( path.endsWith( "tools.jar" ) )
    {
      return true;
    }

    String extDirs = System.getProperty( "java.ext.dirs" );
    if( extDirs != null && extDirs.contains( path ) )
    {
      return true;
    }

    String bootPath = System.getProperty( "sun.boot.class.path" );
    if( bootPath != null && bootPath.contains( path ) )
    {
      return true;
    }

    String javaHome = System.getProperty( "java.home" );
    if( javaHome == null )
    {
      return false;
    }

    return path.startsWith( javaHome );
  }

  private static void warnIfRoot( String p )
  {
    for( File root : File.listRoots() )
    {
      if( new File( p ).equals( root ) )
      {
        System.out.println( "!!!" );
        System.out.println( "WARNING: Root file " + p + " is in the Manifold classpath" );
        System.out.println( "!!!" );
      }
    }
  }
}
