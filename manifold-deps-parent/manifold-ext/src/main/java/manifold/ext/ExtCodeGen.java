/*
 * Copyright (c) 2018 - Manifold Systems LLC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package manifold.ext;

import com.sun.source.tree.CompilationUnitTree;
import com.sun.tools.internal.jxc.gen.config.Classes;
import com.sun.tools.javac.code.Flags;
import com.sun.tools.javac.code.Symbol;
import com.sun.tools.javac.code.Type;
import com.sun.tools.javac.tree.JCTree;
import manifold.ExtIssueMsg;
import manifold.api.fs.IFile;
import manifold.api.fs.cache.PathCache;
import manifold.api.gen.*;
import manifold.api.host.IModule;
import manifold.api.type.ITypeManifold;
import manifold.api.util.JavacDiagnostic;
import manifold.api.util.JavacUtil;
import manifold.ext.rt.ExtensionMethod;
import manifold.ext.rt.ForwardingExtensionMethod;
import manifold.ext.rt.api.Expires;
import manifold.ext.rt.api.Extension;
import manifold.ext.rt.api.This;
import manifold.ext.rt.api.ThisClass;
import manifold.internal.javac.ClassSymbols;
import manifold.internal.javac.JavacPlugin;
import manifold.rt.api.Array;
import manifold.rt.api.util.Pair;
import manifold.util.JreUtil;

import javax.tools.Diagnostic;
import javax.tools.DiagnosticListener;
import javax.tools.JavaFileManager;
import javax.tools.JavaFileObject;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.*;
import java.util.stream.Collectors;

import static manifold.ext.ExtensionManifold.EXTENSIONS_PACKAGE;

/**
 */
class ExtCodeGen
{
  public static final String GENERATEDPROXY_ = "generatedproxy_";
  public static final String OF_ = "_Of_";
  public static final String TO_ = "_To_";

  private JavaFileManager.Location _location;
  private final Model _model;
  private final String _fqn;
  private final boolean _genStubs;
  private String _existingSource;

  ExtCodeGen( JavaFileManager.Location location, Model model, String topLevelFqn, boolean genStubs, String existingSource )
  {
    _location = location;
    _model = model;
    _fqn = topLevelFqn;
    _genStubs = genStubs;
    _existingSource = existingSource;
  }

  private IModule getModule()
  {
    return _model.getTypeManifold().getModule();
  }

  String make( JavaFileManager.Location location, DiagnosticListener<JavaFileObject> errorHandler )
  {
    if( isProxyFactory() )
    {
      // auto-generate a proxy factory for interfaces the extension class implements
      return generateProxyFactory();
    }

    SrcClass srcExtended;
    if( !_existingSource.isEmpty() )
    {
      srcExtended = makeStubFromSource();
    }
    else
    {
      srcExtended = ClassSymbols.instance( getModule() ).makeSrcClassStub( _fqn, location, errorHandler );
      srcExtended.setBinary( true );
    }
    return addExtensions( srcExtended, errorHandler );
  }

  private boolean isProxyFactory()
  {
    return _fqn.contains( GENERATEDPROXY_ ) && _fqn.contains( OF_ ) && _fqn.contains( TO_ );
  }

  private String generateProxyFactory()
  {
    int genIndex = _fqn.indexOf( GENERATEDPROXY_ );
    String pkg = _fqn.substring( 0, genIndex-1 );
    String name = _fqn.substring( genIndex );
    genIndex += GENERATEDPROXY_.length();
    int ofIndex = _fqn.indexOf( OF_, genIndex );
    String baseExtensionName = _fqn.substring( genIndex, ofIndex );
    String extensionFqn = pkg + '.' + baseExtensionName;

    Symbol.ClassSymbol extensionSym = getExtensionSym( extensionFqn );

    Type ifaceType = getInterfaceType( ofIndex, extensionSym );

    Symbol.ClassSymbol extendedSym = getExtendedSym( pkg );

    Pair<String, String> fqnToCode = StaticStructuralTypeProxyGenerator.makeProxy( name, ifaceType, extendedSym, pkg, getModule() );
    return fqnToCode.getSecond();
  }

  private Symbol.ClassSymbol getExtendedSym( String pkg )
  {
    int extentionsIndex = pkg.indexOf( EXTENSIONS_PACKAGE + '.' );
    if( extentionsIndex < 0 )
    {
      throw new IllegalStateException( "'extensions' package missing from extension auto proxy name" );
    }
    String extendedType = pkg.substring( extentionsIndex + EXTENSIONS_PACKAGE.length() + 1 );
    Pair<Symbol.ClassSymbol, JCTree.JCCompilationUnit> pair2 = ClassSymbols.instance( getModule() )
      .getClassSymbol( JavacPlugin.instance().getJavacTask(), extendedType );
    if( pair2 == null || pair2.getFirst() == null )
    {
      throw new IllegalStateException( "Failed to load extended type ClassSymbol for proxy: " + _fqn );
    }
    Symbol.ClassSymbol extendedSym = pair2.getFirst();
    return extendedSym;
  }

