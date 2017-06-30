package manifold.api.fs.physical.fast;

import java.io.File;
import java.util.HashSet;
import java.util.Set;
import manifold.api.fs.physical.IFileMetadata;

public class FastFileMetadata implements IFileMetadata
{
  private File _file;

  public FastFileMetadata( File file )
  {
    _file = file;
  }

  @Override
  public String name()
  {
    return _file.getName();
  }

  @Override
  public long length()
  {
    return _file.length();
  }

  @Override
  public long lastModifiedTime()
  {
    return _file.lastModified();
  }

  @Override
  public boolean exists()
  {
    return _file.exists();
  }

  @Override
  public boolean isFile()
  {
    return !isDir();
  }

  @Override
  public boolean isDir()
  {
    String name = _file.getName();
    if( isAssumedFileSuffix( getFileSuffix( name ) ) )
    {
      return false;
    }
    else
    {
      return _file.isDirectory();
    }
  }

  private static String getFileSuffix( String name )
  {
    int dotIndex = name.lastIndexOf( '.' );
    if( dotIndex == -1 )
    {
      return null;
    }
    else
    {
      return name.substring( dotIndex + 1 );
    }
  }

  private static final Set<String> FILE_SUFFIXES;

  static
  {
    FILE_SUFFIXES = new HashSet<String>();
    FILE_SUFFIXES.add( "class" );
    FILE_SUFFIXES.add( "eti" );
    FILE_SUFFIXES.add( "etx" );
    FILE_SUFFIXES.add( "gif" );
    FILE_SUFFIXES.add( "gr" );
    FILE_SUFFIXES.add( "grs" );
    FILE_SUFFIXES.add( "gs" );
    FILE_SUFFIXES.add( "gst" );
    FILE_SUFFIXES.add( "gsx" );
    FILE_SUFFIXES.add( "gti" );
    FILE_SUFFIXES.add( "gx" );
    FILE_SUFFIXES.add( "jar" );
    FILE_SUFFIXES.add( "java" );
    FILE_SUFFIXES.add( "pcf" );
    FILE_SUFFIXES.add( "png" );
    FILE_SUFFIXES.add( "properties" );
    FILE_SUFFIXES.add( "tti" );
    FILE_SUFFIXES.add( "ttx" );
    FILE_SUFFIXES.add( "txt" );
    FILE_SUFFIXES.add( "wsdl" );
    FILE_SUFFIXES.add( "xml" );
    FILE_SUFFIXES.add( "xsd" );
  }

  private static boolean isAssumedFileSuffix( String suffix )
  {
    return FILE_SUFFIXES.contains( suffix );
  }
}
