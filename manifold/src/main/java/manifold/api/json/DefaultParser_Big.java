package manifold.api.json;

import javax.script.Bindings;
import javax.script.ScriptException;

public class DefaultParser_Big extends DefaultParser {
  private static final DefaultParser_Big INSTANCE = new DefaultParser_Big();

  public static IJsonParser instance()
  {
    return INSTANCE;
  }

  @Override
  public Bindings parseJson( String jsonText ) throws ScriptException
  {
    return super.parseJson( jsonText, true );
  }
}
