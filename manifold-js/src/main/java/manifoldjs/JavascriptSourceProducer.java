package manifoldjs;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import javax.tools.DiagnosticListener;
import javax.tools.JavaFileObject;
import manifold.api.gen.SrcClass;
import manifold.api.host.ITypeLoader;
import manifold.api.sourceprod.ClassType;
import manifold.api.sourceprod.JavaSourceProducer;

/**
 * Created by carson on 5/10/17.
 */
public class JavascriptSourceProducer extends JavaSourceProducer<JavascriptModel>
{
  private static final Set<String> FILE_EXTENSIONS = new HashSet<>( Arrays.asList( "js", "jst" ) );

  public void init( ITypeLoader typeLoader )
  {
    init( typeLoader, JavascriptModel::new );
  }

  @Override
  public boolean handlesFileExtension( String fileExtension )
  {
    return FILE_EXTENSIONS.contains( fileExtension.toLowerCase() );
  }

  @Override
  protected boolean isInnerType( String topLevel, String relativeInner )
  {
    return false;
  }

  @Override
  protected String produce( String topLevelFqn, String existing, JavascriptModel model, DiagnosticListener<JavaFileObject> errrorHandler )
  {
    SrcClass srcClass = new JavascriptCodeGen( model.getFiles().iterator().next(), topLevelFqn ).make();
    //## todo: use errorhandler(), look at JsonImplSourceProvider
    return srcClass.render( new StringBuilder(), 0 ).toString();
  }

  @Override
  public ClassType getClassType( String fqn )
  {
    return ClassType.Class;
  }
}