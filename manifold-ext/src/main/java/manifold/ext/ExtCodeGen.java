package manifold.ext;

import com.sun.tools.javac.api.JavacTaskImpl;
import com.sun.tools.javac.code.Flags;
import com.sun.tools.javac.code.Symbol;
import java.lang.reflect.Modifier;
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
import manifold.ext.api.ExtensionMethod;
import manifold.ext.api.This;
import manifold.internal.javac.ClassSymbols;

/**
 */
class ExtCodeGen
{
  private final Model _model;
  private final String _fqn;

  ExtCodeGen( Model model, String topLevelFqn )
  {
    _model = model;
    _fqn = topLevelFqn;
  }

  private IModule getModule()
  {
    return _model.getSourceProducer().getTypeLoader().getModule();
  }

  SrcClass make( DiagnosticListener<JavaFileObject> errorHandler )
  {
    SrcClass srcClass = ClassSymbols.instance( getModule() ).makeSrcClassStub( _fqn );
    addExtensionMethods( srcClass, errorHandler );
    return srcClass;
  }

  private void addExtensionMethods( SrcClass extendedClass, DiagnosticListener<JavaFileObject> errorHandler )
  {
    Set<String> allExtensions = findAllExtensions();
    for( String fqn : allExtensions )
    {
      JavacTaskImpl[] javacTask = new JavacTaskImpl[1];
      SrcClass extensionClass = ClassSymbols.instance( getModule() ).makeSrcClassStub( fqn, javacTask );
      if( extensionClass != null )
      {
        for( AbstractSrcMethod method : extensionClass.getMethods() )
        {
          addExtensionMethod( method, extendedClass, errorHandler, javacTask[0] );
        }
      }
    }
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

  private void addExtensionMethod( AbstractSrcMethod method, SrcClass extendedType, DiagnosticListener<JavaFileObject> errorHandler, JavacTaskImpl javacTask )
  {
    if( !isExtensionMethod( method, extendedType ) )
    {
      return;
    }

    Symbol.ClassSymbol extendedClassSym = ClassSymbols.instance( getModule() ).getClassSymbol( javacTask, extendedType.getName() );
    if( !verifyExtensionMethod( method, extendedClassSym, errorHandler, javacTask ) )
    {
      return;
    }

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

    // mark as extension method for efficient lookup during method call replacement
    srcMethod.annotation(
      new SrcAnnotationExpression( ExtensionMethod.class )
        .addArgument( "extensionClass", String.class, ((SrcClass)method.getOwner()).getName() ) );
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

    srcMethod.body( new SrcStatementBlock()
                      .addStatement(
                        new SrcRawStatement()
                          .rawText( "throw new RuntimeException();" ) ) );

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
    if( annotations.size() > 0 && annotations.get( 0 ).getType().equals( This.class.getName() ) )
    {
      return false;
    }
    return extendedType.getName().equals( param.getType().getName() );
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
}
