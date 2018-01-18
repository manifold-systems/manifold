package manifold.api.json.schema;

/**
 */
public enum Type
{
  Object( "object" ),
  Array( "array" ),
  String( "string" ),
  Number( "number" ),
  Integer( "integer" ),
  Boolean( "boolean" ),
  Dynamic( "dynamic" ),
  Null( "null" ),
  Invalid( "invalid" );

  private final String _schemaName;

  Type( String name )
  {
    _schemaName = name;
  }

  public String getName()
  {
    return _schemaName;
  }

  public static Type fromName( String schemaName )
  {
    switch( schemaName )
    {
      case "object":
        return Object;
      case "array":
        return Array;
      case "string":
        return String;
      case "number":
        return Number;
      case "integer":
        return Integer;
      case "boolean":
        return Boolean;
      case "dynmaic":
        return Dynamic;
      case "null":
        return Null;
    }
    return Invalid;
  }
}
