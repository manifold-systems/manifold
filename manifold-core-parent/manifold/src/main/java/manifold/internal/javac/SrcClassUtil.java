package manifold.internal.javac;

import com.sun.source.tree.CompilationUnitTree;
import com.sun.source.tree.ImportTree;
import com.sun.tools.javac.api.BasicJavacTask;
import com.sun.tools.javac.code.Attribute;
import com.sun.tools.javac.code.Flags;
import com.sun.tools.javac.code.Symbol;
import com.sun.tools.javac.code.Type;
import com.sun.tools.javac.tree.JCTree;
import com.sun.tools.javac.util.List;
import com.sun.tools.javac.util.Pair;
import java.lang.reflect.Modifier;
import javax.lang.model.type.NoType;
import javax.lang.model.type.NullType;
import manifold.api.gen.SrcAnnotated;
import manifold.api.gen.SrcAnnotationExpression;
import manifold.api.gen.SrcClass;
import manifold.api.gen.SrcField;
import manifold.api.gen.SrcMethod;
import manifold.api.gen.SrcParameter;
import manifold.api.gen.SrcRawExpression;
import manifold.api.gen.SrcRawStatement;
import manifold.api.gen.SrcStatementBlock;
import manifold.api.gen.SrcType;
import manifold.api.host.IModule;

/**
 */
public class SrcClassUtil
{
  private static final SrcClassUtil INSTANCE = new SrcClassUtil();

  private SrcClassUtil()
  {
  }

  public static SrcClassUtil instance()
  {
    return INSTANCE;
  }

  public SrcClass makeStub( IModule module, String fqn, Symbol.ClassSymbol classSymbol, CompilationUnitTree compilationUnit, BasicJavacTask javacTask )
  {
    return makeStub( module, fqn, classSymbol, compilationUnit, javacTask, true );
  }

  public SrcClass makeStub( IModule module, String fqn, Symbol.ClassSymbol classSymbol, CompilationUnitTree compilationUnit, BasicJavacTask javacTask, boolean withMembers )
  {
    return makeSrcClass( module, fqn, classSymbol, compilationUnit, javacTask, withMembers );
  }

  private SrcClass makeSrcClass( IModule module, String fqn, Symbol.ClassSymbol classSymbol, CompilationUnitTree compilationUnit, BasicJavacTask javacTask, boolean withMembers )
  {
    SrcClass srcClass = new SrcClass( fqn, SrcClass.Kind.from( classSymbol.getKind() ) )
      .modifiers( classSymbol.getModifiers() );
    if( classSymbol.getEnclosingElement() instanceof Symbol.PackageSymbol && compilationUnit != null )
    {
      for( ImportTree imp : compilationUnit.getImports() )
      {
        if( imp.isStatic() )
        {
          srcClass.addStaticImport( imp.getQualifiedIdentifier().toString() );
        }
        else
        {
          srcClass.addImport( imp.getQualifiedIdentifier().toString() );
        }
      }
    }
    addAnnotations( srcClass, classSymbol );
    for( Symbol.TypeVariableSymbol typeVar : classSymbol.getTypeParameters() )
    {
      srcClass.addTypeVar( makeTypeVarType( typeVar ) );
    }
    Type superclass = classSymbol.getSuperclass();
    if( !(superclass instanceof NoType) )
    {
      srcClass.superClass( makeNestedType( superclass ) );
    }
    for( Type iface : classSymbol.getInterfaces() )
    {
      srcClass.addInterface( makeNestedType( iface ) );
    }
    if( withMembers )
    {
      for( Symbol sym : classSymbol.getEnclosedElements() )
      {
        long modifiers = SrcAnnotated.modifiersFrom( sym.getModifiers() );
        if( Modifier.isPrivate( (int)modifiers ) )
        {
          continue;
        }

        if( sym instanceof Symbol.ClassSymbol )
        {
          addInnerClass( module, srcClass, sym, javacTask );
        }
        else if( sym instanceof Symbol.VarSymbol )
        {
          addField( srcClass, sym );
        }
        else if( sym instanceof Symbol.MethodSymbol )
        {
          if( !isEnumMethod( sym ) )
          {
            addMethod( module, srcClass, (Symbol.MethodSymbol)sym, javacTask );
          }
        }
      }
    }
    return srcClass;
  }

