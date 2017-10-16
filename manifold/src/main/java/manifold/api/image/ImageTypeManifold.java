package manifold.api.image;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import javax.tools.DiagnosticListener;
import javax.tools.JavaFileObject;
import manifold.api.fs.IFile;
import manifold.api.gen.SrcClass;
import manifold.api.host.ITypeLoader;
import manifold.api.type.JavaTypeManifold;

/**
 */
public class ImageTypeManifold extends JavaTypeManifold<Model>
{
  private static final Set<String> FILE_EXTENSIONS = new HashSet<>( Arrays.asList( "jpg", "png", "bmp", "wbmp", "gif" ) );

  public void init( ITypeLoader typeLoader )
  {
    init( typeLoader, Model::new );
  }

  @Override
  public boolean handlesFileExtension( String fileExtension )
  {
    return FILE_EXTENSIONS.contains( fileExtension.toLowerCase() );
  }

  @Override
  protected String aliasFqn( String fqn, IFile file )
  {
    return fqn + '_' + file.getExtension();
  }

  @Override
  protected boolean isInnerType( String topLevel, String relativeInner )
  {
    return false;
  }

  @Override
  protected String produce( String topLevelFqn, String existing, Model model, DiagnosticListener<JavaFileObject> errorHandler )
  {
    SrcClass srcClass = new ImageCodeGen( model._url, topLevelFqn ).make();
    StringBuilder sb = srcClass.render( new StringBuilder(), 0 );
    return sb.toString();
  }
}