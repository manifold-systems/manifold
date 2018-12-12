package manifold.internal.javac;

import com.sun.tools.javac.api.JavacTrees;
import com.sun.tools.javac.code.Attribute;
import com.sun.tools.javac.code.Symbol;
import com.sun.tools.javac.code.SymbolMetadata;
import com.sun.tools.javac.code.Symtab;
import com.sun.tools.javac.code.TargetType;
import com.sun.tools.javac.code.Type;
import com.sun.tools.javac.code.TypeAnnotationPosition;
import com.sun.tools.javac.code.TypeAnnotationPosition.TypePathEntry;
import com.sun.tools.javac.code.TypeAnnotations;
import com.sun.tools.javac.comp.Annotate;
import com.sun.tools.javac.comp.Attr;
import com.sun.tools.javac.comp.AttrContext;
import com.sun.tools.javac.comp.Check;
import com.sun.tools.javac.comp.DeferredAttr;
import com.sun.tools.javac.comp.Env;
import com.sun.tools.javac.comp.Lower;
import com.sun.tools.javac.comp.MemberEnter;
import com.sun.tools.javac.comp.Resolve;
import com.sun.tools.javac.comp.TransTypes;
import com.sun.tools.javac.main.JavaCompiler;
import com.sun.tools.javac.tree.JCTree;
import com.sun.tools.javac.tree.TreeInfo;
import com.sun.tools.javac.util.Context;
import com.sun.tools.javac.util.List;
import com.sun.tools.javac.util.ListBuffer;
import com.sun.tools.javac.util.Name;
import com.sun.tools.javac.util.Names;
import java.util.ArrayList;
import manifold.util.ReflectUtil;
import manifold.util.Stack;


import static com.sun.tools.javac.code.Kinds.VAL;
import static com.sun.tools.javac.code.TypeTag.WILDCARD;
import static com.sun.tools.javac.tree.JCTree.Tag.SELECT;

public class ManAttr_8 extends Attr implements ManAttr
{
  private final Names _names;
  private final Symtab _syms;
  private final Check _chk;
  private final ManLog_8 _manLog;
  private Stack<JCTree.JCFieldAccess> _selects;
  private Stack<JCTree.JCAnnotatedType> _annotatedTypes;

  public static ManAttr_8 instance( Context ctx )
  {
    Attr attr = ctx.get( attrKey );
    if( !(attr instanceof ManAttr_8) )
    {
      ctx.put( attrKey, (Attr)null );
      attr = new ManAttr_8( ctx );
    }

    return (ManAttr_8)attr;
  }

  private ManAttr_8( Context ctx )
  {
    super( ctx );
    _names = Names.instance( ctx );
    _syms = Symtab.instance( ctx );
    _chk = Check.instance( ctx );
    _selects = new Stack<>();
    _annotatedTypes = new Stack<>();

    // Override logger to handle final field assignment for @Jailbreak
    _manLog = (ManLog_8)ManLog_8.instance( ctx );
    ReflectUtil.field( this, "log" ).set( _manLog );
    ReflectUtil.field( this, "rs" ).set( ManResolve.instance( ctx ) );
    reassignAllEarlyHolders( ctx );
  }

  private void reassignAllEarlyHolders( Context ctx )
  {
    Object[] earlyAttrHolders = {
      Resolve.instance( ctx ),
      DeferredAttr.instance( ctx ),
      MemberEnter.instance( ctx ),
      Lower.instance( ctx ),
      TransTypes.instance( ctx ),
      Annotate.instance( ctx ),
      TypeAnnotations.instance( ctx ),
      JavacTrees.instance( ctx ),
      JavaCompiler.instance( ctx ),
    };
    for( Object instance: earlyAttrHolders )
    {
      ReflectUtil.LiveFieldRef attr = ReflectUtil.WithNull.field( instance, "attr" );
      if( attr != null )
      {
        attr.set( this );
      }
    }
  }

  /**
   * Facilitates @Jailbreak. ManResolve#isAccessible() needs to know the JCFieldAccess in context.
   */
  @Override
  public void visitSelect( JCTree.JCFieldAccess tree )
  {
    // record JCFieldAccess trees as they are visited so we can access them elsewhere while in context
    _selects.push( tree );
    try
    {
      super.visitSelect( tree );
    }
    finally
    {
      _selects.pop();
    }
  }

