package msql;

import manifold.api.fs.IFile;
import manifold.api.sourceprod.AbstractSingleFileModel;
import manifold.api.sourceprod.IModel;

import java.util.Set;

class SQLModel extends AbstractSingleFileModel {

    public SQLModel(String fqn, Set<IFile> files) {
        super(fqn, files);
    }

}
