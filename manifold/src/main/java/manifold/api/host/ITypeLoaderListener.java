/*
 * Copyright 2014 Guidewire Software, Inc.
 */

package manifold.api.host;

public interface ITypeLoaderListener
{
  /**
   * Fired when an existing type is refreshed, i.e. there are potential changes
   * @param request
   */
  public void refreshedTypes( RefreshRequest request );

  /**
   * Fired when the typesystem is fully refreshed
   */
  public void refreshed();

}
