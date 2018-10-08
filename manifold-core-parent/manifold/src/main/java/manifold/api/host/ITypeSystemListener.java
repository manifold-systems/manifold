package manifold.api.host;

public interface ITypeSystemListener
{
  /**
   * Fired when an existing type is refreshed, i.e. there are potential changes
   */
  void refreshedTypes( RefreshRequest request );

  /**
   * Fired when the type system is fully refreshed
   */
  void refreshed();

  /**
   * Return true to hint you need to listen before other listeners, no guarantee of order.
   */
  default boolean notifyEarly()
  {
    return false;
  }
}
