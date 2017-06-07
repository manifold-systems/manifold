package manifold.api.json;

import java.io.StringReader;
import java.util.List;
import javax.script.Bindings;
import javax.script.ScriptException;

public class DefaultParser implements IJsonParser {
  private static final DefaultParser INSTANCE = new DefaultParser();

  public static IJsonParser instance()
  {
    return INSTANCE;
  }

  @Override
  public Bindings parseJson( String jsonText ) throws ScriptException
  {
    return parseJson( jsonText, false );
  }
  public Bindings parseJson( String jsonText, boolean big ) throws ScriptException
  {
    SimpleParserImpl parser = new SimpleParserImpl( new Tokenizer(new StringReader(jsonText)), big );
    Object result = parser.parse();
    List<String> errors = parser.getErrors();
    if(errors.size() != 0) {
      StringBuilder sb = new StringBuilder("Found errors:\n");
      for(String err : errors) {
        sb.append(err).append("\n");
      }
      throw new ScriptException(sb.toString());
    }
    if(result instanceof Bindings) {
      return (Bindings)result;
    }
    return NashornJsonParser.wrapValueInBindings( result );
  }
}
