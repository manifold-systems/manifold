package manifold.api.image;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import javax.tools.DiagnosticListener;
import javax.tools.JavaFileObject;
import manifold.api.fs.IFile;
import manifold.api.gen.SrcClass;
import manifold.api.host.IModuleComponent;
import manifold.api.type.JavaTypeManifold;

/**
 */
public class ImageTypeManifold extends JavaTypeManifold<Model>
{
  private static final Set<String> FILE_EXTENSIONS = new HashSet<>( Arrays.asList( "jpg", "png", "bmp", "wbmp", "gif" ) );

  @Override
  public void init( IModuleComponent typeLoader )
  {
    init( typeLoader, Model::new );
  }

  @Override
  public boolean handlesFileExtension( String fileExtension )
  {
    return FILE_EXTENSIONS.contains( fileExtension.toLowerCase() );
  }

  @Override
  public String getTypeNameForFile( String fqn, IFile file )
  {
    if( !(fqn.endsWith( file.getBaseName() + '_' + file.getExtension() )) )
    {
      fqn = fqn + '_' + file.getExtension();
    }
    return fqn;
  }

  @Override
  public boolean isInnerType( String topLevel, String relativeInner )
  {
    return false;
  }

  @Override
  protected String contribute( String topLevelFqn, String existing, Model model, DiagnosticListener<JavaFileObject> errorHandler )
  {
    SrcClass srcClass = new ImageCodeGen( model._url, topLevelFqn ).make();
    StringBuilder sb = srcClass.render( new StringBuilder(), 0 );
    return sb.toString();
  }
}