  private boolean isEnumMethod( Symbol sym )
  {
    return sym.getEnclosingElement().isEnum() &&
           (sym.toString().equals( "values()" ) || sym.toString().equals( "valueOf(java.lang.String)" ));
  }

  private SrcType makeNestedType( Type type )
  {
    String fqn = type.toString();
    Type enclosingType = type.getEnclosingType();
    SrcType srcType;
    if( enclosingType != null && !(enclosingType instanceof NoType) && fqn.length() > enclosingType.toString().length() )
    {
      String simpleName = fqn.substring( enclosingType.toString().length() + 1 );
      srcType = new SrcType( simpleName );
      srcType.setEnclosingType( makeNestedType( enclosingType ) );
    }
    else
    {
      srcType = new SrcType( fqn );
    }
    return srcType;
  }

  private void addInnerClass( IModule module, SrcClass srcClass, Symbol sym, BasicJavacTask javacTask )
  {
    SrcClass innerClass = makeSrcClass( module, sym.getQualifiedName().toString(), (Symbol.ClassSymbol)sym, null, javacTask, true );
    srcClass.addInnerClass( innerClass );
  }

  private void addField( SrcClass srcClass, Symbol sym )
  {
    Symbol.VarSymbol field = (Symbol.VarSymbol)sym;
    SrcField srcField = new SrcField( field.name.toString(), new SrcType( field.type.toString() ) );
    if( sym.isEnum() )
    {
      srcField.enumConst();
    }
    else
    {
      srcField.modifiers( field.getModifiers() );
      if( Modifier.isFinal( (int)srcField.getModifiers() ) )
      {
        srcField.initializer( new SrcRawExpression( getValueForType( sym.type ) ) );
      }
    }
    srcClass.addField( srcField );
  }

  private void addMethod( IModule module, SrcClass srcClass, Symbol.MethodSymbol method, BasicJavacTask javacTask )
  {
    String name = method.flatName().toString();
    SrcMethod srcMethod = new SrcMethod( srcClass, name.equals( "<init>" ) );
    addAnnotations( srcMethod, method );
    srcMethod.modifiers( method.getModifiers() );
    if( (method.flags() & Flags.VARARGS) != 0 )
    {
      srcMethod.modifiers( srcMethod.getModifiers() | 0x00000080 ); // Modifier.VARARGS
    }
    if( name.equals( "<clinit>" ) )
    {
      return;
    }
    if( !srcMethod.isConstructor() )
    {
      srcMethod.name( name );
      srcMethod.returns( new SrcType( method.getReturnType().toString() ) );
    }
    for( Symbol.TypeVariableSymbol typeVar : method.getTypeParameters() )
    {
      srcMethod.addTypeVar( makeTypeVarType( typeVar ) );
    }
    for( Symbol.VarSymbol param : method.getParameters() )
    {
      SrcParameter srcParam = new SrcParameter( param.flatName().toString(), new SrcType( param.type.toString() ) );
      srcMethod.addParam( srcParam );
      addAnnotations( srcParam, param );
    }
    for( Type throwType : method.getThrownTypes() )
    {
      srcMethod.addThrowType( new SrcType( throwType.toString() ) );
    }
    String bodyStmt;
    if( srcMethod.isConstructor() )
    {
      // Note we can't just throw an exception for the ctor body, the compiler will
      // still complain about the missing super() call if the super class does not have
      // an accessible default ctor. To appease the compiler we generate a super(...)
      // call to the first accessible constructor we can find in the super class.
      bodyStmt = genSuperCtorCall( module, srcClass, javacTask );
    }
    else
    {
      bodyStmt = "throw new RuntimeException();";
    }
    srcMethod.body( new SrcStatementBlock()
                      .addStatement(
                        new SrcRawStatement()
                          .rawText( bodyStmt ) ) );
    srcClass.addMethod( srcMethod );
  }

  private String genSuperCtorCall( IModule module, SrcClass srcClass, BasicJavacTask javacTask )
  {
    String bodyStmt;SrcType superClass = srcClass.getSuperClass();
    if( superClass == null )
    {
      bodyStmt = "";
    }
    else
    {
      Symbol.MethodSymbol superCtor = findConstructor( module, superClass.getFqName(), javacTask );
      if( superCtor == null )
      {
        bodyStmt = "";
      }
      else
      {
        bodyStmt = genSuperCtorCall( superCtor );
      }
    }
    return bodyStmt;
  }

