package manifold.api.json;

import javax.script.Bindings;
import javax.script.ScriptException;

/**
 */
public interface IJsonParser
{
  /**
   * Parse Json text as a standard javax.script.Bindings object.
   *
   * @param jsonText Any Json text, can be an object, a list, or simple value.
   * @param withBigNumbers Parse decimal numbers as BigDecimals and integers and BigIntegers,
   *                       otherwise they are Double and Integer.
   * @param withTokens Store tokens for Json name value pairs.  The token contains positional
   *                   information for tooling e.g., to facilitate navigation in an IDE.  This
   *                   parameter should be false for normal use-cases.
   * @return A standard javax.script.Bindings object of the Json text.  If the Json is List or simple value, it is wrapped in a Bindings.
   * @throws ScriptException
   */
  Bindings parseJson( String jsonText, boolean withBigNumbers, boolean withTokens ) throws ScriptException;

  static IJsonParser getDefaultParser()
  {
    return DefaultParser.instance();
    //return NashornJsonParser.instance();
  }
}
