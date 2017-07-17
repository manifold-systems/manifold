package manifold.ext;

import com.sun.tools.javac.code.Symbol;
import com.sun.tools.javac.tree.JCTree;
import com.sun.tools.javac.tree.TreeTranslator;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import javax.lang.model.element.ElementKind;
import javax.tools.DiagnosticListener;
import javax.tools.JavaFileObject;
import manifold.api.fs.IFile;
import manifold.api.fs.cache.ModulePathCache;
import manifold.api.host.ITypeLoader;
import manifold.api.sourceprod.ClassType;
import manifold.api.sourceprod.ITypeProcessor;
import manifold.api.sourceprod.JavaSourceProducer;
import manifold.ext.api.Extension;
import manifold.internal.javac.IssueReporter;
import manifold.internal.javac.TypeProcessor;
import manifold.util.StreamUtil;

/**
 */
public class ExtSourceProducer extends JavaSourceProducer<Model> implements ITypeProcessor
{
  static final String EXTENSIONS_PACKAGE = "extensions";
  private static final Set<String> FILE_EXTENSIONS = new HashSet<>( Arrays.asList( "java", "class" ) );

  public void init( ITypeLoader typeLoader )
  {
    init( typeLoader, ( fqn, files ) -> new Model( fqn, files, this ) );
  }

  @Override
  public boolean handlesFileExtension( String fileExtension )
  {
    return FILE_EXTENSIONS.contains( fileExtension.toLowerCase() );
  }

  @Override
  public ProducerKind getProducerKind()
  {
    return ProducerKind.Supplemental;
  }

  @Override
  protected String aliasFqn( String fqn, IFile file )
  {
    if( fqn.length() > EXTENSIONS_PACKAGE.length() + 2 && fqn.startsWith( EXTENSIONS_PACKAGE + '.' ) )
    {
      String extendedType = fqn.substring( EXTENSIONS_PACKAGE.length() + 1 );

      int iDot = extendedType.lastIndexOf( '.' );
      if( iDot > 0 )
      {
        return extendedType.substring( 0, iDot );
      }
    }
    return null;
  }

  @Override
  public boolean handlesFile( IFile file )
  {
    Set<String> fqns = ModulePathCache.instance().get( getModule() ).getFqnForFile( file );
    if( fqns == null )
    {
      return false;
    }

    for( String fqn : fqns )
    {
      if( fqn.length() > EXTENSIONS_PACKAGE.length() + 2 && fqn.startsWith( EXTENSIONS_PACKAGE + '.' ) )
      {
        String extendedType = fqn.substring( EXTENSIONS_PACKAGE.length() + 1 );

        int iDot = extendedType.lastIndexOf( '.' );
        if( iDot > 0 )
        {
          //extendedType = extendedType.substring( 0, iDot );
          try
          {
            // barf
            if( file.getExtension().equals( "java" ) )
            {
              String content = StreamUtil.getContent( new InputStreamReader( file.openInputStream() ) );
              return content.contains( "@Extension" ) && content.contains( Extension.class.getPackage().getName() );
            }
            else
            {
              String content = StreamUtil.getContent( new InputStreamReader( file.openInputStream() ) );
              return content.contains( Extension.class.getName().replace( '.', '/' ) );
            }
          }
          catch( IOException e )
          {
            // eat
          }
        }
      }
    }
    return false;
  }

  @Override
  public String[] getTypesForFile( IFile file )
  {
    String[] typesForFile = super.getTypesForFile( file );
    if( typesForFile != null && typesForFile.length > 0 )
    {
      String fqn = typesForFile[0];
      if( fqn.startsWith( EXTENSIONS_PACKAGE ) )
      {
        fqn = fqn.substring( EXTENSIONS_PACKAGE.length() + 1 );
        int iDot = fqn.lastIndexOf( '.' );
        fqn = iDot == -1 ? fqn : fqn.substring( 0, iDot );
        return new String[]{fqn};
      }
    }
    return typesForFile;
  }

  @Override
  protected boolean isInnerType( String topLevel, String relativeInner )
  {
    return false;
  }

  @Override
  public ClassType getClassType( String fqn )
  {
    return ClassType.Class;
  }

  @Override
  public void clear()
  {
    super.clear();
  }

  @Override
  protected String produce( String topLevelFqn, String existing, Model model, DiagnosticListener<JavaFileObject> errorHandler )
  {
    return new ExtCodeGen( model, topLevelFqn, existing ).make( errorHandler );
  }

  @Override
  public void process( String fqn, TypeProcessor typeProcessor, IssueReporter<JavaFileObject> issueReporter )
  {
    Symbol.ClassSymbol typeElement = typeProcessor.getElementUtil().getTypeElement( fqn );
    if( typeElement.getKind() == ElementKind.CLASS || typeElement.getKind() == ElementKind.INTERFACE )
    {
      JCTree tree = (JCTree)typeProcessor.getTreeUtil().getTree( typeElement );
      TreeTranslator visitor = new ExtensionTransformer( this, typeProcessor );
      tree.accept( visitor );
    }
  }

//  @Override
//  public boolean filterError( TypeProcessor typeProcessor, Diagnostic diagnostic )
//  {
//    if( diagnostic.getKind() == Diagnostic.Kind.ERROR )
//    {
//      Object[] args = ((JCDiagnostic)diagnostic).getArgs();
//      if( args != null )
//      {
//        for( Object arg: args )
//        {
//          if( arg instanceof JCDiagnostic )
//          {
//            JCDiagnostic jcArg = (JCDiagnostic)arg;
//            if( jcArg.getCode().equals( "compiler.misc.inconvertible.types" ) )
//            {
//              Object[] argArgs = jcArg.getArgs();
//              if( argArgs != null && argArgs.length == 2 )
//              {
//                Type.ClassType type = (Type.ClassType)argArgs[1];
//                if( type.tsym.hasAnnotations() )
//                {
//                  for( Attribute.Compound anno: type.tsym.getAnnotationMirrors() )
//                  {
//                    if( ((Type.ClassType)anno.getAnnotationType()).tsym.getQualifiedName().toString().equals( Structural.class.getName() ) )
//                    {
//                      //((JCDiagnostic)diagnostic).getDiagnosticPosition().getTree().type = type;
//                      return true;
//                    }
//                  }
//                }
//              }
//            }
//          }
//        }
//      }
//    }
//    return false;
//  }
}