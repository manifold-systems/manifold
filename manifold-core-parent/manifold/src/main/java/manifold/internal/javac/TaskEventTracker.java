package manifold.internal.javac;

import com.sun.source.tree.ExpressionTree;
import com.sun.source.util.TaskEvent;
import com.sun.tools.javac.tree.JCTree;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class TaskEventTracker
{
  private final Map<TaskEvent.Kind, Map<Boolean, Set<String>>> cache = new HashMap<>();

  public boolean alreadyProcessed( TaskEvent e, boolean start, JCTree.JCClassDecl classDecl )
  {
    ExpressionTree packageName = e.getCompilationUnit().getPackageName();
    String pkg = packageName == null ? "" : packageName.toString();
    String fqn = pkg.isEmpty() ? classDecl.name.toString() : (pkg + "." + classDecl.name);
    return !cache.computeIfAbsent( e.getKind(), __ -> new HashMap<>() )
      .computeIfAbsent( start, __ -> new HashSet<>() )
      .add( fqn );
  }
}
