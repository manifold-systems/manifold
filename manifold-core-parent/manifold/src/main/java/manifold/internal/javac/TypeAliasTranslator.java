package manifold.internal.javac;

import com.sun.tools.javac.code.Symbol;
import com.sun.tools.javac.tree.JCTree;
import com.sun.tools.javac.util.Name;

public class TypeAliasTranslator extends JCTree.Visitor {

  public static Importer IMPORTER;
  public static Transformer TRANSFORMER;

  public interface Transformer {
    JCTree transform(JCTree tree);
  }

  public interface Importer {
    void accept(Name name, Symbol.ClassSymbol symbol);
  }
}
