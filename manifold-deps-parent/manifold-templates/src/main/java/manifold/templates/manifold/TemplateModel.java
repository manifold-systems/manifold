package manifold.templates.manifold;

import manifold.api.type.AbstractSingleFileModel;
import manifold.templates.codegen.TemplateGen;
import manifold.api.fs.IFile;
import manifold.internal.javac.IIssue;
import manifold.internal.javac.SourceJavaFileObject;
import manifold.util.JavacDiagnostic;
import manifold.util.StreamUtil;

import javax.tools.Diagnostic;
import javax.tools.DiagnosticListener;
import javax.tools.JavaFileObject;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Set;

class TemplateModel extends AbstractSingleFileModel  {
    private String _source;
    private TemplateIssueContainer _issues;

    TemplateModel(String fqn, Set<IFile> files) {
        super(fqn, files);
        init();
    }

    private void init() {
        IFile file = getFile();
        try {
            String templateSource = StreamUtil.getContent(new InputStreamReader(file.openInputStream()));
            TemplateGen generator = new TemplateGen();
            _source = generator.generateCode(getFqn(), templateSource, file.getName());
            _issues = generator.getIssues();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

    }

    @Override
    public void updateFile(IFile file) {
        super.updateFile(file);
        init();
    }

    String getSource() {
        return _source;
    }
    void report(DiagnosticListener errorHandler) {
        if (_issues.isEmpty() || errorHandler == null) {
            return;
        }

        JavaFileObject file = new SourceJavaFileObject(getFile().toURI());
        for (IIssue issue : _issues.getIssues()) {
            Diagnostic.Kind kind = issue.getKind() == IIssue.Kind.Error ? Diagnostic.Kind.ERROR : Diagnostic.Kind.WARNING;
            errorHandler.report(new JavacDiagnostic(file, kind, issue.getStartOffset(), issue.getLine(), issue.getColumn(), issue.getMessage()));
        }
    }

}
