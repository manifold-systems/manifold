package manifold.api.json.schema;

import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.Stack;
import manifold.api.json.IJsonType;
import manifold.util.Pair;

/**
 * Manages a cache of base types per URL for a given Json parser/transformer session.  Note
 * a given session is single-threaded, therefore there is one instance of this class per thread
 * and per session accessible via instance().
 */
public class JsonSchemaTransformerSession
{
  private static final ThreadLocal<JsonSchemaTransformerSession> INSTANCE = new ThreadLocal<>();

  private Map<URL, Pair<IJsonType, JsonSchemaTransformer>> _baseTypeByUrl;
  private Stack<JsonSchemaTransformer> _transformers;

  public static JsonSchemaTransformerSession instance()
  {
    JsonSchemaTransformerSession instance = INSTANCE.get();
    if( instance == null )
    {
      INSTANCE.set( instance = new JsonSchemaTransformerSession() );
    }
    return instance;
  }

  private JsonSchemaTransformerSession()
  {
    _baseTypeByUrl = new HashMap<>();
    _transformers = new Stack<>();
  }

  void pushTransformer( JsonSchemaTransformer transformer )
  {
    _transformers.push( transformer );
  }
  void popTransformer( JsonSchemaTransformer transformer )
  {
    if( _transformers.peek() != transformer )
    {
      throw new IllegalStateException( "Unbalanced transformer pop" );
    }
    _transformers.pop();
  }

  Pair<IJsonType, JsonSchemaTransformer> getCachedBaseType( URL url )
  {
    return _baseTypeByUrl.get( url );
  }
  void cacheBaseType( URL url, Pair<IJsonType, JsonSchemaTransformer> pair )
  {
    _baseTypeByUrl.put( url, pair );
  }

  public void maybeClear()
  {
    if( _transformers.size() == 0 )
    {
      _baseTypeByUrl.clear();
    }
  }
}
