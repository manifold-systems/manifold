package manifold.js;

import javax.tools.Diagnostic;
import javax.tools.DiagnosticListener;
import javax.tools.JavaFileObject;
import manifold.api.fs.IFile;
import manifold.api.gen.*;
import manifold.internal.javac.SourceJavaFileObject;
import manifold.js.parser.Parser;
import manifold.js.parser.TemplateParser;
import manifold.js.parser.TemplateTokenizer;
import manifold.js.parser.Tokenizer;
import manifold.js.parser.tree.ParseError;
import manifold.js.parser.tree.ProgramNode;
import manifold.js.parser.tree.template.JSTNode;

import java.util.Objects;
import manifold.util.JavacDiagnostic;


import static manifold.js.Util.safe;

public class JavascriptCodeGen {

    private final IFile _file;
    private final String _fqn;

    public JavascriptCodeGen(IFile file, String topLevelFqn) {
        _file = file;
        _fqn = topLevelFqn;
    }

    public SrcClass make( DiagnosticListener<JavaFileObject> errorHandler ) {
        if (Objects.equals(_file.getExtension(), "jst")) {
            TemplateParser parser = new TemplateParser(new TemplateTokenizer(Util.loadContent(safe(_file::openInputStream)), true));
            return JavascriptTemplate.genClass(_fqn, (JSTNode) parser.parse());
        } else {
            Parser parser = new Parser(new Tokenizer(Util.loadContent(safe(_file::openInputStream))));
            ProgramNode programNode = (ProgramNode) parser.parse();

          reportErrors( errorHandler, programNode );

          if (parser.isES6Class()) {
                return JavascriptClass.genClass(_fqn, programNode);
            } else {
                return JavascriptProgram.genProgram(_fqn, programNode);
            }
        }
    }

  private void reportErrors( DiagnosticListener<JavaFileObject> errorHandler, ProgramNode programNode )
  {
    if (programNode.errorCount() > 0) {
      JavaFileObject file = new SourceJavaFileObject( _file.toURI() );
      for( ParseError error: programNode.getErrorList() ) {
        Tokenizer.Token token = error.getToken();
        errorHandler.report( new JavacDiagnostic( file, Diagnostic.Kind.ERROR, token.getOffset(), token.getLineNumber(), token.getCol(), error.getMessage() ) );
      }
    }
  }

}
