package manifold.internal.javac;

import com.sun.tools.javac.tree.JCTree;
import com.sun.tools.javac.tree.TreeScanner;
import com.sun.tools.javac.util.JCDiagnostic;
import com.sun.tools.javac.util.ListBuffer;
import com.sun.tools.javac.util.Log;

import java.util.EnumSet;
import java.util.Queue;
import java.util.Set;
import java.util.function.Predicate;

public class DeferredAttrDiagHandler_8
{
  public static class DeferredDiagnosticHandler extends Log.DiagnosticHandler
  {
    private Queue<JCDiagnostic> deferred = new ListBuffer<>();
    private final Predicate<JCDiagnostic> filter;

    public DeferredDiagnosticHandler(Log log) {
      this(log, null);
    }

    public DeferredDiagnosticHandler(Log log, Predicate<JCDiagnostic> filter) {
      this.filter = filter;
      install(log);
    }

    @Override
    public void report(JCDiagnostic diag) {
      if (!diag.isFlagSet(JCDiagnostic.DiagnosticFlag.NON_DEFERRABLE) &&
          (filter == null || filter.test(diag))) {
        deferred.add(diag);
      } else {
        prev.report(diag);
      }
    }

    public Queue<JCDiagnostic> getDiagnostics() {
      return deferred;
    }

    /** Report all deferred diagnostics. */
    public void reportDeferredDiagnostics() {
      reportDeferredDiagnostics( EnumSet.allOf( JCDiagnostic.Kind.class));
    }

    /** Report selected deferred diagnostics. */
    public void reportDeferredDiagnostics( Set<JCDiagnostic.Kind> kinds) {
      JCDiagnostic d;
      while ((d = deferred.poll()) != null) {
        if (kinds.contains(d.getKind()))
          prev.report(d);
      }
      deferred = null; // prevent accidental ongoing use
    }
  }

  public static class PosScanner extends TreeScanner
  {
    JCDiagnostic.DiagnosticPosition pos;
    boolean found = false;

    PosScanner( JCDiagnostic.DiagnosticPosition pos )
    {
      this.pos = pos;
    }

    @Override
    public void scan( JCTree tree )
    {
      if( tree != null &&
          tree.pos() == pos )
      {
        found = true;
      }
      super.scan( tree );
    }
  }

  public static class DeferredAttrDiagHandler extends DeferredDiagnosticHandler implements IDeferredAttrDiagHandler
  {
    public DeferredAttrDiagHandler( Log log, JCTree newTree )
    {
      super( log, d -> {
        PosScanner posScanner = new PosScanner( d.getDiagnosticPosition() );
        posScanner.scan( newTree );
        return posScanner.found;
      } );
    }
  }
}