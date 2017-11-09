package manifold.api.json;

import java.net.URL;
import manifold.api.fs.IFile;

/**
 */
public class ErrantType extends JsonStructureType
{
  private int _offset;

  public ErrantType( URL file, String errantTypeName )
  {
    super( null, errantTypeName );
    setFile( file );
  }

  public int getOffset()
  {
    return _offset;
  }
  public void setPosition( int offset )
  {
    _offset = offset;
  }
}
