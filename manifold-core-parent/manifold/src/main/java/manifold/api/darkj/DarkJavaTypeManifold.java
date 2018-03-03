package manifold.api.darkj;

import com.sun.source.tree.CompilationUnitTree;
import com.sun.tools.javac.tree.JCTree;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import javax.tools.DiagnosticListener;
import javax.tools.JavaFileObject;
import manifold.api.host.IModuleComponent;
import manifold.api.type.JavaTypeManifold;
import manifold.internal.javac.JavaParser;
import manifold.util.StreamUtil;

/**
 */
public class DarkJavaTypeManifold extends JavaTypeManifold<Model>
{
  @SuppressWarnings("WeakerAccess")
  public static final Set<String> FILE_EXTENSIONS = Collections.singleton( "darkj" );

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
  public boolean isInnerType( String topLevel, String relativeInner )
  {
    if( isAnonymous( relativeInner ) )
    {
      return true;
    }

    Model model = getModel( topLevel );
    if( model == null )
    {
      return false;
    }

    JCTree.JCClassDecl classDecl = getClassDecl( model );
    if( classDecl == null )
    {
      return false;
    }

    for( JCTree m: classDecl.getMembers() )
    {
      if( m instanceof JCTree.JCClassDecl )
      {
        return isInnerClass( (JCTree.JCClassDecl)m, relativeInner );
      }
    }

    return false;
  }

  private JCTree.JCClassDecl getClassDecl( Model model )
  {
    JCTree.JCClassDecl classDecl = model.getClassDecl();
    if( classDecl != null )
    {
      return classDecl;
    }

    List<CompilationUnitTree> trees = new ArrayList<>();
    JavaParser.instance().parseText( getSource( model ), trees, null, null, null );
    if( trees.isEmpty() )
    {
      return null;
    }
    classDecl = (JCTree.JCClassDecl)trees.get( 0 ).getTypeDecls().get( 0 );
    model.setClassDecl( classDecl );
    return classDecl;
  }

  private boolean isAnonymous( String relativeInner )
  {
    String first = relativeInner;
    int iDot = relativeInner.indexOf( '.' );
    if( iDot > 0 )
    {
      first = relativeInner.substring( 0, iDot );
    }
    try
    {
      int result = Integer.parseInt( first );
      return result >= 0;
    }
    catch( Exception e )
    {
      return false;
    }
  }

  private boolean isInnerClass( JCTree.JCClassDecl cls, String relativeInner )
  {
    String name;
    String remainder;
    int iDot = relativeInner.indexOf( '.' );
    if( iDot > 0 )
    {
      name = relativeInner.substring( 0, iDot );
      remainder = relativeInner.substring( iDot+1 );
    }
    else
    {
      name = relativeInner;
      remainder = null;
    }
    if( cls.getSimpleName().toString().equals( name ) )
    {
      if( remainder != null )
      {
        for( JCTree m: cls.getMembers() )
        {
          if( m instanceof JCTree.JCClassDecl )
          {
            if( isInnerClass( (JCTree.JCClassDecl)m, remainder ) )
            {
              return true;
            }
          }
        }
      }
      else
      {
        return true;
      }
    }
    return false;
  }

  private String getSource( Model model )
  {
    try
    {
      return StreamUtil.getContent( new InputStreamReader( model.getFile().openInputStream() ) );
    }
    catch( IOException ioe )
    {
      throw new RuntimeException( ioe );
    }
  }

  @Override
  protected String contribute( String topLevelFqn, String existing, Model model, DiagnosticListener<JavaFileObject> errorHandler )
  {
    return getSource( model );
  }
}