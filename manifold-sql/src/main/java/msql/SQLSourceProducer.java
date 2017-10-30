package msql;


import manifold.api.type.JavaTypeManifold;

import javax.tools.DiagnosticListener;
import javax.tools.JavaFileObject;

public class SQLSourceProducer extends JavaTypeManifold<SQLModel> {

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
