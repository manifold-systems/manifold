package manifold.internal.javac;

import javax.tools.JavaFileManager;

public class ManPatchModuleLocation implements JavaFileManager.Location
{
  private final String _moduleName;

  ManPatchModuleLocation( String moduleName )
  {
    _moduleName = moduleName;
  }

  @Override
  public String getName()
  {
    return _moduleName;
  }

  @Override
  public boolean isOutputLocation()
  {
    return false;
  }
}
