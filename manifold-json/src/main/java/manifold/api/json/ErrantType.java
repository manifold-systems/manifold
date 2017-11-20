package manifold.api.json;

import java.net.URL;

/**
 */
public class ErrantType extends JsonStructureType
{
  private int _offset;

  public ErrantType( URL file, String errantTypeName )
  {
    super( null, file, errantTypeName );
  }

  @Override
  public String getIdentifier()
  {
    return "Object";
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
