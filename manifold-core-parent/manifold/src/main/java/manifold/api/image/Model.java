package manifold.api.image;

import java.net.MalformedURLException;
import java.util.Set;
import manifold.api.fs.IFile;
import manifold.api.type.AbstractSingleFileModel;

/**
 */
class Model extends AbstractSingleFileModel
{
  String _url;

  Model( String fqn, Set<IFile> files )
  {
    super( fqn, files );
    assignUrl();
  }

  private void assignUrl()
  {
    try
    {
      _url = getFile().toURI().toURL().toString();
    }
    catch( MalformedURLException e )
    {
      throw new RuntimeException( e );
    }
  }

  public String getUrl()
  {
    return _url;
  }

  @Override
  public void updateFile( IFile file )
  {
    super.updateFile( file );
    assignUrl();
  }
}
