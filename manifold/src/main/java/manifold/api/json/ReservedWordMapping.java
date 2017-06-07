package manifold.api.json;

import java.util.HashMap;
import java.util.Map;
import manifold.internal.host.ManifoldHost;

/**
 */
public class ReservedWordMapping
{
  private static final Map<String, String> RESERVED_WORD_TO_IDENTIFIER = new HashMap<>();

  static
  {
    // Capitalize/Uncapitalize reserved keywords to avoid parse errors.
    // Internally we perserve the case of the keys, but in structure types
    // we expose them as alternate versions of the reserved words.

    for( String kw : ManifoldHost.getAllReservedWords() )
    {
      String identifier;
      char c = kw.charAt( 0 );
      if( Character.isLowerCase( c ) )
      {
        identifier = Character.toUpperCase( c ) + kw.substring( 1 );
      }
      else
      {
        identifier = Character.toLowerCase( c ) + kw.substring( 1 );
      }
      RESERVED_WORD_TO_IDENTIFIER.put( kw, identifier );
    }
  }

  public static String getIdentifierForName( String key )
  {
    return RESERVED_WORD_TO_IDENTIFIER.getOrDefault( key, key );
  }
}
