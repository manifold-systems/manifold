package manifold.js;

import manifold.api.fs.IFile;
import manifold.api.gen.*;
import manifold.js.parser.Parser;
import manifold.js.parser.TemplateParser;
import manifold.js.parser.TemplateTokenizer;
import manifold.js.parser.Tokenizer;
import manifold.js.parser.tree.ProgramNode;
import manifold.js.parser.tree.template.JSTNode;

import java.util.Objects;

import static manifold.js.Util.safe;

public class JavascriptCodeGen {

    private final IFile _file;
    private final String _fqn;

    public JavascriptCodeGen(IFile file, String topLevelFqn) {
        _file = file;
        _fqn = topLevelFqn;
    }

    public SrcClass make() {
        if (Objects.equals(_file.getExtension(), "jst")) {
            TemplateParser parser = new TemplateParser(new TemplateTokenizer(Util.loadContent(safe(_file::openInputStream)), true));
            return JavascriptTemplate.genClass(_fqn, (JSTNode) parser.parse());
        } else {
            Parser parser = new Parser(new Tokenizer(Util.loadContent(safe(_file::openInputStream))));
            ProgramNode programNode = (ProgramNode) parser.parse();

            if (programNode.errorCount() > 0) {
                programNode.printErrors();
            }

            if (parser.isES6Class()) {
                return JavascriptClass.genClass(_fqn, programNode);
            } else {
                return JavascriptProgram.genProgram(_fqn, programNode);
            }
        }
    }

}
