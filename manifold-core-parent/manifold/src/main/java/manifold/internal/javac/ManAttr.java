package manifold.internal.javac;

import com.sun.tools.javac.tree.JCTree;

public interface ManAttr
{
  boolean JAILBREAK_PRIVATE_FROM_SUPERS = true;

  JCTree.JCFieldAccess peekSelect();
  JCTree.JCAnnotatedType peekAnnotatedType();
}
