package manifoldjs;

import manifold.util.StreamUtil;

import javax.script.Bindings;
import javax.script.ScriptContext;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.concurrent.Callable;

public class Util
{
  public static <T> T safe(Callable<T> elt) {
    try {
      return elt.call();
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  static String loadContent(InputStream in) {
    return safe(() -> StreamUtil.getContent(new InputStreamReader(in)));
  }
}
