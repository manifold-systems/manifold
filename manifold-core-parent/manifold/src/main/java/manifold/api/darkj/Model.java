package manifold.api.darkj;

import com.sun.tools.javac.tree.JCTree;
import java.net.MalformedURLException;
import java.util.Set;
import manifold.api.fs.IFile;
import manifold.api.host.IManifoldHost;
import manifold.api.type.AbstractSingleFileModel;

/**
 */
class Model extends AbstractSingleFileModel
{
  private String _url;
  private JCTree.JCClassDecl _classDecl;

  Model( IManifoldHost host, String fqn, Set<IFile> files )
  {
    super( host, fqn, files );
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

  public JCTree.JCClassDecl getClassDecl()
  {
    return _classDecl;
  }
  public void setClassDecl( JCTree.JCClassDecl classDecl )
  {
    _classDecl = classDecl;
  }

  @Override
  public void updateFile( IFile file )
  {
    super.updateFile( file );
    _classDecl = null;
    assignUrl();
  }
}
