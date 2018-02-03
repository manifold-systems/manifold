package manifold.api.type;

import java.util.Collections;
import java.util.List;
import manifold.api.fs.IFile;
import manifold.api.host.ITypeLoader;
import manifold.api.host.RefreshKind;
import manifold.api.service.BaseService;

public abstract class UrlTypeManifold extends BaseService implements ITypeManifold
{
  private ITypeLoader _typeLoader;

  @Override
  public void init( ITypeLoader tl )
  {
    _typeLoader = tl;
  }

  @Override
  public ITypeLoader getTypeLoader()
  {
    return _typeLoader;
  }

  @Override
  public SourceKind getSourceKind()
  {
    return SourceKind.Java;
  }

  @Override
  public ProducerKind getProducerKind()
  {
    return ProducerKind.Primary;
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
