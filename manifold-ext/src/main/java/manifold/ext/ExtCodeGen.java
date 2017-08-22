package manifold.ext;

import com.sun.source.tree.CompilationUnitTree;
import com.sun.source.tree.Tree;
import com.sun.tools.javac.api.JavacTaskImpl;
import com.sun.tools.javac.code.Flags;
import com.sun.tools.javac.code.Symbol;
import com.sun.tools.javac.model.JavacElements;
import com.sun.tools.javac.tree.JCTree;
import java.lang.reflect.Modifier;
import java.util.*;
import javax.tools.Diagnostic;
import javax.tools.DiagnosticListener;
import javax.tools.JavaFileObject;
import manifold.ExtIssueMsg;
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
import manifold.internal.javac.SourceJavaFileObject;
import manifold.util.JavacDiagnostic;

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
    return _model.getTypeManifold().getTypeLoader().getModule();
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
    List<CompilationUnitTree> trees = new ArrayList<>();
    JavaParser.instance().parseText( _existingSource, trees, null, null, null );
    JCTree.JCClassDecl classDecl = (JCTree.JCClassDecl)trees.get( 0 ).getTypeDecls().get( 0 );
    SrcClass srcExtended = new SrcClass( _fqn, classDecl.getKind() == Tree.Kind.CLASS ? SrcClass.Kind.Class : SrcClass.Kind.Interface )
      .modifiers( classDecl.getModifiers().getFlags() );
    if( classDecl.extending != null )
    {
      srcExtended.superClass( classDecl.extending.toString() );
    }
    for( JCTree.JCExpression iface : classDecl.implementing )
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
    JavacTaskImpl[] javacTask = new JavacTaskImpl[1];
    for( String fqn : allExtensions )
    {
      SrcClass srcExtension = ClassSymbols.instance( getModule() ).makeSrcClassStub( fqn, javacTask, null );
      if( srcExtension != null )
      {
        for( AbstractSrcMethod method : srcExtension.getMethods() )
        {
          addExtensionMethod( method, extendedClass, errorHandler, javacTask[0] );
          methodExtensions = true;
        }
        for( SrcType iface : srcExtension.getInterfaces() )
        {
          addExtensionInteface( iface, extendedClass, errorHandler, javacTask[0] );
          interfaceExtensions = true;
        }
        for( SrcAnnotationExpression anno : srcExtension.getAnnotations() )
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
    for( SrcAnnotationExpression anno : srcClass.getAnnotations() )
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
    for( AbstractSrcMethod method : srcClass.getMethods() )
    {
      method.render( sb, 2 );
    }
    sb.append( "\n}" );
  }

  private Set<String> findAllExtensions()
  {
    Set<String> fqns = new TreeSet<>();

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

    if( warnIfDuplicate( method, extendedType, errorHandler, javacTask ) )
    {
      return;
    }

    // the class is a produced class, therefore we must delegate the calls since calls are not replaced
    boolean delegateCalls = !_existingSource.isEmpty();

    boolean isInstanceExtensionMethod = isInstanceExtensionMethod( method, extendedType );

    SrcMethod srcMethod = new SrcMethod( extendedType );
    long modifiers = method.getModifiers();
    if( extendedType.isInterface() && isInstanceExtensionMethod )
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

    if( isInstanceExtensionMethod )
    {
      // remove static for instance method
      modifiers &= ~Modifier.STATIC;
    }

    srcMethod.modifiers( modifiers );

    if( !delegateCalls )
    {
      // mark as extension method for efficient lookup during method call replacement
      srcMethod.addAnnotation(
        new SrcAnnotationExpression( ExtensionMethod.class )
          .addArgument( "extensionClass", String.class, ((SrcClass)method.getOwner()).getName() )
          .addArgument( "isStatic", boolean.class, !isInstanceExtensionMethod ) );
    }

    srcMethod.returns( method.getReturnType() );

    String name = method.getSimpleName();
    srcMethod.name( name );
    List typeParams = method.getTypeVariables();

    // extension method must reflect extended type's type vars before its own
    int extendedTypeVarCount = extendedType.getTypeVariables().size();
    for( int i = isInstanceExtensionMethod ? extendedTypeVarCount : 0; i < typeParams.size(); i++ )
    {
      SrcType typeVar = (SrcType)typeParams.get( i );
      srcMethod.addTypeVar( typeVar );
    }

    List params = method.getParameters();
    for( int i = isInstanceExtensionMethod ? 1 : 0; i < params.size(); i++ )
    {
      // exclude This param

      SrcParameter param = (SrcParameter)params.get( i );
      srcMethod.addParam( param.getSimpleName(), param.getType() );
    }

    for( Object throwType : method.getThrowTypes() )
    {
      srcMethod.addThrowType( (SrcType)throwType );
    }

    if( delegateCalls )
    {
      // delegate to the extension method

      delegateCall( method, isInstanceExtensionMethod, srcMethod );
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

  private void delegateCall( AbstractSrcMethod method, boolean isInstanceExtensionMethod, SrcMethod srcMethod )
  {
    StringBuilder call = new StringBuilder();
    SrcType returnType = srcMethod.getReturnType();
    if( returnType != null && !returnType.getName().equals( void.class.getName() ) )
    {
      call.append( "return " );
    }
    String extClassName = ((SrcClass)method.getOwner()).getName();
    call.append( extClassName ).append( '.' ).append( srcMethod.getSimpleName() ).append( '(' );
    if( isInstanceExtensionMethod )
    {
      call.append( "this" );
    }
    for( SrcParameter param : srcMethod.getParameters() )
    {
      if( call.charAt( call.length()-1 ) != '(' )
      {
        call.append( ", " );
      }
      call.append( param.getSimpleName() );
    }
    call.append( ");\n" );
    srcMethod.body( new SrcStatementBlock()
                      .addStatement(
                        new SrcRawStatement()
                          .rawText( call.toString() ) ) );
  }

  private boolean warnIfDuplicate( AbstractSrcMethod method, SrcClass extendedType, DiagnosticListener<JavaFileObject> errorHandler, JavacTaskImpl javacTask )
  {
    AbstractSrcMethod duplicate = findMethod( method, extendedType, new JavacTaskImpl[]{javacTask} );

    if( duplicate == null )
    {
      return false;
    }

    JavacElements elems = JavacElements.instance( javacTask.getContext() );
    Symbol.ClassSymbol sym = elems.getTypeElement( ((SrcClass)method.getOwner()).getName() );
    JavaFileObject file = sym.sourcefile;
    SrcAnnotationExpression anno = duplicate.getAnnotation( ExtensionMethod.class );
    if( anno != null )
    {
      errorHandler.report( new JavacDiagnostic( file.toUri().getScheme() == null ? null : new SourceJavaFileObject( file.toUri() ),
                                                Diagnostic.Kind.WARNING, 0, 0, 0, ExtIssueMsg.MSG_EXTENSION_DUPLICATION.get( method.signature(), ((SrcClass)method.getOwner()).getName(), anno.getArgument( ExtensionMethod.extensionClass ).getValue()) ) );
    }
    else
    {
      errorHandler.report( new JavacDiagnostic( file.toUri().getScheme() == null ? null : new SourceJavaFileObject( file.toUri() ),
                                                Diagnostic.Kind.WARNING, 0, 0, 0, ExtIssueMsg.MSG_EXTENSION_SHADOWS.get( method.signature(), ((SrcClass)method.getOwner()).getName(), extendedType.getName()) ) );
    }
    return true;
  }

  private AbstractSrcMethod findMethod( AbstractSrcMethod method, SrcClass extendedType, JavacTaskImpl[] javacTask )
  {
    AbstractSrcMethod duplicate = null;
    outer:
    for( AbstractSrcMethod m: extendedType.getMethods() )
    {
      if( m.getSimpleName().equals( method.getSimpleName() ) && m.getParameters().size() == method.getParameters().size()-1 )
      {
        List parameters = method.getParameters();
        List params = m.getParameters();
        for( int i = 1; i < parameters.size(); i++ )
        {
          SrcParameter param = (SrcParameter)parameters.get( i );
          SrcParameter p = (SrcParameter)params.get( i-1 );
          if( !param.getType().equals( p.getType() ) )
          {
            continue outer;
          }
        }
        duplicate = m;
        break;
      }
    }
    if( duplicate == null )
    {
      if( !extendedType.isInterface() )
      {
        SrcType superClass = extendedType.getSuperClass();
        if( superClass != null && superClass.getName().equals( Object.class.getName() ) )
        {
          SrcClass superSrcClass = ClassSymbols.instance( getModule() ).makeSrcClassStub( superClass.getName(), javacTask, null );
          duplicate = findMethod( method, superSrcClass, javacTask );
        }
      }
      if( duplicate == null )
      {
        //## note: we are checking interfaces even for a non-abstract class because it could be
        //## inheriting default interface methods, which must not be shadowed by an extension.
        for( SrcType iface: extendedType.getInterfaces() )
        {
          SrcClass superIface = ClassSymbols.instance( getModule() ).makeSrcClassStub( iface.getName(), javacTask, null );
          duplicate = findMethod( method, superIface, javacTask );
          if( duplicate != null )
          {
            break;
          }
        }
      }
    }
    return duplicate;
  }

  private boolean isExtensionMethod( AbstractSrcMethod method, SrcClass extendedType )
  {
    if( !Modifier.isStatic( (int)method.getModifiers() ) || Modifier.isPrivate( (int)method.getModifiers() ) )
    {
      return false;
    }

    if( method.hasAnnotation( Extension.class ) )
    {
      return true;
    }

    return hasThisAnnotation( method, extendedType );
  }
  private boolean isInstanceExtensionMethod( AbstractSrcMethod method, SrcClass extendedType )
  {
    if( !Modifier.isStatic( (int)method.getModifiers() ) || Modifier.isPrivate( (int)method.getModifiers() ) )
    {
      return false;
    }

    return hasThisAnnotation( method, extendedType );
  }

  private boolean hasThisAnnotation( AbstractSrcMethod method, SrcClass extendedType )
  {
    List params = method.getParameters();
    if( params.size() == 0 )
    {
      return false;
    }
    SrcParameter param = (SrcParameter)params.get( 0 );
    if( !param.hasAnnotation( This.class ) )
    {
      return false;
    }
    // checking only for simple name for cases where the name cannot be resolved yet e.g., extension method on another source producer type
    return param.getType().getName().endsWith( extendedType.getSimpleName() );
  }

//  private Symbol.MethodSymbol resolveMethod( Context ctx, JCDiagnostic.DiagnosticPosition pos, Name name, Type qual, com.sun.tools.javac.util.List<Type> args )
//  {
//    Resolve rs = Resolve.instance( ctx );
//    AttrContext attrContext = new AttrContext();
//    Env<AttrContext> env = new AttrContextEnv( pos.getTree(), attrContext );
//    env.toplevel = _tp.getCompilationUnit();
//    return rs.resolveInternalMethod( pos, env, qual, name, args, null );
//  }
}
