package manifold.ext;

import com.sun.source.tree.CompilationUnitTree;
import com.sun.source.tree.Tree;
import com.sun.tools.javac.api.JavacTaskImpl;
import com.sun.tools.javac.code.Flags;
import com.sun.tools.javac.code.Symbol;
import com.sun.tools.javac.tree.JCTree;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import javax.tools.DiagnosticListener;
import javax.tools.JavaFileObject;
import manifold.api.fs.IFile;
import manifold.api.fs.cache.ModulePathCache;
import manifold.api.fs.cache.PathCache;
import manifold.api.gen.AbstractSrcMethod;
import manifold.api.gen.SrcAnnotationExpression;
import manifold.api.gen.SrcClass;
import manifold.api.gen.SrcMethod;
import manifold.api.gen.SrcParameter;
import manifold.api.gen.SrcRawStatement;
import manifold.api.gen.SrcStatementBlock;
import manifold.api.gen.SrcType;
import manifold.api.host.IModule;
import manifold.ext.api.Extension;
import manifold.ext.api.This;
import manifold.internal.javac.ClassSymbols;
import manifold.internal.javac.JavaParser;

/**
 */
class ExtCodeGen
{
  private final Model _model;
  private final String _fqn;
  private String _existingSource;

  ExtCodeGen( Model model, String topLevelFqn, String existingSource )
  {
    _model = model;
    _fqn = topLevelFqn;
    _existingSource = existingSource;
  }

  private IModule getModule()
  {
    return _model.getSourceProducer().getTypeLoader().getModule();
  }

  String make( DiagnosticListener<JavaFileObject> errorHandler )
  {
    SrcClass srcExtended;
    if( !_existingSource.isEmpty() )
    {
      srcExtended = makeStubFromSource();
    }
    else
    {
      srcExtended = ClassSymbols.instance( getModule() ).makeSrcClassStub( _fqn );
    }
    return addExtensions( srcExtended, errorHandler );
  }

  private SrcClass makeStubFromSource()
  {
    SrcClass srcExtended;List<CompilationUnitTree> trees = new ArrayList<>();
    JavaParser.instance().parseText( _existingSource, trees, null, null, null );
    JCTree.JCClassDecl classDecl = (JCTree.JCClassDecl)trees.get( 0 ).getTypeDecls().get( 0 );
    srcExtended = new SrcClass( _fqn, classDecl.getKind() == Tree.Kind.CLASS ? SrcClass.Kind.Class : SrcClass.Kind.Interface )
      .modifiers( classDecl.getModifiers().getFlags() );
    if( classDecl.extending != null )
    {
      srcExtended.superClass( classDecl.extending.toString() );
    }
    for( JCTree.JCExpression iface: classDecl.implementing )
    {
      srcExtended.addInterface( iface.toString() );
    }
    return srcExtended;
  }

  private String addExtensions( SrcClass extendedClass, DiagnosticListener<JavaFileObject> errorHandler )
  {
    boolean methodExtensions = false;
    boolean interfaceExtensions = false;
    boolean annotationExtensions = false;
    Set<String> allExtensions = findAllExtensions();
    for( String fqn : allExtensions )
    {
      JavacTaskImpl[] javacTask = new JavacTaskImpl[1];
      SrcClass srcExtension = ClassSymbols.instance( getModule() ).makeSrcClassStub( fqn, javacTask );
      if( srcExtension != null )
      {
        for( AbstractSrcMethod method : srcExtension.getMethods() )
        {
          addExtensionMethod( method, extendedClass, errorHandler, javacTask[0] );
          methodExtensions = true;
        }
        for( SrcType iface: srcExtension.getInterfaces() )
        {
          addExtensionInteface( iface, extendedClass, errorHandler, javacTask[0] );
          interfaceExtensions = true;
        }
        for( SrcAnnotationExpression anno: srcExtension.getAnnotations() )
        {
          addExtensionAnnotation( anno, extendedClass, errorHandler, javacTask[0] );
          annotationExtensions = true;
        }
      }
    }
    if( !_existingSource.isEmpty() )
    {
      return addExtensionsToExistingClass( extendedClass, methodExtensions, interfaceExtensions, annotationExtensions );
    }
    else
    {
      return extendedClass.render( new StringBuilder(), 0 ).toString();
    }
  }

