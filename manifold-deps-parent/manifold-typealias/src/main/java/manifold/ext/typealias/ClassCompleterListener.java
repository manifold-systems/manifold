package manifold.ext.typealias;

import com.sun.tools.javac.code.Symbol;


public class ClassCompleterListener implements Symbol.Completer {

  @Override
  public void complete(Symbol symbol) throws Symbol.CompletionFailure {
    System.out.println(symbol);
  }
}
