package msql;

import manifold.api.sourceprod.JavaSourceProducer;

import javax.tools.DiagnosticListener;
import javax.tools.JavaFileObject;

public class SQLSourceProducer extends JavaSourceProducer<SQLModel> {

    @Override
    public boolean handlesFileExtension(String fileExtension) {
        return fileExtension.equals("ddl");
    }

    @Override
    protected boolean isInnerType(String topLevelFqn, String relativeInner) {
        return false;
    }

    @Override
    protected String produce(String topLevelFqn, String existing, SQLModel model, DiagnosticListener<JavaFileObject> errorHandler) {
        new DDLCodeGenerator(topLevelFqn, existing, model, errorHandler);
        return "";
    }

}
