package manifold.internal.javac;

import com.sun.source.tree.CompilationUnitTree;
import com.sun.source.util.DocTrees;
import com.sun.source.util.SourcePositions;
import java.util.Collection;
import java.util.List;
import java.util.function.Consumer;
import javax.tools.DiagnosticCollector;
import javax.tools.DiagnosticListener;
import javax.tools.JavaFileObject;
import manifold.util.Pair;

/**
 */
public interface IJavaParser
{
  boolean parseText( String src, List<CompilationUnitTree> trees, Consumer<SourcePositions> sourcePositions, Consumer<DocTrees> docTrees, DiagnosticCollector<JavaFileObject> errorHandler );
  boolean parseType( String fqn, List<CompilationUnitTree> trees, DiagnosticCollector<JavaFileObject> errorHandler );

  InMemoryClassJavaFileObject compile( JavaFileObject jfo, String fqn, Iterable<String> options, DiagnosticCollector<JavaFileObject> errorHandler );
  Collection<InMemoryClassJavaFileObject> compile( Collection<JavaFileObject> jfo, Iterable<String> options, DiagnosticCollector<JavaFileObject> errorHandler );
  InMemoryClassJavaFileObject compile( String fqn, Iterable<String> options, DiagnosticCollector<JavaFileObject> errorHandler );

  Pair<JavaFileObject, String> findJavaSource( String fqn, DiagnosticListener<JavaFileObject> errorHandler );

  void clear();
}
