package manifold.api.json;

import java.util.List;
import javax.script.Bindings;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;
import javax.script.SimpleBindings;

/**
 */
public class NashornJsonParser implements IJsonParser
{
  private static final NashornJsonParser INSTANCE = new NashornJsonParser();

  public static IJsonParser instance()
  {
    return INSTANCE;
  }

  private ScriptEngine _engine;

  private NashornJsonParser()
  {
  }

  @Override
  public Bindings parseJson( String jsonText, boolean withBigNumbers, boolean withTokens ) throws ScriptException
  {
    if( withBigNumbers || withTokens )
    {
      throw new UnsupportedOperationException( "Nashorn json parser does not support Big numbers or tokens" );
    }

    if( _engine == null )
    {
      _engine = new ScriptEngineManager().getEngineByName( "javascript" );
    }

    String script = "Java.asJSONCompatible(" + jsonText + ")";
    Object result = _engine.eval( script );
    if( result instanceof Bindings )
    {
      return (Bindings)result;
    }
    return wrapValueInBindings( result );
  }

  static Bindings wrapValueInBindings( Object result ) throws ScriptException
  {
    if( result == null ||
        result instanceof List ||
        result instanceof String ||
        result instanceof Number ||
        result instanceof Boolean )
    {
      Bindings wrapper = new SimpleBindings();
      wrapper.put( "value", result );
      return wrapper;
    }
    throw new ScriptException( "Unexpected JSON result type: " + result.getClass().getName() );
  }
}
