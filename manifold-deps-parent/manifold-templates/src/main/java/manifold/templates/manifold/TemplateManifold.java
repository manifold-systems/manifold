package manifold.templates.manifold;

import javax.tools.DiagnosticListener;
import javax.tools.JavaFileManager;
import javax.tools.JavaFileObject;
import manifold.api.fs.IFile;
import manifold.api.host.IModule;
import manifold.api.type.JavaTypeManifold;

public class TemplateManifold extends JavaTypeManifold<TemplateModel>
{
  public void init( IModule module )
  {
    init( module, (fqn, files) -> new TemplateModel( module.getHost(), fqn, files ) );
  }

  /**
   * Remove the content extension e.g., com.abc.Foo_html -> com.abc.Foo
   */
  @Override
  public String getTypeNameForFile( String fqn, IFile file )
  {
    String fileBaseName = file.getBaseName();
    int contentExt = fileBaseName.lastIndexOf( '.' );
    if( contentExt < 0 )
    {
      // No secondary extension in name, keep fqn as-is
      return fqn;
    }
    int indexContentExt = fqn.lastIndexOf( '_' );
    if( fqn.substring( indexContentExt + 1 ).equals( fileBaseName.substring( contentExt + 1 ) ) )
    {
      return fqn.substring( 0, indexContentExt );
    }
    return fqn;
  }

  @Override
  public boolean isInnerType( String topLevelFqn, String relativeInner )
  {
    return true;
  }

  @Override
  public boolean handlesFileExtension( String fileExtension )
  {
    return fileExtension.equals( "mtl" );
  }

  @Override
  public boolean handlesFile( IFile file )
  {
    return file.getExtension().equals( "mtl" );
  }

  @Override
  protected String contribute( JavaFileManager.Location location, String topLevelFqn, String existing, TemplateModel model, DiagnosticListener<JavaFileObject> errorHandler )
  {
    String source = model.getSource();
    model.report( errorHandler );
    return source;
  }

}
