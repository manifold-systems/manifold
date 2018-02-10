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