  private boolean shouldCheckSuperType( Type type )
  {
    return _shouldCheckSuperType( type, true );
  }
  private boolean _shouldCheckSuperType( Type type, boolean checkSuper )
  {
    return
      type instanceof Type.ClassType &&
      type != Type.noType &&
      !(type instanceof Type.ErrorType) &&
      !type.toString().equals( Object.class.getTypeName() ) &&
      (!checkSuper || _shouldCheckSuperType( ((Symbol.ClassSymbol)type.tsym).getSuperclass(), false ));
  }

  /**
   * Facilitates @Jailbreak. ManResolve#isAccessible() needs to know the JCAnnotatedType in context.
   */
  @Override
  public void visitAnnotatedType( JCTree.JCAnnotatedType tree )
  {
    _annotatedTypes.push( tree );
    try
    {
      super.visitAnnotatedType( tree );
    }
    finally
    {
      _annotatedTypes.pop();
    }
  }

  public JCTree.JCFieldAccess peekSelect()
  {
    return _selects.isEmpty() ? null : _selects.peek();
  }
  public JCTree.JCAnnotatedType peekAnnotatedType()
  {
    return _annotatedTypes.isEmpty() ? null : _annotatedTypes.peek();
  }

  private ReflectUtil.MethodRef attribArgs = ReflectUtil.method( Attr.class, "attribArgs", int.class, List.class, Env.class, ListBuffer.class );
  private Class<?> ResultInfo = ReflectUtil.type( "com.sun.tools.javac.comp.Attr$ResultInfo" );
  private ReflectUtil.MethodRef attribTree = ReflectUtil.method( Attr.class, "attribTree", JCTree.class, Env.class, ResultInfo );
  private ReflectUtil.ConstructorRef resultInfoCtor = ReflectUtil.constructor( ResultInfo, Attr.class, int.class, Type.class, Check.CheckContext.class );
  private ReflectUtil.MethodRef adjustMethodReturnType = ReflectUtil.method( Attr.class, "adjustMethodReturnType", Type.class, Name.class, List.class, Type.class );
  private ReflectUtil.MethodRef checkRefTypes = ReflectUtil.method( Check.class, "checkRefTypes", List.class, List.class );
  private ReflectUtil.MethodRef capture = ReflectUtil.method( Attr.class, "capture", Type.class );
  private ReflectUtil.MethodRef check = ReflectUtil.method( Attr.class, "check", JCTree.class, Type.class, int.class, ResultInfo );
  private ReflectUtil.MethodRef validate = ReflectUtil.method( Check.class, "validate", List.class, Env.class );

  /**
   * Handles @Self, replaces expression type with self type
   */
  @Override
  public void visitApply( JCTree.JCMethodInvocation tree )
  {
    if( !(tree.meth instanceof JCTree.JCFieldAccess) )
    {
      _visitApply( tree );
      return;
    }

    if( JAILBREAK_PRIVATE_FROM_SUPERS )
    {
      _manLog.pushSuspendIssues( tree ); // since method-calls can be nested, we need a tree of stacks TreeNode(JCTree.JCFieldAccess, Stack<JCDiagnostic>>)
    }

    JCTree.JCFieldAccess fieldAccess = (JCTree.JCFieldAccess)tree.meth;
    try
    {
      _visitApply( tree );

      if( JAILBREAK_PRIVATE_FROM_SUPERS )
      {
        if( fieldAccess.type instanceof Type.ErrorType )
        {
          if( shouldCheckSuperType( fieldAccess.selected.type ) && _manLog.isJailbreakSelect( fieldAccess ) )
          {
            // set qualifier type to supertype to handle private methods
            Type.ClassType oldType = (Type.ClassType)fieldAccess.selected.type;
            fieldAccess.selected.type = ((Symbol.ClassSymbol)oldType.tsym).getSuperclass();
            ((JCTree.JCIdent)fieldAccess.selected).sym.type = fieldAccess.selected.type;
            fieldAccess.type = null;
            fieldAccess.sym = null;
            tree.type = null;

            // retry with supertype
            visitApply( tree );

            // restore original type
            fieldAccess.selected.type = oldType;
            ((JCTree.JCIdent)fieldAccess.selected).sym.type = fieldAccess.selected.type;
          }
        }
        else
        {
          // apply any issues logged for the found method (only the top of the suspend stack)
          _manLog.recordRecentSuspendedIssuesAndRemoveOthers( tree );
        }
      }
    }
    finally
    {
      if( JAILBREAK_PRIVATE_FROM_SUPERS )
      {
        _manLog.popSuspendIssues( tree );
      }
    }
  }

