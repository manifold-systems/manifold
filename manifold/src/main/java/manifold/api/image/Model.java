package manifold.api.image;

import java.net.MalformedURLException;
import java.util.Set;
import manifold.api.fs.IFile;
import manifold.api.sourceprod.AbstractSingleFileModel;

/**
 */
class Model extends AbstractSingleFileModel
{
  String _url;

  Model( String fqn, Set<IFile> files )
  {
    super( fqn, files );
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
}
