package manifold.api.host;

public interface ITypeLoaderListener
{
  /**
   * Fired when an existing type is refreshed, i.e. there are potential changes
   */
  void refreshedTypes( RefreshRequest request );

  /**
   * Fired when the typesystem is fully refreshed
   */
  void refreshed();

}
