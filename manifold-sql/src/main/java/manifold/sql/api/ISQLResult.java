package manifold.sql.api;

public interface ISQLResult
{
  Object getRawValue(String property);

  void setRawValue(String property, Object value);

  String getTableName();
}
