package manifold.internal.javac;

import com.sun.tools.javac.code.Symbol;
import com.sun.tools.javac.code.Symtab;
import com.sun.tools.javac.code.Type;
import com.sun.tools.javac.model.JavacElements;
import com.sun.tools.javac.tree.JCTree;
import com.sun.tools.javac.util.Context;
import com.sun.tools.javac.util.Filter;
import com.sun.tools.javac.util.JCDiagnostic;
import com.sun.tools.javac.util.List;
import com.sun.tools.javac.util.Log;
import com.sun.tools.javac.util.Name;
import com.sun.tools.javac.util.Names;
import java.util.Locale;
import javax.tools.Diagnostic;
import javax.tools.JavaFileObject;


public class Java8DynamicJdk implements IDynamicJdk
{
  @Override
  public <T> void report( Log issueLogger, Diagnostic<? extends T> diagnostic )
  {
    // Adapted from JavacMessager.printMessage.  Following same basic routine regarding use of Log

    JavaFileObject oldSource = issueLogger.useSource( (JavaFileObject)diagnostic.getSource() );
    boolean oldMultipleErrors = issueLogger.multipleErrors;
    issueLogger.multipleErrors = true;
    try
    {
      switch( diagnostic.getKind() )
      {
        case ERROR:
          issueLogger.error( new IssueReporter.Position( diagnostic ), "proc.messager", diagnostic.getMessage( Locale.getDefault() ) );
          break;
        case WARNING:
          issueLogger.warning( new IssueReporter.Position( diagnostic ), "proc.messager", diagnostic.getMessage( Locale.getDefault() ) );
          break;
        case MANDATORY_WARNING:
          issueLogger.mandatoryWarning( new IssueReporter.Position( diagnostic ), "proc.messager", diagnostic.getMessage( Locale.getDefault() ) );
          break;
        case NOTE:
        case OTHER:
          issueLogger.note( new IssueReporter.Position( diagnostic ), "proc.messager", diagnostic.getMessage( Locale.getDefault() ) );
          break;
      }
    }
    finally
    {
      issueLogger.useSource( oldSource );
      issueLogger.multipleErrors = oldMultipleErrors;
    }
  }

  @Override
  public Iterable<Symbol> getMembers( Symbol.ClassSymbol classSym )
  {
    return classSym.members().getElements();
  }

  @Override
  public Iterable<Symbol> getMembers( Symbol.ClassSymbol classSym, Filter<Symbol> filter )
  {
    return classSym.members().getElements( filter );
  }

  @Override
  public Iterable<Symbol> getMembersByName( Symbol.ClassSymbol classSym, Name call )
  {
    return classSym.members().getElementsByName( call );
  }

  @Override
  public Symbol.ClassSymbol getTypeElement( Context ctx, Object ignore, String fqn )
  {
    return JavacElements.instance( ctx ).getTypeElement( fqn );
  }

  @Override
  public Symbol.ClassSymbol getLoadedClass( Context ctx, String fqn )
  {
    Name name = Names.instance( ctx ).fromString( fqn );
    return Symtab.instance( ctx ).classes.get( name );
  }

  @Override
  public List<Type> getTargets( JCTree.JCLambda tree )
  {
    return tree.targets;
  }
  @Override
  public void setTargets( JCTree.JCLambda tree, List<Type> targets )
  {
    tree.targets = targets;
  }

  @Override
  public void logError( Log logger, JCDiagnostic.DiagnosticPosition pos, String key, Object... args )
  {
    logger.error( pos, key, args );
  }
}
