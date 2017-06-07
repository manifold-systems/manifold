package manifoldjs.plugin.legacy;

import manifold.api.fs.IFile;
import gw.lang.reflect.IType;
import gw.lang.reflect.ITypeLoader;
import gw.lang.reflect.TypeBase;
import gw.util.GosuExceptionUtil;
import gw.util.StreamUtil;

import java.io.IOException;
import java.io.InputStreamReader;

abstract public class JavascriptTypeBase extends TypeBase implements IType
{
  private String _name;
  private JavascriptPlugin _typeloader;
  private String _relativeName;
  private String _package;
  private String _src;
  private IFile _file;

  public JavascriptTypeBase( JavascriptPlugin typeloader, String name, IFile jsFile )
  {
    _name = name;
    _typeloader = typeloader;
    _file = jsFile;
    if( _name.indexOf( '.' ) > 0 )
    {
      _relativeName = _name.substring( _name.lastIndexOf( '.' ) + 1 );
      _package = _name.substring( 0, _name.lastIndexOf( '.' ) );
    }
    else
    {
      _relativeName = _name;
      _package = "";
    }
    if( _name.indexOf( '.' ) > 0 )
    {
      _relativeName = _name.substring( _name.lastIndexOf( '.' ) + 1 );
      _package = _name.substring( 0, _name.lastIndexOf( '.' ) );
    }
    else
    {
      _relativeName = _name;
      _package = "";
    }
    try
    {
      _src = StreamUtil.getContent( new InputStreamReader( jsFile.openInputStream() ) );
    }
    catch( IOException e )
    {
      throw new RuntimeException( e );
    }

  }

  public String getSource()
  {
    return _src;
  }

  @Override
  public String getName()
  {
    return _name;
  }

  @Override
  public String getRelativeName()
  {
    return _relativeName;
  }

  @Override
  public String getNamespace()
  {
    return _package;
  }

  @Override
  public ITypeLoader getTypeLoader()
  {
    return _typeloader;
  }

  @Override
  public IType getSupertype()
  {
    return null;
  }

  @Override
  public IType[] getInterfaces()
  {
    return new IType[0];
  }

  @Override
  public IFile[] getSourceFiles()
  {
    return new IFile[]{_file};
  }
}