  private void _visitApply( JCTree.JCMethodInvocation tree )
  {
    //noinspection unchecked
    Env<AttrContext> env = (Env<AttrContext>)ReflectUtil.field( this, "env" ).get();

    Env<AttrContext> localEnv = env.dup( tree, (AttrContext)ReflectUtil.method( env.info, "dup" ).invoke() );

    // The types of the actual method arguments.
    List<Type> argtypes;

    // The types of the actual method type arguments.
    List<Type> typeargtypes;

    Name methName = TreeInfo.name( tree.meth );

    boolean isConstructorCall = methName == _names._this || methName == _names._super;

    if( isConstructorCall )
    {
      super.visitApply( tree );
      return;
    }

    // Otherwise, we are seeing a regular method call.
    // Attribute the arguments, yielding list of argument types, ...
    ListBuffer<Type> argtypesBuf = new ListBuffer<>();
    int kind = (int)attribArgs.invoke( this, VAL, tree.args, localEnv, argtypesBuf );
    argtypes = argtypesBuf.toList();
    typeargtypes = attribAnyTypes( tree.typeargs, localEnv );

    Object resultInfo = ReflectUtil.field( this, "resultInfo" ).get();

    // ... and attribute the method using as a prototype a methodtype
    // whose formal argument types is exactly the list of actual
    // arguments (this will also set the method symbol).
    Type mpt = newMethodTemplate( (Type)ReflectUtil.field( resultInfo, "pt" ).get(), argtypes, typeargtypes );
    ReflectUtil.field( localEnv.info, "pendingResolutionPhase" ).set( null );
    Type mtype = (Type)attribTree.invoke( this, tree.meth, localEnv,
      resultInfoCtor.newInstance( this, kind, mpt, ReflectUtil.field( resultInfo, "checkContext" ).get() ) );

    // Compute the result type.
    Type restype = mtype.getReturnType();

    // location of @Self if on a type argument or array component type
    Symbol symbol = tree.meth.hasTag( SELECT )
                    ? ((JCTree.JCFieldAccess)tree.meth).sym
                    : ((JCTree.JCIdent)tree.meth).sym;
    TypeAnnotationPosition selfPos = findSelfAnnotationLocation( symbol );

    if( restype.hasTag( WILDCARD ) )
    {
      throw new AssertionError( mtype );
    }

    Type qualifier = (tree.meth.hasTag( SELECT ))
                     ? ((JCTree.JCFieldAccess)tree.meth).selected.type
                     : env.enclClass.sym.type;

    if( selfPos != null || hasSelfType( restype ) )
    {
      // Replace with self type
      restype = replaceSelfTypesWithQualifier( qualifier, restype, selfPos );
    }
    else
    {
      restype = (Type)adjustMethodReturnType.invoke( this, qualifier, methName, argtypes, restype );
    }
    checkRefTypes.invoke( _chk, tree.typeargs, typeargtypes );

    // Check that value of resulting type is admissible in the
    // current context.  Also, capture the return type

    Type captured = (Type)capture.invoke( this, restype );
    restype = (Type)check.invoke( this, tree, captured, VAL, resultInfo);
    ReflectUtil.field( this, "result" ).set( restype );
    validate.invoke( _chk, tree.typeargs, localEnv );
  }

  private TypeAnnotationPosition findSelfAnnotationLocation( Symbol sym )
  {
    if( sym == null )
    {
      return null;
    }

    SymbolMetadata metadata = sym.getMetadata();
    if( metadata == null || metadata.isTypesEmpty() )
    {
      return null;
    }

    List<Attribute.TypeCompound> typeAttributes = metadata.getTypeAttributes();
    if( typeAttributes.isEmpty() )
    {
      return null;
    }

    return typeAttributes.stream()
      .filter( attr -> attr.getPosition().type == TargetType.METHOD_RETURN &&
                       attr.type.toString().equals( "manifold.ext.api.Self" ) )
      .map( Attribute.TypeCompound::getPosition )
      .findFirst()
      .orElse( null );
  }

  private boolean hasSelfType( Type type )
  {
    if( type instanceof Type.AnnotatedType )
    {
      for( Attribute.TypeCompound anno: type.getAnnotationMirrors() )
      {
        if( anno.type.toString().equals( "manifold.ext.api.Self" ) )
        {
          return true;
        }
      }
    }

    if( type instanceof Type.ArrayType )
    {
      return hasSelfType( ((Type.ArrayType)type).getComponentType() );
    }

    for( Type typeParam: type.getTypeArguments() )
    {
      if( hasSelfType( typeParam ) )
      {
        return true;
      }
    }

    if( type instanceof Type.IntersectionClassType )
    {
      for( Type compType: ((Type.IntersectionClassType)type).getComponents() )
      {
        if( hasSelfType( compType ) )
        {
          return true;
        }
      }
    }

    return false;
  }

