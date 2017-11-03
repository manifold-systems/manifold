package manifold.sql.api;

public interface ISQLMetadata {
    String getColumnForProperty(IColumnEnum column);
    String getTableName();
    String getTableForType(Class rootType);
}
