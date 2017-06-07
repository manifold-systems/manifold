package manifold.api.json;

import java.util.Collections;
import java.util.Set;
import java.util.StringTokenizer;
import javax.tools.DiagnosticListener;
import javax.tools.JavaFileObject;
import manifold.api.fs.IFile;
import manifold.api.gen.SrcClass;
import manifold.api.sourceprod.ClassType;
import manifold.api.sourceprod.JavaSourceProducer;
import manifold.api.host.ITypeLoader;
import manifold.util.GosuClassUtil;

/**
 */
public class JsonImplSourceProducer extends JavaSourceProducer<Model>
{
  private static final String FILE_EXTENSION = "json";
  private static final Set<String> FILE_EXTENSIONS = Collections.singleton( FILE_EXTENSION );
  private static final String IMPL = "impl";

  public void init( ITypeLoader typeLoader )
  {
    init( typeLoader, FILE_EXTENSIONS, Model::new );
  }

  @Override
  protected String aliasFqn( String fqn, IFile file )
  {
    return makeImplName( fqn );
  }

  static String makeImplName( String fqn )
  {
    return GosuClassUtil.getPackage( fqn ) + '.' + IMPL + '.' + GosuClassUtil.getShortClassName( fqn );
  }

  @Override
  protected boolean isInnerType( String topLevel, String relativeInner )
  {
    Model model = getModel( topLevel );
    JsonStructureType type = model == null ? null : model.getType();
    if( type == null )
    {
      return false;
    }
    JsonStructureType csr = type;
    for( StringTokenizer tokenizer = new StringTokenizer( relativeInner, "." ); tokenizer.hasMoreTokens(); )
    {
      String childName = tokenizer.nextToken();
      IJsonParentType child = csr.findChild( childName );
      if( child instanceof JsonStructureType )
      {
        csr = (JsonStructureType)child;
        continue;
      }
      else if( child instanceof JsonListType )
      {
        IJsonType componentType = ((JsonListType)child).getComponentType();
        if( componentType instanceof JsonStructureType )
        {
          csr = (JsonStructureType)componentType;
          continue;
        }
      }
      return false;
    }
    return true;
  }

  @Override
  protected String produce( String topLevelFqn, Model model, DiagnosticListener<JavaFileObject> errorHandler )
  {
    StringBuilder sb = new StringBuilder();
    SrcClass srcClass = new JsonImplCodeGen( model.getType(), topLevelFqn ).make();
    model.report( errorHandler );
    srcClass.render( sb, 0 );
    return sb.toString();
  }

  @Override
  public ClassType getClassType( String fqn )
  {
    return ClassType.Class;
  }
}