  private String genSuperCtorCall( Symbol.MethodSymbol superCtor )
  {
    String bodyStmt;
    StringBuilder sb = new StringBuilder( "super(" );
    List<Symbol.VarSymbol> parameters = superCtor.getParameters();
    for( int i = 0; i < parameters.size(); i++ )
    {
      Symbol.VarSymbol param = parameters.get( i );
      if( i > 0 )
      {
        sb.append( ", " );
      }
      sb.append( getValueForType( param.type ) );
    }
    sb.append( ");" );
    bodyStmt = sb.toString();
    return bodyStmt;
  }

  private Symbol.MethodSymbol findConstructor( IModule module, String fqn, BasicJavacTask javacTask )
  {
    manifold.util.Pair<Symbol.ClassSymbol, JCTree.JCCompilationUnit> classSymbol = ClassSymbols.instance( module ).getClassSymbol( javacTask, fqn );
    Symbol.ClassSymbol cs = classSymbol.getFirst();
    Symbol.MethodSymbol ctor = null;
    for( Symbol sym : cs.getEnclosedElements() )
    {
      if( sym instanceof Symbol.MethodSymbol && sym.flatName().toString().equals( "<init>" ) )
      {
        if( ctor == null )
        {
          ctor = (Symbol.MethodSymbol)sym;
        }
        else
        {
          ctor = mostAccessible( ctor, (Symbol.MethodSymbol)sym );
        }
        if( Modifier.isPublic( (int)ctor.flags() ) )
        {
          return ctor;
        }
      }
    }
    return ctor;
  }

  private Symbol.MethodSymbol mostAccessible( Symbol.MethodSymbol ctor, Symbol.MethodSymbol sym )
  {
    int ctorMods = (int)ctor.flags();
    int symMods = (int)sym.flags();
    if( Modifier.isPublic( ctorMods ) )
    {
      return ctor;
    }
    if( Modifier.isPublic( symMods ) )
    {
      return sym;
    }
    if( Modifier.isProtected( ctorMods ) )
    {
      return ctor;
    }
    if( Modifier.isProtected( symMods ) )
    {
      return sym;
    }
    if( Modifier.isPrivate( ctorMods ) )
    {
      return Modifier.isPrivate( symMods ) ? ctor : sym;
    }
    return ctor;
  }

  private void addAnnotations( SrcAnnotated<?> srcAnnotated, Symbol symbol )
  {
    for( Attribute.Compound annotationMirror : symbol.getAnnotationMirrors() )
    {
      String fqn = annotationMirror.getAnnotationType().toString();
      if( fqn.equals( "jdk.internal.HotSpotIntrinsicCandidate" ) )
      {
        // Since java 10 we have to keep these out of stubbed java source
        continue;
      }
      SrcAnnotationExpression annoExpr = new SrcAnnotationExpression( fqn );
      for( Pair<Symbol.MethodSymbol, Attribute> value : annotationMirror.values )
      {
        annoExpr.addArgument( value.fst.flatName().toString(), new SrcType( value.snd.type.toString() ), value.snd.getValue() );
      }
      srcAnnotated.addAnnotation( annoExpr );
    }
  }

  private SrcType makeTypeVarType( Symbol.TypeVariableSymbol typeVar )
  {
    StringBuilder sb = new StringBuilder( typeVar.type.toString() );
    Type lowerBound = typeVar.type.getLowerBound();
    if( lowerBound != null && !(lowerBound instanceof NullType) )
    {
      sb.append( " super " ).append( lowerBound.toString() );
    }
    else
    {
      Type upperBound = typeVar.type.getUpperBound();
      if( upperBound != null && !(upperBound instanceof NoType) && !upperBound.toString().equals( Object.class.getName() ) )
      {
        sb.append( " extends " ).append( upperBound.toString() );
      }
    }
    return new SrcType( sb.toString() );
  }

  private String getValueForType( Type type )
  {
    if( type.toString().equals( "boolean" ) )
    {
      return "false";
    }
    else if( type.isPrimitive() )
    {
      return "0";
    }
    else
    {
      return "null";
    }
  }

}
