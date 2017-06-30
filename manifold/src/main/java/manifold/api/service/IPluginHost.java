package manifold.api.service;

import java.util.Collections;
import java.util.List;

/**
 * This simple interface provides the core foundation for component architecture in Gosu.
 */
public interface IPluginHost
{
  /**
   * Provides an implementation of a specified interface.
   *
   * @return The implementation[s] of the interface or null if unsupported.
   */
  default <T> List<T> getInterface( Class<T> apiInterface )
  {
    return Collections.emptyList();
  }
}
