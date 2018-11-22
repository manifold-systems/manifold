package manifold.internal.javac;

import com.sun.tools.javac.code.Attribute;
import com.sun.tools.javac.code.Symbol;
import com.sun.tools.javac.code.SymbolMetadata;
import com.sun.tools.javac.code.Symtab;
import com.sun.tools.javac.code.TargetType;
import com.sun.tools.javac.code.Type;
import com.sun.tools.javac.code.TypeAnnotationPosition;
import com.sun.tools.javac.code.TypeAnnotationPosition.TypePathEntry;
import com.sun.tools.javac.comp.Attr;
import com.sun.tools.javac.comp.AttrContext;
import com.sun.tools.javac.comp.Check;
import com.sun.tools.javac.comp.Env;
import com.sun.tools.javac.tree.JCTree;
import com.sun.tools.javac.tree.TreeInfo;
import com.sun.tools.javac.util.Context;
import com.sun.tools.javac.util.List;
import com.sun.tools.javac.util.ListBuffer;
import com.sun.tools.javac.util.Name;
import com.sun.tools.javac.util.Names;
import java.util.ArrayList;
import java.util.Objects;
import manifold.util.ReflectUtil;


import static com.sun.tools.javac.code.TypeTag.WILDCARD;
import static com.sun.tools.javac.tree.JCTree.Tag.SELECT;

public class ManAttr_9 extends Attr
{
  private final Names _names;
  private final Symtab _syms;
  private final Check _chk;

  public static ManAttr_9 instance( Context ctx )
  {
    Attr attr = ctx.get( attrKey );
    if( !(attr instanceof ManAttr_9) )
    {
      ctx.put( attrKey, (Attr)null );
      attr = new ManAttr_9( ctx );
    }

    return (ManAttr_9)attr;
  }

  private ManAttr_9( Context ctx )
  {
    super( ctx );
    _names = Names.instance( ctx );
    _syms = Symtab.instance( ctx );
    _chk = Check.instance( ctx );
    
    reassignAllEarlyHolders( ctx );
  }

  private void reassignAllEarlyHolders( Context ctx )
  {
    String[] earlyAttrHolders = {
      "com.sun.tools.javac.comp.Modules",
      "com.sun.tools.javac.comp.Resolve",
      "com.sun.tools.javac.comp.DeferredAttr",
      "com.sun.tools.javac.comp.ArgumentAttr",
      "com.sun.tools.javac.comp.MemberEnter",
      "com.sun.tools.javac.comp.TypeEnter",
      "com.sun.tools.javac.comp.Analyzer",
      "com.sun.tools.javac.comp.Annotate",
      "com.sun.tools.javac.comp.Lower",
      "com.sun.tools.javac.comp.TransTypes",
      "com.sun.tools.javac.comp.Annotate",
      "com.sun.tools.javac.code.TypeAnnotations",
      "com.sun.tools.javac.api.JavacTrees",
      "com.sun.tools.javac.main.JavaCompiler",
    };
    for( String earlyAttrHolder: earlyAttrHolders )
    {
      Object instance = ReflectUtil.method( earlyAttrHolder, "instance", Context.class ).invokeStatic( ctx );
      ReflectUtil.LiveFieldRef attr = ReflectUtil.WithNull.field( instance, "attr" );
      if( attr != null )
      {
        attr.set( this );
      }
    }
  }

