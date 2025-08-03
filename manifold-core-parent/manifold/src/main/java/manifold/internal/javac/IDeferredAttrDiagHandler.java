package manifold.internal.javac;

import com.sun.tools.javac.util.JCDiagnostic;

import java.util.Collection;

public interface IDeferredAttrDiagHandler
{
  Collection<JCDiagnostic> getDiagnostics();
  void reportDeferredDiagnostics();
}
