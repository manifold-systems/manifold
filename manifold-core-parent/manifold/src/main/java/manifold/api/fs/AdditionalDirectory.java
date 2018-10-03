package manifold.api.fs;

import java.util.ArrayList;
import java.util.List;

/**
 */
public class AdditionalDirectory extends DelegateDirectory
{
  public AdditionalDirectory( IFileSystem fs, IDirectory delegate )
  {
    super( fs, delegate );
  }

  @Override
  public List<? extends IDirectory> listDirs()
  {
    List<IDirectory> result = new ArrayList<IDirectory>();
    for( IDirectory dir : getDelegate().listDirs() )
    {
      result.add( new AdditionalDirectory( getFileSystem(), dir ) );
    }
    return result;
  }

  @Override
  public boolean isAdditional()
  {
    return true;
  }
}