  private Class<?> KindSelector = ReflectUtil.type( "com.sun.tools.javac.code.Kinds$KindSelector" );
  private Object VAL = Objects.requireNonNull( ReflectUtil.field( KindSelector, "VAL" ) ).getStatic();
  private ReflectUtil.MethodRef attribArgs = ReflectUtil.method( Attr.class, "attribArgs", KindSelector, List.class, Env.class, ListBuffer.class );
  private Class<?> ResultInfo = ReflectUtil.type( "com.sun.tools.javac.comp.Attr$ResultInfo" );
  private ReflectUtil.MethodRef attribTree = ReflectUtil.method( Attr.class, "attribTree", JCTree.class, Env.class, ResultInfo );
  private ReflectUtil.ConstructorRef resultInfoCtor = ReflectUtil.constructor( ResultInfo, Attr.class, KindSelector, Type.class, Check.CheckContext.class );
  private ReflectUtil.MethodRef adjustMethodReturnType = ReflectUtil.method( Attr.class, "adjustMethodReturnType", Symbol.class, Type.class, Name.class, List.class, Type.class );
  private ReflectUtil.MethodRef checkRefTypes = ReflectUtil.method( Check.class, "checkRefTypes", List.class, List.class );
  private ReflectUtil.MethodRef check = ReflectUtil.method( Attr.class, "check", JCTree.class, Type.class, KindSelector, ResultInfo );
  private ReflectUtil.MethodRef validate = ReflectUtil.method( Check.class, "validate", List.class, Env.class );
  private ReflectUtil.MethodRef cachedCapture = ReflectUtil.method( "com.sun.tools.javac.comp.InferenceContext", "cachedCapture", JCTree.class, Type.class, boolean.class );

  @Override
  public void visitApply( JCTree.JCMethodInvocation tree )
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
    Object kind = attribArgs.invoke( this, VAL, tree.args, localEnv, argtypesBuf );
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
    TypeAnnotationPosition selfPos =
      findSelfAnnotationLocation( tree.meth.hasTag( SELECT )
                                  ? ((JCTree.JCFieldAccess)tree.meth).sym
                                  : ((JCTree.JCIdent)tree.meth).sym );

    if( restype.hasTag( WILDCARD ) )
    {
      throw new AssertionError( mtype );
    }

    Type qualifier = (tree.meth.hasTag( SELECT ))
                     ? ((JCTree.JCFieldAccess)tree.meth).selected.type
                     : env.enclClass.sym.type;

    if( selfPos != null || hasSelfType( restype ) )
    {
      restype = replaceSelfTypesWithQualifier( qualifier, restype, selfPos );
    }
    else
    {
      Symbol msym = TreeInfo.symbol( tree.meth );
      restype = (Type)adjustMethodReturnType.invoke( this, msym, qualifier, methName, argtypes, restype );
    }
    checkRefTypes.invoke( _chk, tree.typeargs, typeargtypes );

    // Check that value of resulting type is admissible in the
    // current context.  Also, capture the return type

    Check.CheckContext checkContext = (Check.CheckContext)ReflectUtil.field( resultInfo, "checkContext" ).get();
    Type capturedRes = (Type)cachedCapture.invoke( ReflectUtil.method( checkContext, "inferenceContext" ).invoke(), tree, restype, true );
    restype = (Type)check.invoke( this, tree, capturedRes, VAL, resultInfo );
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
  //  if( type instanceof Type.AnnotatedType )
  //  {
      for( Attribute.TypeCompound anno: type.getAnnotationMirrors() )
      {
        if( anno.type.toString().equals( "manifold.ext.api.Self" ) )
        {
          return true;
        }
      }
   // }

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
            //noinspection ConstantConditions
            TypeAnnotationPosition posCopy = (TypeAnnotationPosition)ReflectUtil
              .method( TypeAnnotationPosition.class, "methodReceiver", List.class ).invokeStatic( selfLocationCopy );
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
      //noinspection ConstantConditions
      TypeAnnotationPosition posCopy = (TypeAnnotationPosition)ReflectUtil
        .method( TypeAnnotationPosition.class, "methodReceiver", List.class ).invokeStatic( selfLocationCopy );
      Type newType = replaceSelfTypesWithQualifier( receiverType, ((Type.WildcardType)type).type, posCopy );
      return new Type.WildcardType( newType, ((Type.WildcardType)type).kind, _syms.boundClass );
    }

    return type;
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