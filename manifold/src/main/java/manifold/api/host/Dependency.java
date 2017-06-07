/*
 * Copyright 2014 Guidewire Software, Inc.
 */

package manifold.api.host;

public class Dependency
{
  private IModule _module;
  private boolean _exported;

  public Dependency( IModule module, boolean bExported )
  {
    _module = module;
    _exported = bExported;
  }

  public IModule getModule()
  {
    return _module;
  }

  public boolean isExported()
  {
    return _exported;
  }
  
  public String toString()
  {
    return _module.toString() + (_exported ? " (exported)" : " (not exported)");
  }
}
