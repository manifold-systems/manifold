package manifoldjs;

import java.net.MalformedURLException;
import java.util.Set;
import manifold.api.fs.IFile;
import manifold.api.sourceprod.AbstractSingleFileModel;

/**
 * Created by carson on 5/10/17.
 */
public class JavascriptModel extends AbstractSingleFileModel
{
  private String _url;

  JavascriptModel( String fqn, Set<IFile> files )
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

