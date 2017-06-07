package manifold.api.json;

import javax.script.Bindings;
import javax.script.ScriptException;

/**
 */
public interface IJsonParser
{
  Bindings parseJson( String jsonText ) throws ScriptException;

  static IJsonParser getDefaultParser()
  {
    return DefaultParser.instance();
    //return NashornJsonParser.instance();
  }
}
