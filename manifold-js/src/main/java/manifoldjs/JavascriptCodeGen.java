package manifoldjs;

import manifold.api.fs.IFile;
import manifold.api.gen.*;
import manifoldjs.parser.Parser;
import manifoldjs.parser.TemplateParser;
import manifoldjs.parser.TemplateTokenizer;
import manifoldjs.parser.Tokenizer;
import manifoldjs.parser.tree.FunctionNode;
import manifoldjs.parser.tree.ParameterNode;
import manifoldjs.parser.tree.ProgramNode;
import manifoldjs.parser.tree.template.JSTNode;

import javax.script.ScriptEngine;
import java.lang.reflect.Modifier;
import java.util.Objects;

import static manifoldjs.Util.safe;

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
