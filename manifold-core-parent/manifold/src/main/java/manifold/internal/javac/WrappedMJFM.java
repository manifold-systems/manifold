package manifold.internal.javac;

import java.io.IOException;
import java.util.Set;
import javax.tools.ForwardingJavaFileManager;
import javax.tools.JavaFileManager;
import javax.tools.JavaFileObject;

public class WrappedMJFM extends ForwardingJavaFileManager
{
  private final JavaFileManager _plainFm;

  /**
   * Creates a new instance of ForwardingJavaFileManager.
   *
   * @param fileManager delegate to this file manager
   */
  protected WrappedMJFM( JavaFileManager fileManager, ManifoldJavaFileManager mfm )
  {
    super( mfm );
    _plainFm = fileManager;
  }

  @Override
  public Iterable<JavaFileObject> list( Location location, String packageName, Set set, boolean recurse ) throws IOException
  {
    Iterable list = _plainFm.list( location, packageName, set, recurse );
    if( !list.iterator().hasNext() )
    {
      list = super.list( location, packageName, set, recurse );
    }
    return list;
  }
}
