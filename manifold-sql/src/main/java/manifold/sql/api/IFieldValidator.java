package manifold.sql.api;

public interface IFieldValidator<T>
{
  /**
   * @throws Exception - an exception if the value is not valid
   *
   * @param value to validate
   */
  void validateValue(T value);
}
