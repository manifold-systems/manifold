package manifold.api.type;

import java.util.Collections;
import java.util.List;
import manifold.api.fs.IFile;
import manifold.api.host.IModule;
import manifold.api.host.RefreshKind;
import manifold.api.service.BaseService;

/**
 * A base class for non-resource based type manifolds.  For instance, a type
 * manifold for a subset of <a href="https://www.w3.org/RDF/">RDF</a> could
 * subclass {@link UrlTypeManifold}.
 */
@SuppressWarnings("unused")
public abstract class UrlTypeManifold extends BaseService implements ITypeManifold
{
  private IModule _module;

  @Override
  public void init( IModule module )
  {
    _module = module;
  }

  @Override
  public IModule getModule()
  {
    return _module;
  }

  @Override
  public ISourceKind getSourceKind()
  {
    return ISourceKind.Java;
  }

  @Override
  public ContributorKind getContributorKind()
  {
    return ContributorKind.Primary;
  }

  @Override
  public boolean isTopLevelType( String fqn )
  {
    return isType( fqn );
  }

  @Override
  public ClassType getClassType( String fqn )
  {
    return ClassType.JavaClass;
  }

  @Override
  public List<IFile> findFilesForType( String fqn )
  {
    return Collections.emptyList();
  }

  @Override
  public void clear()
  {

  }

  @Override
  public boolean handlesFileExtension( String fileExtension )
  {
    return false;
  }

  @Override
  public boolean handlesFile( IFile file )
  {
    return false;
  }

  @Override
  public String[] getTypesForFile( IFile file )
  {
    return new String[0];
  }

  @Override
  public RefreshKind refreshedFile( IFile file, String[] types, RefreshKind kind )
  {
    return null;
  }
}
