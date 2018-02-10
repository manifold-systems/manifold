package manifold.templates.manifold;

import manifold.api.fs.IFile;
import manifold.api.host.ITypeLoader;
import manifold.api.type.JavaTypeManifold;

import javax.tools.DiagnosticListener;
import javax.tools.JavaFileObject;

public class TemplateManifold extends JavaTypeManifold<TemplateModel> {

    public void init( ITypeLoader typeLoader )
    {
        init(typeLoader, TemplateModel::new);
    }

    @Override
    protected String aliasFqn(String fqn, IFile file) {
        String extention;
        if (file.getBaseName().split("\\.").length == 2) {
            extention = "_" + file.getBaseName().split("\\.")[1];
        } else {
            extention = null;
        }
        return (extention != null && fqn.endsWith(extention)) ? fqn.substring(0, fqn.indexOf(extention)) : fqn;
    }

    @Override
    public boolean isInnerType( String topLevelFqn, String relativeInner ) {
        return true;
    }

    @Override
    public boolean handlesFileExtension(String fileExtension) {
        return fileExtension.equals("mtf");
    }

    @Override
    public boolean handlesFile(IFile file) {
        return file.getExtension().equals("mtf");
    }

    @Override
    protected String produce(String topLevelFqn, String existing, TemplateModel model, DiagnosticListener<JavaFileObject> errorHandler) {
        String source = model.getSource();
        model.report( errorHandler );
        return source;
    }

}
