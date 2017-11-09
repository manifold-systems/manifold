package manifold.api.json.schema;

import manifold.api.json.Token;

/**
 */
public class IllegalSchemaTypeName extends IllegalArgumentException
{
  private final String _typeName;
  private Token _token;

  IllegalSchemaTypeName( String typeName, Token token )
  {
    super( typeName );
    _typeName = typeName;
    _token = token;
  }

  public String getTypeName()
  {
    return _typeName;
  }

  public Token getToken()
  {
    return _token;
  }
}
