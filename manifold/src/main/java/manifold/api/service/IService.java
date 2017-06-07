/*
 * Copyright 2014 Guidewire Software, Inc.
 */

package manifold.api.service;

public interface IService extends IPluginHost
{
  /**
   * @return true if this service has been initialized, false otherwise
   */
  boolean isInited();

  /**
   * Initialize this service
   */
  void init();

  /**
   * Uninitialize this service
   */
  void uninit();
}