package manifold.js;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import javax.tools.DiagnosticListener;
import javax.tools.JavaFileManager;
import javax.tools.JavaFileObject;
import manifold.api.gen.SrcClass;
import manifold.api.host.IModule;
import manifold.api.type.JavaTypeManifold;

/**
 * Created by carson on 5/10/17.
 */
public class JavascriptTypeManifold extends JavaTypeManifold<JavascriptModel>
{
  public static final String JS = "js";
  public static final String JST = "jst";
  private static final Set<String> FILE_EXTENSIONS = new HashSet<>( Arrays.asList( JS, JST ) );

  public void init( IModule module )
  {
    init( module, (fqn, files) -> new JavascriptModel( getModule().getHost(), fqn, files ) );
  }

  @Override
  public boolean handlesFileExtension( String fileExtension )
  {
    return FILE_EXTENSIONS.contains( fileExtension.toLowerCase() );
  }

  @Override
  public boolean isInnerType( String topLevel, String relativeInner )
  {
    return false;
  }

  @Override
  protected String contribute( JavaFileManager.Location location, String topLevelFqn, String existing, JavascriptModel model, DiagnosticListener<JavaFileObject> errorHandler )
  {
    SrcClass srcClass = new JavascriptCodeGen( model.getFiles().iterator().next(), topLevelFqn ).make( errorHandler );
    return srcClass.render( new StringBuilder(), 0).toString();
  }
}