  private Type getInterfaceType( int ofIndex, Symbol.ClassSymbol extensionSym )
  {
    int toIndex = _fqn.indexOf( TO_, ofIndex );
    String baseIfaceName = _fqn.substring( toIndex + TO_.length() );

    Type ifaceType = null;
    for( Type csr: extensionSym.getInterfaces() )
    {
      if( csr.tsym.getSimpleName().toString().equals( baseIfaceName ) )
      {
        ifaceType = csr;
        break;
      }
    }
    if( ifaceType == null )
    {
      throw new IllegalStateException( "Failed to load implemented interface ClassSymbol for proxy: " + _fqn );
    }
    return ifaceType;
  }

  private Symbol.ClassSymbol getExtensionSym( String extensionFqn )
  {
    Pair<Symbol.ClassSymbol, JCTree.JCCompilationUnit> pair = ClassSymbols.instance( getModule() )
      .getClassSymbol( JavacPlugin.instance().getJavacTask(), extensionFqn );
    if( pair == null || pair.getFirst() == null )
    {
      throw new IllegalStateException( "Failed to get extension class symbol: " + extensionFqn );
    }
    Symbol.ClassSymbol extensionSym = pair.getFirst();
    return extensionSym;
  }

  private SrcClass makeStubFromSource()
  {
    List<CompilationUnitTree> trees = new ArrayList<>();
    _model.getHost().getJavaParser().parseText( _existingSource, trees, null, null, null );
    JCTree.JCClassDecl classDecl = (JCTree.JCClassDecl)trees.get( 0 ).getTypeDecls().get( 0 );
    SrcClass srcExtended = new SrcClass( _fqn, SrcClass.Kind.from( classDecl.getKind() ) )
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
    List<String> sourceClassFqns = new ArrayList<>();
    Set<String> allExtensions = findAllExtensions();
    _model.pushProcessing( _fqn );
    try
    {
      for( Iterator<String> iterator = allExtensions.iterator(); iterator.hasNext(); )
      {
        String extensionFqn = iterator.next();
        SrcClass srcExtension = ClassSymbols.instance( getModule() ).makeSrcClassStub( extensionFqn ); // _location );
        if( srcExtension != null )
        {
          for( AbstractSrcMethod method: srcExtension.getMethods() )
          {
            addExtensionMethod( method, extendedClass, errorHandler );
            methodExtensions = true;
          }
          for( SrcType iface: srcExtension.getInterfaces() )
          {
            addExtensionInteface( iface, extendedClass );
            interfaceExtensions = true;
          }
          for( SrcAnnotationExpression anno: srcExtension.getAnnotations() )
          {
            addExtensionAnnotation( anno, extendedClass );
            annotationExtensions = true;
            if( anno.getAnnotationType().equals( Extension.class.getName() ) ) {
              SrcArgument sourceClassesArg = anno.getArgument( "sources" );
              if( sourceClassesArg != null ) {
                String[] sourceClassFqnsSplit = sourceClassesArg.getValue().toString()
                    .replace("{", "").replace("}", "").split(",");
                for ( String sourceClassFqn : sourceClassFqnsSplit ) {
                  sourceClassFqns.add( sourceClassFqn.substring( 0, sourceClassFqn.lastIndexOf(".class") ).trim() );
                }
              }
            }
          }
        }
        else
        {
          iterator.remove();
        }
      }
      for(String sourceClassFqn : sourceClassFqns ) {
        SrcClass srcClass = ClassSymbols.instance( getModule() ).makeSrcClassStub( sourceClassFqn );
        for ( AbstractSrcMethod<?> method : srcClass.getMethods() ) {
          if ( !method.getParameters().isEmpty() && method.getParameters().get( 0 ).getType().getFqName().equals( _fqn ) ) {
            // Mark first param with @This annotation, so it is handled as an extension method
            method.getParameters().get(0).addAnnotation(This.class);
            AbstractSrcMethod duplicate = findMethod(method, extendedClass);
            if (duplicate == null) {
              addExtensionMethod(method, extendedClass, errorHandler);
            }
          }
        }
      }
      if( !_existingSource.isEmpty() )
      {
        if( allExtensions.isEmpty() )
        {
          return _existingSource;
        }
        return addExtensionsToExistingClass( extendedClass, methodExtensions, interfaceExtensions, annotationExtensions );
      }
      return extendedClass.render( new StringBuilder(), 0 ).toString();
    }
    finally
    {
      _model.popProcessing( _fqn );
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
    String start = (srcClass.isInterface()
      ? "interface "
      : srcClass.getKind() == AbstractSrcClass.Kind.Record
        ? "record "
        : "class ") + srcClass.getSimpleName();
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

    String start = (srcClass.isInterface()
      ? "interface "
      : srcClass.getKind() == AbstractSrcClass.Kind.Record
      ? "record "
      : "class ") + srcClass.getSimpleName();

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
    sb.append( _existingSource, 0, iBrace );
    for( AbstractSrcMethod method : srcClass.getMethods() )
    {
      method.render( sb, 2 );
    }
    sb.append( "\n}" );
  }

  private Set<String> findAllExtensions()
  {
    if( _model.isProcessing( _fqn ) )
    {
      // short-circuit e.g., extension producers
      return Collections.emptySet();
    }

    Set<String> fqns = new LinkedHashSet<>();
    findExtensionsOnDisk( fqns );
    findExtensionsFromExtensionClassProviders( fqns );
    return fqns;
  }

  private void findExtensionsOnDisk( Set<String> fqns )
  {
    PathCache pathCache = getModule().getPathCache();
    for( IFile file : _model.getFiles() )
    {
      Set<String> fqn = pathCache.getFqnForFile( file );
      for( String f : fqn )
      {
        if( f != null )
        {
          String innerExtFqn = findInnerClassInExtension( f );
          fqns.add( innerExtFqn );
        }
      }
    }
  }

  private String findInnerClassInExtension( String extensionFqn )
  {
    String toplevel = _model.getFqn();
    if( toplevel.length() == _fqn.length() )
    {
      return extensionFqn;
    }

    int index = _fqn.indexOf( toplevel );
    if( index >= 0 )
    {
      return extensionFqn + _fqn.substring( toplevel.length() );
    }
    return extensionFqn;
  }

  private void findExtensionsFromExtensionClassProviders( Set<String> fqns )
  {
    ExtensionManifold extensionManifold = _model.getTypeManifold();
    for( ITypeManifold tm: extensionManifold.getModule().getTypeManifolds() )
    {
      if( tm != extensionManifold &&
          tm instanceof IExtensionClassProducer )
      {
        Set<String> extensionClasses = ((IExtensionClassProducer)tm).getExtensionClasses( _model.getFqn() );
        extensionClasses = extensionClasses.stream().map( e -> findInnerClassInExtension( e ) ).collect( Collectors.toSet() );
        fqns.addAll( extensionClasses );
      }
    }
  }

  private void addExtensionInteface( SrcType iface, SrcClass extendedType )
  {
    extendedType.addInterface( iface );
  }

  private void addExtensionAnnotation( SrcAnnotationExpression anno, SrcClass extendedType )
  {
    if( anno.getAnnotationType().equals( Extension.class.getName() ) )
    {
      return;
    }

    if( extendedType.getAnnotations().stream().noneMatch( e -> e.getAnnotationType().equals( anno.getAnnotationType() ) ) )
    {
      extendedType.addAnnotation( anno.copy() );
    }
  }

  private void addExtensionMethod( AbstractSrcMethod method, SrcClass extendedType, DiagnosticListener<JavaFileObject> errorHandler )
  {
    if( !isExtensionMethod( method, extendedType ) )
    {
      return;
    }

    if( warnIfDuplicate( method, extendedType, errorHandler ) )
    {
      return;
    }

    // the class is a produced class, therefore we must delegate the calls since calls are not replaced
    boolean delegateCalls = !_existingSource.isEmpty() && !_genStubs;

    boolean isInstanceExtensionMethod = isInstanceExtensionMethod( method, extendedType );

    SrcMethod srcMethod = new SrcMethod( extendedType );
    long modifiers = method.getModifiers();
    if( extendedType.isInterface() && isInstanceExtensionMethod )
    {
      // extension method must be default method in interface to not require implementation
      modifiers |= Flags.DEFAULT;
    }

//## Don't mark extension methods on classes as final, it otherwise blocks extended classes from implementing an interface with the same method signature
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
          .addArgument( "isStatic", boolean.class, !isInstanceExtensionMethod )
          .addArgument( "isSmartStatic", boolean.class, hasThisClassAnnotation( method ) ) );
    }
    else
    {
      srcMethod.addAnnotation( new SrcAnnotationExpression( ForwardingExtensionMethod.class ) );
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

    // exclude @This param
    int firstParam = isInstanceExtensionMethod || hasThisClassAnnotation( method ) ? 1 : 0;

    for( int i = firstParam; i < params.size(); i++ )
    {
      SrcParameter param = (SrcParameter)params.get( i );
      SrcParameter p = new SrcParameter( param.getSimpleName(), param.getType() );
      for( SrcAnnotationExpression anno: param.getAnnotations() )
      {
        // ensure annotations on parameters such as @NotNull map correctly
        p.addAnnotation( anno );
      }
      srcMethod.addParam( p );
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
    else if( hasThisClassAnnotation( method ) )
    {
      //todo @ThisClass does NOT work in delegation mode, this will fail
      call.append( srcMethod.getOwner().getSimpleName() ).append( ".class" );
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

  private boolean warnIfDuplicate( AbstractSrcMethod method, SrcClass extendedType, DiagnosticListener<JavaFileObject> errorHandler )
  {
    AbstractSrcMethod duplicate = findMethod( method, extendedType );

    if( duplicate == null )
    {
      return false;
    }

    if( extendedType.getName().equals( Array.class.getTypeName() ) &&
      ((SrcClass)duplicate.getOwner()).getName().equals( Object.class.getTypeName() ) )
    {
      // to support shadowing Object#equals() etc. on arrays
      return false;
    }

    SrcAnnotationExpression anno = duplicate.getAnnotation( ExtensionMethod.class );
    if( anno != null )
    {
      errorHandler.report( new JavacDiagnostic( null, Diagnostic.Kind.WARNING, 0, 0, 0,
        ExtIssueMsg.MSG_EXTENSION_DUPLICATION.get( method.signature(), ((SrcClass)method.getOwner()).getName(), anno.getArgument( ExtensionMethod.extensionClass ).getValue()) ) );
    }
    else
    {
      errorHandler.report( new JavacDiagnostic( null, Diagnostic.Kind.WARNING, 0, 0, 0,
        ExtIssueMsg.MSG_EXTENSION_SHADOWS.get( method.signature(), ((SrcClass)method.getOwner()).getName(), extendedType.getName()) ) );
    }

    return true;
  }

  private AbstractSrcMethod findMethod( AbstractSrcMethod method, SrcClass extendedType )
  {
    if( extendedType == null )
    {
      return null;
    }

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
          SrcClass superSrcClass = ClassSymbols.instance( getModule() ).makeSrcClassStub( superClass.getName() );
          duplicate = findMethod( method, superSrcClass );
        }
      }
      if( duplicate == null )
      {
        //## note: we are checking interfaces even for a non-abstract class because it could be
        //## inheriting default interface methods, which must not be shadowed by an extension.
        for( SrcType iface: extendedType.getInterfaces() )
        {
          SrcClass superIface = ClassSymbols.instance( getModule() ).makeSrcClassStub( iface.getName() );
          duplicate = findMethod( method, superIface );
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

    SrcAnnotationExpression expires = method.getAnnotation( Expires.class );
    if( expires != null &&
      JavacUtil.getReleaseNumber() >= Integer.parseInt( expires.getArgument( "value" ).getValue().toString() ) )
    {
      // on or past the method's expiration jdk
      return false;
    }

    //noinspection SimplifiableIfStatement
    if( method.hasAnnotation( Extension.class ) )
    {
      return true;
    }

    return hasThisAnnotation( method, extendedType ) || hasThisClassAnnotation( method );
  }
  private boolean isInstanceExtensionMethod( AbstractSrcMethod method, SrcClass extendedType )
  {
    //noinspection SimplifiableIfStatement
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
    return param.getType().getName().endsWith( extendedType.getSimpleName() ) ||
      isArrayExtension( param, extendedType );
  }
  private boolean hasThisClassAnnotation( AbstractSrcMethod method )
  {
    List params = method.getParameters();
    if( params.size() == 0 )
    {
      return false;
    }
    SrcParameter param = (SrcParameter)params.get( 0 );
    if( !param.hasAnnotation( ThisClass.class ) )
    {
      return false;
    }
    // checking only for simple name for cases where the name cannot be resolved yet e.g., extension method on another source producer type
    return param.getType().getName().endsWith( "Class" ) || param.getType().getName().contains( "Class<" );
  }

  private boolean isArrayExtension( SrcParameter param, SrcClass extendedType )
  {
    return extendedType.getName().equals( "manifold.rt.api.Array" ) &&
      param.getType().getFqName().equals( Object.class.getTypeName() );
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
