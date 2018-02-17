package manifold.js;

import java.util.Objects;
import javax.tools.Diagnostic;
import javax.tools.DiagnosticListener;
import javax.tools.JavaFileObject;
import manifold.api.fs.IFile;
import manifold.api.gen.SrcClass;
import manifold.internal.javac.SourceJavaFileObject;
import manifold.js.parser.Parser;
import manifold.js.parser.TemplateParser;
import manifold.js.parser.TemplateTokenizer;
import manifold.js.parser.Token;
import manifold.js.parser.Tokenizer;
import manifold.js.parser.tree.ParseError;
import manifold.js.parser.tree.ProgramNode;
import manifold.js.parser.tree.template.JSTNode;
import manifold.util.JavacDiagnostic;


class JavascriptCodeGen
{

  private final IFile _file;
  private final String _fqn;

  JavascriptCodeGen( IFile file, String topLevelFqn )
  {
    _file = file;
    _fqn = topLevelFqn;
  }

  SrcClass make( DiagnosticListener<JavaFileObject> errorHandler )
  {
    if( Objects.equals( _file.getExtension(), "jst" ) )
    {
      TemplateParser parser = new TemplateParser( new TemplateTokenizer( _file, true ) );
      return JavascriptTemplate.genClass( _fqn, (JSTNode)parser.parse() );
    }
    else
    {
      Parser parser = new Parser( new Tokenizer( _file ) );
      ProgramNode programNode = (ProgramNode)parser.parse();

      reportErrors( errorHandler, programNode );

      if( parser.isES6Class() )
      {
        return JavascriptClass.genClass( _fqn, programNode );
      }
      else
      {
        return JavascriptProgram.genProgram( _fqn, programNode );
      }
    }
  }

  private void reportErrors( DiagnosticListener<JavaFileObject> errorHandler, ProgramNode programNode )
  {
    if( programNode.errorCount() > 0 )
    {
      JavaFileObject file;
      try
      {
        file = new SourceJavaFileObject( _file.toURI() );
      }
      catch( Exception e )
      {
        file = null;
      }

      for( ParseError error : programNode.getErrorList() )
      {
        Token token = error.getToken();
        errorHandler.report( new JavacDiagnostic( file, Diagnostic.Kind.ERROR, token.getOffset(), token.getLineNumber(), token.getCol(), error.getMessage() ) );
      }
    }
  }

}
