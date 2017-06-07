package manifold.api.image;

import java.net.MalformedURLException;
import java.util.Collections;
import java.util.List;
import manifold.api.fs.IFile;
import manifold.api.sourceprod.ResourceFileSourceProducer;

/**
 */
class Model implements ResourceFileSourceProducer.IModel
{
  private String _fqn;
  private IFile _file;
  String _url;

  Model( String fqn, IFile file )
  {
    _fqn = fqn;
    _file = file;
    try
    {
      _url = file.toURI().toURL().toString();
    }
    catch( MalformedURLException e )
    {
      throw new RuntimeException( e );
    }
  }

  @Override
  public String getFqn()
  {
    return _fqn;
  }

  @Override
  public List<IFile> getFiles()
  {
    return Collections.singletonList( _file );
  }

  public String getUrl()
  {
    return _url;
  }
}