  private String addExtensionsToExistingClass( SrcClass srcClass, boolean methodExtensions, boolean interfaceExtensions, boolean annotationExtensions )
  {
    StringBuilder sb = new StringBuilder();
    if( methodExtensions )
    {
      addExtensionMethodsToExistingClass( srcClass, sb );
    }
    if( interfaceExtensions )
    {
      addExtensionInterfacesToExistingClass( srcClass, sb );
    }
    if( annotationExtensions )
    {
      addExtensionAnnotationsToExistingClass( srcClass, sb );
    }
    return sb.toString();
  }

  private void addExtensionInterfacesToExistingClass( SrcClass srcClass, StringBuilder sb )
  {
    String start = (srcClass.isInterface() ? "interface " : "class ") + srcClass.getSimpleName();
    int iStart = sb.indexOf( start );
    int iBrace = sb.indexOf( "{", iStart );

    StringBuilder sbSrcClass = new StringBuilder();
    srcClass.render( sbSrcClass, 0 );
    int iSrcClassStart = sbSrcClass.indexOf( start );
    int iSrcClassBrace = sbSrcClass.indexOf( "{", iSrcClassStart );
    String fromSrcClass = sbSrcClass.substring( iSrcClassStart, iSrcClassBrace );

    sb.replace( iStart, iBrace, fromSrcClass );
  }

  private void addExtensionAnnotationsToExistingClass( SrcClass srcClass, StringBuilder sb )
  {
    if( srcClass.getAnnotations().isEmpty() )
    {
      return;
    }

    StringBuilder sbAnnos = new StringBuilder();
    for( SrcAnnotationExpression anno: srcClass.getAnnotations() )
    {
      anno.render( sbAnnos, 0 ).append( '\n' );
    }

    String start = (srcClass.isInterface() ? "interface " : "class ") + srcClass.getSimpleName();
    int iStart = sb.indexOf( start );
    while( iStart != 0 )
    {
      if( sb.charAt( iStart ) == '\n' )
      {
        break;
      }
      iStart--;
    }
    if( sb.charAt( iStart ) == '\n' )
    {
      iStart++;
    }

    sb.insert( iStart, sbAnnos );
  }

  private void addExtensionMethodsToExistingClass( SrcClass srcClass, StringBuilder sb )
  {
    int iBrace = _existingSource.lastIndexOf( '}' );
    sb.append( _existingSource.substring( 0, iBrace ) );
    for( AbstractSrcMethod method: srcClass.getMethods() )
    {
      method.render( sb, 2 );
    }
    sb.append( "\n}" );
  }

  private Set<String> findAllExtensions()
  {
    Set<String> fqns = new HashSet<>();

    PathCache pathCache = ModulePathCache.instance().get( getModule() );
    for( IFile file : _model.getFiles() )
    {
      Set<String> fqn = pathCache.getFqnForFile( file );
      for( String f : fqn )
      {
        if( f != null )
        {
          fqns.add( f );
        }
      }
    }
    return fqns;
  }

  private void addExtensionInteface( SrcType iface, SrcClass extendedType, DiagnosticListener<JavaFileObject> errorHandler, JavacTaskImpl javacTask )
  {
    extendedType.addInterface( iface );
  }

  private void addExtensionAnnotation( SrcAnnotationExpression anno, SrcClass extendedType, DiagnosticListener<JavaFileObject> errorHandler, JavacTaskImpl javacTask )
  {
    if( extendedType.getAnnotations().stream().noneMatch( e -> e.getAnnotationType().equals( anno.getAnnotationType() ) ) )
    {
      extendedType.addAnnotation( anno.copy() );
    }
  }

