package msql;

import manifold.api.fs.IFile;
import manifold.api.type.AbstractSingleFileModel;

import java.util.Set;

class SQLModel extends AbstractSingleFileModel {

    public SQLModel(String fqn, Set<IFile> files) {
        super(fqn, files);
    }

}
