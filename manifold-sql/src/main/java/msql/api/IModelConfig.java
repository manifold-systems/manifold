package msql.api;

import java.util.List;
import java.util.Map;
import java.util.Set;

public interface IModelConfig
{
  void setTableName(String name);
  String getTableName();

  void setIdColumn(String config);
  String getIdColumn();

  <T> void addValidation(Enum propertyReference, IFieldValidator<T> validator);
  <T> void validateFormat(Enum propertyReference, String regexp);
  <T> void requiredFields(Enum propertyReferences);
  <T> void lengthBetween(Enum propertyReference, int minlength, int maxlength);
  <T> void unique(Enum propertyReference);
  <T> void hasContent(Enum propertyReference);
  <T> void isInSet(Enum propertyReference, List<Object> objs);
  <T> void isInSet(Enum propertyReference, Set<Object> objs);

  void clearValidators();
  Map<String, List<String>> getErrorsList();

  boolean isValidModifyingErrors(ISQLRecord sqlRecord);
  boolean isValid(ISQLRecord sqlRecord);
}