  private void addExtensionMethod( AbstractSrcMethod method, SrcClass extendedType, DiagnosticListener<JavaFileObject> errorHandler, JavacTaskImpl javacTask )
  {
    if( !isExtensionMethod( method, extendedType ) )
    {
      return;
    }

//    Pair<Symbol.ClassSymbol, JCTree.JCCompilationUnit> extendedClassSym = ClassSymbols.instance( getModule() ).getClassSymbol( javacTask, extendedType.getName() );
//    if( !verifyExtensionMethod( method, extendedClassSym.getFirst(), errorHandler, javacTask ) )
//    {
//      return;
//    }

    // the class is a produced class, therefore we must delegate the calls since calls are not replaced
    boolean delegateCalls = !_existingSource.isEmpty();

    SrcMethod srcMethod = new SrcMethod( extendedType );
    long modifiers = method.getModifiers();
    if( extendedType.isInterface() )
    {
      // extension method must be default method in interface to not require implementation
      modifiers |= Flags.DEFAULT;
    }
//## Don't mark extension methods on classes as final, it otherwise blocks extended
//   classes from implementing an interface with the same method signature
//    else
//    {
//      // extension method must be final in class to prohibit override
//      modifiers |= Modifier.FINAL;
//    }

    // remove static
    srcMethod.modifiers( modifiers & ~Modifier.STATIC );

    if( !delegateCalls )
    {
      // mark as extension method for efficient lookup during method call replacement
      srcMethod.addAnnotation(
        new SrcAnnotationExpression( ExtensionMethod.class )
          .addArgument( "extensionClass", String.class, ((SrcClass)method.getOwner()).getName() ) );
    }

    srcMethod.returns( method.getReturnType() );

    String name = method.getSimpleName();
    srcMethod.name( name );
    List<SrcType> typeParams = method.getTypeVariables();

    // extension method must reflect extended type's type vars before its own
    int extendedTypeVarCount = extendedType.getTypeVariables().size();
    for( int i = extendedTypeVarCount; i < typeParams.size(); i++ )
    {
      SrcType typeVar = typeParams.get( i );
      srcMethod.addTypeVar( typeVar );
    }

    List<SrcParameter> params = method.getParameters();
    for( int i = 1; i < params.size(); i++ )
    {
      // exclude This param

      SrcParameter param = params.get( i );
      srcMethod.addParam( param.getSimpleName(), param.getType() );
    }

    for( Object throwType : method.getThrowTypes() )
    {
      srcMethod.addThrowType( (SrcType)throwType );
    }

    if( delegateCalls )
    {
      // delegate to the extension method

      StringBuilder call = new StringBuilder();
      SrcType returnType = srcMethod.getReturnType();
      if( returnType != null && !returnType.getName().equals( void.class.getName() ) )
      {
        call.append( "return " );
      }
      String extClassName = ((SrcClass)method.getOwner()).getName();
      call.append( extClassName ).append( '.' ).append( srcMethod.getSimpleName() ).append( "(this" );
      for( SrcParameter param: srcMethod.getParameters() )
      {
        call.append( ", " ).append( param.getSimpleName() );
      }
      call.append( ");\n" );
      srcMethod.body( new SrcStatementBlock()
                              .addStatement(
                                new SrcRawStatement()
                                  .rawText( call.toString() ) ) );
    }
    else
    {
      // stub the body

      srcMethod.body( new SrcStatementBlock()
                        .addStatement(
                          new SrcRawStatement()
                            .rawText( "throw new " + RuntimeException.class.getSimpleName() + "(\"Should not exist at runtime!\");" ) ) );
    }

    extendedType.addMethod( srcMethod );
  }

  private boolean isExtensionMethod( AbstractSrcMethod method, SrcClass extendedType )
  {
    if( !Modifier.isStatic( (int)method.getModifiers() ) || Modifier.isPrivate( (int)method.getModifiers() ) )
    {
      return false;
    }
    List<SrcParameter> params = method.getParameters();
    if( params.size() == 0 )
    {
      return false;
    }
    SrcParameter param = params.get( 0 );
    List<SrcAnnotationExpression> annotations = param.getAnnotations();
    if( annotations.size() > 0 && annotations.get( 0 ).getAnnotationType().equals( This.class.getName() ) )
    {
      return false;
    }
    // checking only for simple name for cases where the name cannot be resolved yet e.g., extension method on another source producer type
    return param.getType().getName().endsWith( extendedType.getSimpleName() );
  }

  private boolean verifyExtensionMethod( AbstractSrcMethod method, Symbol.ClassSymbol extendedType, DiagnosticListener<JavaFileObject> errorHandler, JavacTaskImpl javacTask )
  {
    return true;
    //## todo: warn if method with same signature exists in extended type hierarchy
    //     JCDiagnostic.DiagnosticPosition position = ((JCTree)Trees.instance( javacTask ).getTree( extensionMethodSymbol )).pos();
    //        errorHandler.report( new JavacDiagnostic( classSymbol.sourcefile, Diagnostic.Kind.WARN, position.getStartPosition(), 0, 0,
    //                                                  "Illegal extension method. '" + extendedMethodName +
    //                                                  "' overloads or shadows a method in the extended class" ) );
  }

  public boolean isManifoldExtAnnotation( SrcAnnotationExpression anno )
  {
    return anno.getAnnotationType().startsWith( Extension.class.getPackage().getName() );
  }
}
