package manifold.api.fs;

public class IFileUtil
{

  public static String getExtension( IFile file )
  {
    int lastDot = file.getName().lastIndexOf( "." );
    if( lastDot >= 0 )
    {
      return file.getName().substring( lastDot + 1 );
    }
    else
    {
      return "";
    }
  }

  public static String getExtension( String fileName )
  {
    int lastDot = fileName.lastIndexOf( "." );
    if( lastDot >= 0 )
    {
      return fileName.substring( lastDot + 1 );
    }
    else
    {
      return "";
    }
  }

  public static String getBaseName( IFile file )
  {
    return getBaseName( file.getName() );
  }

  public static String getBaseName( String fileName )
  {
    int lastDot = fileName.lastIndexOf( "." );
    if( lastDot >= 0 )
    {
      return fileName.substring( 0, lastDot );
    }
    else
    {
      return "";
    }
  }

  /**
   * Avoid including dependency jar files that are not meant to be scanned for source files
   */
  public static boolean hasSourceFiles( IDirectory root )
  {
    if( !root.exists() )
    {
      return false;
    }

    return !Extensions.containsManifest( root ) ||
           !Extensions.getExtensions( root, Extensions.CONTAINS_SOURCES ).isEmpty() ||

           // this check is too aggressive eg., including jars like nashorn.jar
           //!Extensions.getExtensions( root, "Main-Class" ).isEmpty() ||

           // Weblogic packages all WEB-INF/classes content into this JAR
           // http://middlewaremagic.com/weblogic/?p=408
           // http://www.coderanch.com/t/69641/BEA-Weblogic/wl-cls-gen-jar-coming
           // So we need to always treat it as containing sources
           root.getName().equals( "_wl_cls_gen.jar" );
  }
}