  private Type replaceSelfTypesWithQualifier( Type receiverType, Type type, TypeAnnotationPosition selfPos )
  {
    if( type instanceof Type.AnnotatedType )
    {
      Type unannotatedType = type.unannotatedType();
      for( Attribute.TypeCompound anno: type.getAnnotationMirrors() )
      {
        if( anno.type.toString().equals( "manifold.ext.api.Self" ) )
        {
          Type newType;
          if( unannotatedType instanceof Type.ArrayType )
          {
            newType = makeArray( unannotatedType, receiverType );
          }
          else
          {
            newType = receiverType;
          }
          return newType;
          //return (Type)ReflectUtil.constructor( Type.AnnotatedType.class, List.class, Type.class ).newInstance( type.getAnnotationMirrors(), newType );
        }
      }
      //noinspection UnnecessaryLocalVariable
      Type newType = replaceSelfTypesWithQualifier( receiverType, unannotatedType, selfPos );
//      if( newType != unannotatedType )
//      {
//        return (Type)ReflectUtil.constructor( Type.AnnotatedType.class, List.class, Type.class ).newInstance( type.getAnnotationMirrors(), newType );
//      }
      return newType;
    }

    if( type instanceof Type.ArrayType )
    {
      if( hasSelfType( type ) || selfPos != null )
      {
        Type componentType = ((Type.ArrayType)type).getComponentType();
        if( componentType instanceof Type.ClassType )
        {
          return new Type.ArrayType( receiverType, _syms.arrayClass );
        }
        return new Type.ArrayType( replaceSelfTypesWithQualifier( receiverType, componentType, selfPos ), _syms.arrayClass );
      }
    }

    if( type instanceof Type.ClassType )
    {
      if( selfPos == null )
      {
        return type;
      }

      if( selfPos.location == null || selfPos.location.isEmpty() )
      {
        return receiverType;
      }

      List<TypePathEntry> selfLocation = selfPos.location;
      TypePathEntry loc = selfLocation.get( 0 );
      List<TypePathEntry> selfLocationCopy = List.from( selfLocation.subList( 1, selfLocation.size() ) );

      boolean replaced = false;
      ArrayList<Type> newParams = new ArrayList<>();
      List<Type> typeArguments = type.getTypeArguments();
      for( int i = 0; i < typeArguments.size(); i++ )
      {
        Type typeParam = typeArguments.get( i );
        if( i == loc.arg )
        {
          if( selfLocationCopy.isEmpty() )
          {
            typeParam = receiverType;
          }
          else
          {
            TypeAnnotationPosition posCopy = new TypeAnnotationPosition();
            posCopy.location = selfLocationCopy;
            typeParam = replaceSelfTypesWithQualifier( receiverType, typeParam, posCopy );
          }
          replaced = true;
        }
        newParams.add( typeParam );
      }
      if( replaced )
      {
        return new Type.ClassType( type.getEnclosingType(), List.from( newParams ), type.tsym );
      }
    }

    if( type instanceof Type.WildcardType )
    {
      List<TypePathEntry> selfLocationCopy = List.from( selfPos.location.subList( 1, selfPos.location.size() ) );
      TypeAnnotationPosition posCopy = new TypeAnnotationPosition();
      posCopy.location = selfLocationCopy;
      Type newType = replaceSelfTypesWithQualifier( receiverType, ((Type.WildcardType)type).type, posCopy );
      return new Type.WildcardType( newType, ((Type.WildcardType)type).kind, _syms.boundClass );
    }

    return type;
  }

  private Type makeArray( Type unannotatedType, Type receiverType )
  {
    if( unannotatedType instanceof Type.ArrayType )
    {
      return makeArray( ((Type.ArrayType)unannotatedType).getComponentType(), new Type.ArrayType( receiverType, _syms.arrayClass ) );
    }
    return receiverType;
  }

  private Type newMethodTemplate( Type restype, List<Type> argtypes, List<Type> typeargtypes )
  {
    Type.MethodType mt = new Type.MethodType( argtypes, restype, List.nil(), _syms.methodClass );
    return (typeargtypes == null) ? mt : new Type.ForAll( typeargtypes, mt );
  }

  private List<Type> attribAnyTypes( List<JCTree.JCExpression> trees, Env<AttrContext> env )
  {
    ListBuffer<Type> argtypes = new ListBuffer<>();
    for( List<JCTree.JCExpression> l = trees; l.nonEmpty(); l = l.tail )
    {
      argtypes.append( attribType( l.head, env ) );
    }
    return argtypes.toList();
  }

}