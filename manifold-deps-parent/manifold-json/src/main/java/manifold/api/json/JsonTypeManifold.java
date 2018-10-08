package manifold.api.json;

import java.util.StringTokenizer;
import javax.tools.DiagnosticListener;
import javax.tools.JavaFileManager;
import javax.tools.JavaFileObject;
import manifold.api.host.IModule;
import manifold.api.type.JavaTypeManifold;
import manifold.ext.api.Structural;
import manifold.util.ManClassUtil;

/**
 */
public class JsonTypeManifold extends JavaTypeManifold<JsonModel>
{
  public static final String FILE_EXTENSION = "json";

  @Override
  public void init( IModule module )
  {
    init( module, (fqn, files) -> new JsonModel( getModule().getHost(), fqn, files ) );
  }

  @Override
  public boolean handlesFileExtension( String fileExtension )
  {
    return fileExtension.equals( FILE_EXTENSION );
  }

  @Override
  public boolean isInnerType( String topLevel, String relativeInner )
  {
    JsonModel model = getModel( topLevel );
    IJsonParentType type = model == null ? null : model.getType();
    if( type == null )
    {
      return false;
    }
    IJsonParentType csr = type;
    for( StringTokenizer tokenizer = new StringTokenizer( relativeInner, "." ); tokenizer.hasMoreTokens(); )
    {
      String childName = tokenizer.nextToken();
      IJsonType child = csr.findChild( childName );
      if( child instanceof IJsonParentType )
      {
        csr = (IJsonParentType)child;
        continue;
      }
      return false;
    }
    return true;
  }

  @Override
  protected String contribute( JavaFileManager.Location location, String topLevelFqn, String existing, JsonModel model, DiagnosticListener<JavaFileObject> errorHandler )
  {
    StringBuilder sb = new StringBuilder();
    sb.append( "package " ).append( ManClassUtil.getPackage( topLevelFqn ) ).append( ";\n\n" )
      .append( "import " ).append( Structural.class.getName() ).append( ";\n\n" );
    model.report( errorHandler );
    model.getType().render( sb, 0, true );
    return sb.toString();
  }
}