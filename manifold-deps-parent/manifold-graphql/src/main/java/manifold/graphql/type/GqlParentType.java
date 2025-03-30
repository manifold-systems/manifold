/*
 * Copyright (c) 2019 - Manifold Systems LLC
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

package manifold.graphql.type;

import com.sun.tools.javac.code.Flags;
import graphql.language.*;

import java.lang.Class;
import java.lang.reflect.Modifier;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import graphql.validation.ValidationError;
import graphql.validation.ValidationErrorType;
import manifold.api.gen.*;
import manifold.ext.rt.CoercionProviders;
import manifold.ext.rt.api.*;
import manifold.graphql.rt.api.*;
import manifold.rt.api.Bindings;
import javax.tools.DiagnosticListener;
import javax.tools.JavaFileManager;
import javax.tools.JavaFileObject;
import manifold.api.fs.IFileFragment;
import manifold.api.host.IModule;
import manifold.json.rt.api.*;
import manifold.rt.api.ActualName;
import manifold.rt.api.FragmentValue;
import manifold.rt.api.SourcePosition;
import manifold.rt.api.util.ManEscapeUtil;
import manifold.rt.api.util.ManStringUtil;
import manifold.rt.api.util.Pair;
import manifold.ext.rt.RuntimeMethods;
import manifold.graphql.rt.api.request.Executor;
import manifold.rt.api.DisableStringLiteralTemplates;
import manifold.util.ReflectUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;


import static manifold.api.gen.AbstractSrcClass.Kind.Class;
import static manifold.api.gen.AbstractSrcClass.Kind.Enum;
import static manifold.api.gen.AbstractSrcClass.Kind.*;
import static manifold.api.gen.SrcLinkedClass.addActualNameAnnotation;
import static manifold.api.gen.SrcLinkedClass.makeIdentifier;

/**
 * The top-level class enclosing all the types defined in a single ".graphql" file.
 */
class GqlParentType
{
  private static final String ANONYMOUS_TYPE = "Anonymous_";

  private final GqlModel _model;
  private final Map<TypeDefinition, Set<UnionTypeDefinition>> _typeToUnions;
  private int _anonCount;

  GqlParentType( GqlModel model )
  {
    _model = model;
    _typeToUnions = new HashMap<>();
  }

  private String getFqn()
  {
    return _model.getFqn();
  }

  @SuppressWarnings( "unused" )
  boolean hasChild( String childName )
  {
    return _model.getTypeRegistry().getType( childName ).isPresent() ||
           _model.getOperations().containsKey( childName );
  }

  Definition getChild( String childName )
  {
    Definition def = _model.getTypeRegistry().getType( childName ).orElse( null );
    if( def == null )
    {
      def = _model.getOperations().get( childName );
    }
    return def;
  }

  void render( StringBuilder sb, JavaFileManager.Location location, IModule module, DiagnosticListener<JavaFileObject> errorHandler )
  {
    SrcLinkedClass srcClass = new SrcLinkedClass( getFqn(), Class, _model.getFile(), location, module, errorHandler )
      .addAnnotation( new SrcAnnotationExpression( DisableStringLiteralTemplates.class.getSimpleName() ) )
      .addAnnotation( new SrcAnnotationExpression( FragmentValue.class.getSimpleName() )
        .addArgument( "methodName", String.class, "fragmentValue" )
        .addArgument( "type", String.class, getFqn() ) )
      .modifiers( Modifier.PUBLIC );
    addImports( srcClass );
    addInnerTypes( srcClass );
    addInnerOperations( srcClass );
    addFragmentValueMethod( srcClass );
    srcClass.render( sb, 0 );
  }

  private void addFragmentValueMethod( SrcLinkedClass srcClass )
  {
    if( !(_model.getFile() instanceof IFileFragment) )
    {
      return;
    }

    switch( ((IFileFragment)_model.getFile()).getHostKind() )
    {
      case DOUBLE_QUOTE_LITERAL:
      case TEXT_BLOCK_LITERAL:
        break;

      default:
        return;
    }

    for( OperationDefinition operation: _model.getOperations().values() )
    {
      switch( operation.getOperation() )
      {
        case QUERY:
        case MUTATION:
          // Queries and mutations are the same thing -- web requests
          addValueMethodForOperation( operation, srcClass );
          break;
        case SUBSCRIPTION:
          // todo:
          break;
      }
    }
  }

  private void addValueMethodForOperation( OperationDefinition operation, SrcLinkedClass srcClass )
  {
    String identifier = makeIdentifier( operation.getName(), false );

    SrcMethod method = new SrcMethod( srcClass )
      .modifiers( Flags.PUBLIC )
      .name( "builder" )
      .returns( new SrcType( identifier + ".Builder" ) );
    addRequiredParameters( srcClass, operation, method );
    StringBuilder sb = new StringBuilder();
    sb.append( "return $identifier.builder(" );
    int count = 0;
    for( SrcParameter param: method.getParameters() )
    {
      if( count++ > 0 )
      {
        sb.append( ", " );
      }
      //noinspection unused
      sb.append( param.getSimpleName() );
    }
    sb.append( ");" );
    method.body( sb.toString() );
    srcClass.addMethod( method );


    SrcMethod valueMethod = new SrcMethod( srcClass )
      .modifiers( Modifier.PUBLIC | Modifier.STATIC )
      .name( "fragmentValue" )
      .returns( srcClass.getSimpleName() )
      .body( "return new ${srcClass.getSimpleName()}();" );
    srcClass.addMethod( valueMethod );
  }

  private void addInnerTypes( SrcLinkedClass srcClass )
  {
    mapUnionMemberToUnions();

    for( TypeDefinition type: _model.getTypeRegistry().types().values() )
    {
      if( type instanceof ObjectTypeDefinition )
      {
        addInnerObjectType( (ObjectTypeDefinition)type, srcClass );
      }
      else if( type instanceof InterfaceTypeDefinition )
      {
        addInnerInterfaceType( (InterfaceTypeDefinition)type, srcClass );
      }
      else if( type instanceof EnumTypeDefinition )
      {
        addInnerEnumType( (EnumTypeDefinition)type, srcClass );
      }
      else if( type instanceof InputObjectTypeDefinition )
      {
        addInnerInputType( (InputObjectTypeDefinition)type, srcClass );
      }
      else if( type instanceof UnionTypeDefinition )
      {
        addInnerUnionType( (UnionTypeDefinition)type, srcClass );
      }
    }
  }

  // Map of union type member to union types, facilitates nominal typing (for performance) e.g.,
  //
  //     union PointyShape = Triangle | Diamond
  //
  //     interface PointyShape { /* intersection methods of Triangle and Diamond */ }
  //
  // Since we model a union as an interface consisting of the intersection of methods of its member types, by
  // definition the member types logically implement the union, therefore the member type explicitly declares that it
  // implements all unions of which it is a member:
  //
  //     interface Triangle extends PointyShape { ... }
  //
  // Thus we can nominally address a Triangle as a PointyShape:
  //
  //     PointyShape pointy = triangle;
  //
  // Note because we model graphql types as *structural* interfaces the nominal typing added here is unnecessary --
  // because PointyShape is structural we could cast triangle. However, from a performance standpoint it is worthwhile
  // because it saves us the initial cost of dynamic proxy generation, which otherwise adds considerable lag on first
  // use.
  private void mapUnionMemberToUnions()
  {
    _model.getTypeRegistry().types().values().stream()
      .filter( typeDef -> typeDef instanceof UnionTypeDefinition )
      .map( typeDef -> (UnionTypeDefinition)typeDef )
      .forEach( unionTypeDef -> unionTypeDef.getMemberTypes().stream()
        .map( memberType -> findTypeDefinition( memberType ) )
        .forEach( memberTypeDef ->
          _typeToUnions.computeIfAbsent( memberTypeDef, t -> new HashSet<>() )
            .add( unionTypeDef ) ) );
  }

  private void addInnerOperations( SrcLinkedClass srcClass )
  {
    for( OperationDefinition operation: _model.getOperations().values() )
    {
      switch( operation.getOperation() )
      {
        case QUERY:
        case MUTATION:
          // Queries and mutations are the same thing -- web requests
          addQueryType( operation, srcClass );
          break;
        case SUBSCRIPTION:
          // todo:
          break;
      }
    }
  }

  private void addQueryType( OperationDefinition operation, SrcLinkedClass enclosingType )
  {
    String name = getOperationName( operation );
    String identifier = makeIdentifier( name, false );
    String fqn = getFqn() + '.' + identifier;
    SrcLinkedClass srcClass = new SrcLinkedClass( fqn, enclosingType, Interface )
      .addInterface( IJsonBindingsBacked.class.getSimpleName() )
      .addInterface( new SrcType( "GqlQuery<$identifier.Result>" ) )
      .addAnnotation( new SrcAnnotationExpression( Structural.class.getSimpleName() ) )
      .modifiers( Modifier.PUBLIC );
    addActualNameAnnotation( srcClass, name, false );
    addSourcePositionAnnotation( srcClass, operation, operation::getName, srcClass );
    addQueryResultType( operation, getRoot( operation.getOperation() ), srcClass );
    addBuilder( srcClass, operation );
    addCreateMethod( srcClass, operation );
    addBuilderMethod( srcClass, operation );
    addRequestMethods( srcClass, operation );
    addLoadMethod( srcClass );
    addCopierMethod( srcClass );
    addCopyMethod( srcClass );

    for( VariableDefinition varDef: operation.getVariableDefinitions() )
    {
      String actualName = ensure$included( varDef );
      String nameNo$ = remove$( actualName );
      String propName = makeIdentifier( nameNo$, true );
      SrcType getterType = makeSrcType( enclosingType, varDef.getType(), false );
      SrcType setterType = makeSrcType( enclosingType, varDef.getType(), false, true );
//      //noinspection unused
//      StringBuilder propertyType = getterType.render( new StringBuilder(), 0, false );
//      //noinspection unused
//      StringBuilder componentType = getComponentType( getterType ).render( new StringBuilder(), 0, false );
      SrcGetProperty getter = new SrcGetProperty( propName, getterType );
      addActualNameAnnotation( getter, nameNo$, true );
      addSourcePositionAnnotation( srcClass, varDef, actualName, getter );
      srcClass.addGetProperty( getter ).modifiers( Modifier.PUBLIC );

      SrcSetProperty setter = new SrcSetProperty( propName, setterType );
      addActualNameAnnotation( setter, nameNo$, true );
      addSourcePositionAnnotation( srcClass, varDef, actualName, setter );
      srcClass.addSetProperty( setter ).modifiers( Modifier.PUBLIC );
    }

    enclosingType.addInnerClass( srcClass );
  }

  @NotNull
  private String getOperationName( OperationDefinition operation )
  {
    String name = operation.getName();
    return (name == null || name.isEmpty()) ? ANONYMOUS_TYPE + _anonCount++ : name;
  }

  private String remove$( String name )
  {
    if( name.charAt( 0 ) == '$' )
    {
      return name.substring( 1 );
    }
    return name;
  }

  private void addRequestMethods( SrcLinkedClass srcClass, OperationDefinition operation )
  {
    //noinspection unused
    String query = ManEscapeUtil.escapeForJavaStringLiteral( AstPrinter.printAstCompact( operation ) );
    String fragments = getFragments( srcClass );
    //noinspection UnusedAssignment
    query += " " + fragments;
    srcClass.addMethod( new SrcMethod()
      .addAnnotation( new SrcAnnotationExpression( DisableStringLiteralTemplates.class.getSimpleName() ) )
      .modifiers( Flags.DEFAULT )
      .name( "request" )
      .addParam( "url", String.class )
      .returns( new SrcType( "Executor<Result>" ) )
      .body( "return new Executor<Result>(url, \"${operation.getOperation().name().toLowerCase()}\", \"$query\", getBindings(), Result.class);"
      ) );
    srcClass.addMethod( new SrcMethod()
      .addAnnotation( new SrcAnnotationExpression( DisableStringLiteralTemplates.class.getSimpleName() ) )
      .modifiers( Flags.DEFAULT )
      .name( "request" )
      .addParam( "endpoint", Endpoint.class )
      .returns( new SrcType( "Executor<Result>" ) )
      .body( "return new Executor<Result>(endpoint, \"${operation.getOperation().name().toLowerCase()}\", \"$query\", getBindings(), Result.class);"
      ) );
    srcClass.addMethod( new SrcMethod()
      .addAnnotation( new SrcAnnotationExpression( DisableStringLiteralTemplates.class.getSimpleName() ) )
      .modifiers( Flags.DEFAULT )
      .name( "request" )
      .addParam( "requester", new SrcType( "Supplier<Requester<Bindings>>" ) )
      .returns( new SrcType( "Executor<Result>" ) )
      .body( "return new Executor<Result>(requester, \"${operation.getOperation().name().toLowerCase()}\", \"$query\", getBindings(), Result.class);"
      ) );
  }

  private String getFragments( SrcLinkedClass srcClass )
  {
    //noinspection unchecked
    Map<String, FragmentDefinition> fragments = (Map)srcClass.getUserData( "fragments" );
    if( fragments == null )
    {
      return "";
    }

    StringBuilder sb = new StringBuilder();
    fragments.values().forEach( fragment ->
      sb.append( ManEscapeUtil.escapeForJavaStringLiteral( AstPrinter.printAstCompact( fragment ) ) ).append( " " ) );
    return sb.toString();
  }

  private void addLoadMethod( SrcLinkedClass srcClass )
  {
    //noinspection unused
    String simpleName = srcClass.getSimpleName();
    SrcMethod method = new SrcMethod( srcClass )
      .modifiers( Modifier.STATIC )
      .name( "load" )
      .returns( new SrcType( "Loader<$simpleName>" ) )
      .body( "return new Loader<>();" );
    srcClass.addMethod( method );
  }

  private void addCreateMethod( SrcLinkedClass srcClass, Definition definition )
  {
    String simpleName = srcClass.getSimpleName();
    SrcMethod method = new SrcMethod( srcClass )
      .modifiers( Modifier.STATIC )
      .name( "create" )
      .returns( simpleName );
    addRequiredParameters( srcClass, definition, method );
    srcClass.addMethod( method );

    SrcStatementBlock block = new SrcStatementBlock();
    block.addStatement( "$simpleName thiz = ($simpleName)coerceFromBindingsValue(new DataBindings(), $simpleName.class);" );
    for( NamedNode node: getDefinitions( definition ) )
    {
      String name = makeIdentifier( remove$( node.getName() ), false );
      //noinspection unused
      String Prop = makeIdentifier( name, true );
      if( isRequiredVar( node ) )
      {
        block.addStatement( "thiz.set$Prop($name);" );
      }
      else if( node instanceof VariableDefinition )
      {
        Value defaultValue = ((VariableDefinition)node).getDefaultValue();
        if( defaultValue != null )
        {
          StringBuilder value = new StringBuilder();
          makeStringValue( value, defaultValue, ((VariableDefinition)node).getType(), srcClass );
          block.addStatement( "thiz.set$Prop($value);" );
        }
      }
    }
    block.addStatement( "return thiz;" );
    method.body( block );
  }

  private void makeStringValue( StringBuilder sb, Value value, Type type, SrcLinkedClass srcClass )
  {
    if( value instanceof ArrayValue )
    {
      sb.append( "new " );
      SrcType srcType = makeSrcType( srcClass, type, false );
      srcType.render( sb, 0 );
      sb.append( '[' );
      for( Value elem: ((ArrayValue)value).getValues() )
      {
        if( sb.charAt( sb.length()-1 ) != '[' )
        {
          sb.append( ',' );
        }
        makeStringValue( sb, elem, ((ListType)type).getType(), srcClass );
      }
      sb.append( ']' );
    }
    else if( value instanceof BooleanValue )
    {
      sb.append( ((BooleanValue)value).isValue() );
    }
    else if( value instanceof EnumValue )
    {
      SrcType srcType = makeSrcType( srcClass, type, false );
      srcType.render( sb, 0 );
      sb.append( '.' ).append( makeIdentifier( ((EnumValue)value).getName(), false ) );
    }
    else if( value instanceof FloatValue )
    {
      sb.append( ((FloatValue)value).getValue().doubleValue() );
    }
    else if( value instanceof IntValue )
    {
      sb.append( ((IntValue)value).getValue().intValue() );
    }
    else if( value instanceof NullValue )
    {
      sb.append( "null" );
    }
    else if( value instanceof StringValue )
    {                                                             
      sb.append( '"' ).append( ((StringValue)value).getValue() ).append( '"' );
    }
    else
    {
      throw new UnsupportedOperationException( "Unexpected constant value type: " + value.getClass().getSimpleName() );
    }
  }

  private void addBuilderMethod( SrcLinkedClass srcClass, Definition definition )
  {
    SrcMethod method = new SrcMethod( srcClass )
      .modifiers( Modifier.STATIC )
      .name( "builder" )
      .returns( new SrcType( "Builder" ) );
    addRequiredParameters( srcClass, definition, method );
    srcClass.addMethod( method );

    StringBuilder sb = new StringBuilder();
    sb.append( "return (Builder)coerceFromBindingsValue(create(" );
    int count = 0;
    for( SrcParameter param: method.getParameters() )
    {
      if( count++ > 0 )
      {
        sb.append( ", " );
      }
      //noinspection unused
      sb.append( makeIdentifier( param.getSimpleName(), false ) );
    }
    sb.append( ").getBindings(), Builder.class);" );
    method.body( sb.toString() );
  }

  private void addRequiredParameters( SrcLinkedClass owner, Definition definition, AbstractSrcMethod method )
  {
    for( NamedNode node: getDefinitions( definition ) )
    {
      if( isRequiredVar( node ) )
      {
        Type type = getType( node );
        SrcType srcType = makeSrcType( owner, type, false, true );
        method.addParam( makeIdentifier( remove$( node.getName() ), false ), srcType );
      }
    }
  }

  private void addBuilder( SrcLinkedClass enclosingType, Definition definition )
  {
    String fqn = enclosingType.getName() + ".Builder";
    SrcLinkedClass srcClass = new SrcLinkedClass( fqn, enclosingType, Interface )
      .addAnnotation( new SrcAnnotationExpression( Structural.class.getSimpleName() ) )
      .addInterface( new SrcType( GqlBuilder.class.getSimpleName() ).addTypeParam( enclosingType.getSimpleName() ) );
    enclosingType.addInnerClass( srcClass );
    addWithMethods( srcClass, definition );
  }

  private void addWithMethods( SrcLinkedClass srcClass, Definition definition )
  {
    for( NamedNode node: getDefinitions( definition ) )
    {
      if( isRequiredVar( node ) )
      {
        continue;
      }

      Type type = getType( node );
      String propName = makeIdentifier( node.getName(), true );
      addWithMethod( srcClass, node, propName, makeSrcType( srcClass, type, false, true ) );
    }
  }

  private void addCopierMethod( SrcLinkedClass enclosingType )
  {
    SrcMethod method = new SrcMethod( enclosingType )
      .modifiers( Modifier.STATIC )
      .name( "copier" )
      .returns( new SrcType( "Builder" ) )
      .addParam( "from", enclosingType.getSimpleName() )
      .body( "return (Builder)coerceFromBindingsValue(from.getBindings().deepCopy(), Builder.class);" );
    enclosingType.addMethod( method );
  }

  private void addCopyMethod( SrcLinkedClass enclosingType )
  {
    SrcMethod method = new SrcMethod( enclosingType )
      .modifiers( Flags.DEFAULT )
      .name( "copy" )
      .returns( new SrcType( enclosingType.getSimpleName() ) )
      .body( "return (${enclosingType.getSimpleName()})coerceFromBindingsValue(getBindings().deepCopy(), ${enclosingType.getSimpleName()}.class);" );
    enclosingType.addMethod( method );
  }

  private List<? extends NamedNode> getDefinitions( Definition def )
  {
    if( def instanceof OperationDefinition )
    {
      return ((OperationDefinition)def).getVariableDefinitions();
    }
    if( def instanceof InputObjectTypeDefinition )
    {
      return ((InputObjectTypeDefinition)def).getInputValueDefinitions();
    }
    if( def instanceof ObjectTypeDefinition )
    {
      return ((ObjectTypeDefinition)def).getFieldDefinitions();
    }
    throw new IllegalStateException();
  }

  private Type getType( Node def )
  {
    if( def instanceof VariableDefinition )
    {
      return ((VariableDefinition)def).getType();
    }
    if( def instanceof InputValueDefinition )
    {
      return ((InputValueDefinition)def).getType();
    }
    if( def instanceof FieldDefinition )
    {
      return ((FieldDefinition)def).getType();
    }
    throw new IllegalStateException();
  }

  private void addWithMethod( SrcLinkedClass srcClass, NamedNode node, @SuppressWarnings( "unused" ) String propName,
                              SrcType type )
  {
    //noinspection unused
    String actualName = ensure$included( node );

    //noinspection unused
    StringBuilder propertyType = type.render( new StringBuilder(), 0, false );
    SrcMethod withMethod = new SrcMethod()
      .name( "with$propName" )
      .addParam( "${'$'}value", type )
      .returns( new SrcType( srcClass.getSimpleName() ) );
    addActualNameAnnotation( withMethod, remove$( actualName ), true );
    addSourcePositionAnnotation( srcClass, node, actualName, withMethod );

    srcClass.addMethod( withMethod );
  }

  @NotNull
  private String ensure$included( NamedNode node )
  {
    String actualName = node.getName();
    if( !actualName.startsWith( "$" ) && node instanceof VariableDefinition )
    {
      actualName = '$' + actualName;
    }
    return actualName;
  }

  private boolean isRequiredVar( NamedNode node )
  {
    Type type = getType( node );
    return type instanceof NonNullType &&
           (!(node instanceof VariableDefinition) || ((VariableDefinition)node).getDefaultValue() == null);
  }

  private TypeDefinition getRoot( OperationDefinition.Operation operation )
  {
    TypeDefinition root = null;
    SchemaDefinition schemaDefinition = _model.getScope().getSchemaDefinition();
    if( schemaDefinition != null )
    {
      Optional<OperationTypeDefinition> rootOperationType =
        schemaDefinition.getOperationTypeDefinitions().stream()
          .filter( e -> e.getName().equals( getOperationKey( operation ) ) ).findFirst();
      if( rootOperationType.isPresent() )
      {
        Type type = rootOperationType.get().getTypeName();
        root = findTypeDefinition( type );
      }
    }
    if( root == null )
    {
      // e.g., by convention a 'type' named "Query" is considered the root query type
      // if one is not specified in the 'schema'
      root = _model.getScope().findTypeDefinition( getOperationDefaultTypeName( operation ) );
    }
    return root;
  }

  private String getOperationDefaultTypeName( OperationDefinition.Operation operation )
  {
    return ManStringUtil.capitalize( operation.name().toLowerCase() );
  }

  @NotNull
  private String getOperationKey( OperationDefinition.Operation operation )
  {
    return operation.name().toLowerCase();
  }

  private void addQueryResultType( OperationDefinition operation, TypeDefinition ctx, SrcLinkedClass enclosingType )
  {
    String fqn = enclosingType.getName() + ".Result";
    SrcLinkedClass srcClass = new SrcLinkedClass( fqn, enclosingType, Interface )
      .addInterface( GqlQueryResult.class.getSimpleName() )
      .addAnnotation( new SrcAnnotationExpression( Structural.class.getSimpleName() ) )
      .modifiers( Modifier.PUBLIC );

    for( Selection member: operation.getSelectionSet().getSelections() )
    {
      addQuerySelection( srcClass, ctx, member );
    }

    enclosingType.addInnerClass( srcClass );
  }

  private void addImports( SrcLinkedClass srcClass )
  {
    srcClass.addImport( Bindings.class );
    srcClass.addImport( Endpoint.class );
    srcClass.addImport( Executor.class );
    srcClass.addImport( GqlQuery.class );
    srcClass.addImport( GqlQueryResult.class );
    srcClass.addImport( Requester.class );
    srcClass.addImport( GqlType.class );
    srcClass.addImport( GqlBuilder.class );
    srcClass.addImport( DataBindings.class );
    srcClass.addImport( IBindingType.class );
    srcClass.addImport( IJsonBindingsBacked.class );
    srcClass.addImport( IProxyFactory.class );
    srcClass.addImport( List.class );
    srcClass.addImport( IListBacked.class );
    srcClass.addImport( Loader.class );
    srcClass.addImport( Map.class );
    srcClass.addImport( HashMap.class );
    srcClass.addImport( RuntimeMethods.class );
    srcClass.addImport( Supplier.class );
    srcClass.addImport( NotNull.class );
    srcClass.addImport( ActualName.class );
    srcClass.addImport( DisableStringLiteralTemplates.class );
    srcClass.addImport( SourcePosition.class );
    srcClass.addImport( Structural.class );
    srcClass.addImport( FragmentValue.class );
    srcClass.addStaticImport( RuntimeMethods.class.getName() + ".coerceFromBindingsValue" );
    importAllOtherGqlTypes( srcClass );
  }

  private void importAllOtherGqlTypes( SrcLinkedClass srcClass )
  {
    _model.getScope().getAllModels().forEach( model -> {
      if( !model.getFqn().equals( getFqn() ) )
      {
        // exclude manifold fragments
        if( !(model.getFile() instanceof IFileFragment) )
        {
          srcClass.addStaticImport( model.getFqn() + ".*" );
        }
      }
    } );
  }

  private void addInnerObjectType( ObjectTypeDefinition type, SrcLinkedClass enclosingType )
  {
    String identifier = makeIdentifier( type.getName(), false );
    String fqn = getFqn() + '.' + identifier;
    SrcLinkedClass srcClass = new SrcLinkedClass( fqn, enclosingType, Interface )
      .addInterface( GqlType.class.getSimpleName() )
      .addAnnotation( new SrcAnnotationExpression( Structural.class.getSimpleName() ) )
      .modifiers( Modifier.PUBLIC );
    addUnionInterfaces( type, srcClass );
    addActualNameAnnotation( srcClass, type.getName(), false );
    addSourcePositionAnnotation( srcClass, type, srcClass );
    List<Type> interfaces = type.getImplements();
    addInterfaces( srcClass, interfaces );
    addBuilder( srcClass, type );
    addCreateMethod( srcClass, type );
    addBuilderMethod( srcClass, type );
    addLoadMethod( srcClass );
    addCopierMethod( srcClass );
    addCopyMethod( srcClass );

    List<FieldDefinition> fieldDefinitions = type.getFieldDefinitions();
    for( FieldDefinition member: fieldDefinitions )
    {
      addMember( srcClass, member, name -> fieldDefinitions.stream().anyMatch( f -> f.getName().equals( name ) ) );
    }
    addObjectExtensions( type, srcClass );
    enclosingType.addInnerClass( srcClass );
  }

  private void addUnionInterfaces( TypeDefinition type, SrcLinkedClass srcClass )
  {
    Set<UnionTypeDefinition> unions = _typeToUnions.get( type );
    if( unions != null )
    {
      unions.forEach( union -> srcClass.addInterface( makeIdentifier( union.getName(), false ) ) );
    }
  }

  private void addObjectExtensions( ObjectTypeDefinition type, SrcLinkedClass srcClass )
  {
    List<FieldDefinition> baseFieldDefinitions = type.getFieldDefinitions();
    List<ObjectTypeExtensionDefinition> objectExtensions = _model.getTypeRegistry().objectTypeExtensions().get( type.getName() );
    if( objectExtensions != null )
    {
      for( ObjectTypeExtensionDefinition ext: objectExtensions )
      {
        List<FieldDefinition> extFieldDefinitions = ext.getFieldDefinitions();
        for( FieldDefinition member: extFieldDefinitions )
        {
          addMember( srcClass, member,
            name ->
              baseFieldDefinitions.stream().anyMatch( f -> f.getName().equals( name ) ) ||
              extFieldDefinitions.stream().anyMatch( f -> f.getName().equals( name ) )
          );
        }
      }
    }
  }

  private void addInnerInputType( InputObjectTypeDefinition type, SrcLinkedClass enclosingType )
  {
    String identifier = makeIdentifier( type.getName(), false );
    String fqn = getFqn() + '.' + identifier;
    SrcLinkedClass srcClass = new SrcLinkedClass( fqn, enclosingType, Interface )
      .addInterface( GqlType.class.getSimpleName() )
      .addAnnotation( new SrcAnnotationExpression( Structural.class.getSimpleName() ) )
      .modifiers( Modifier.PUBLIC );
    addUnionInterfaces( type, srcClass );
    addActualNameAnnotation( srcClass, type.getName(), false );
    addSourcePositionAnnotation( srcClass, type, srcClass );
    addBuilder( srcClass, type );
    addCreateMethod( srcClass, type );
    addBuilderMethod( srcClass, type );
    addLoadMethod( srcClass );
    addCopierMethod( srcClass );
    addCopyMethod( srcClass );

    List<InputValueDefinition> inputValueDefinitions = type.getInputValueDefinitions();

    for( InputValueDefinition member: inputValueDefinitions )
    {
      addMember( srcClass, member, name -> inputValueDefinitions.stream().anyMatch( f -> f.getName().equals( name ) ) );
    }
    addInputExtensions( type, srcClass );
    enclosingType.addInnerClass( srcClass );
  }

  private void addInputExtensions( InputObjectTypeDefinition type, SrcLinkedClass srcClass )
  {
    List<InputValueDefinition> baseInputValueDefinitions = type.getInputValueDefinitions();
    List<InputObjectTypeExtensionDefinition> inputExtensions = _model.getTypeRegistry().inputObjectTypeExtensions().get( type.getName() );
    if( inputExtensions != null )
    {
      for( InputObjectTypeExtensionDefinition ext: inputExtensions )
      {
        List<InputValueDefinition> extInputValueDefinitions = ext.getInputValueDefinitions();
        for( InputValueDefinition member: extInputValueDefinitions )
        {
          addMember( srcClass, member,
            name ->
              baseInputValueDefinitions.stream().anyMatch( f -> f.getName().equals( name ) ) ||
              extInputValueDefinitions.stream().anyMatch( f -> f.getName().equals( name ) )
          );
        }
      }
    }
  }

  private void addInnerInterfaceType( InterfaceTypeDefinition type, SrcLinkedClass enclosingType )
  {
    String identifier = makeIdentifier( type.getName(), false );
    String fqn = getFqn() + '.' + identifier;
    SrcLinkedClass srcClass = new SrcLinkedClass( fqn, enclosingType, Interface )
      .addInterface( GqlType.class.getSimpleName() )
      .addAnnotation( new SrcAnnotationExpression( Structural.class.getSimpleName() ) )
      .modifiers( Modifier.PUBLIC );
    addUnionInterfaces( type, srcClass );
    addActualNameAnnotation( srcClass, type.getName(), false );
    addSourcePositionAnnotation( srcClass, type, srcClass );
    List<FieldDefinition> fieldDefinitions = type.getFieldDefinitions();
    for( FieldDefinition member: fieldDefinitions )
    {
      addMember( srcClass, member, name -> fieldDefinitions.stream().anyMatch( f -> f.getName().equals( name ) ) );
    }
    addInterfaceExtensions( type, srcClass );
    enclosingType.addInnerClass( srcClass );
  }

  private void addInterfaceExtensions( InterfaceTypeDefinition type, SrcLinkedClass srcClass )
  {
    List<FieldDefinition> baseFieldDefinitions = type.getFieldDefinitions();
    List<InterfaceTypeExtensionDefinition> interfaceExtensions = _model.getTypeRegistry().interfaceTypeExtensions().get( type.getName() );
    if( interfaceExtensions != null )
    {
      for( InterfaceTypeExtensionDefinition ext: interfaceExtensions )
      {
        List<FieldDefinition> extFieldDefinitions = ext.getFieldDefinitions();
        for( FieldDefinition member: extFieldDefinitions )
        {
          addMember( srcClass, member,
            name ->
              baseFieldDefinitions.stream().anyMatch( f -> f.getName().equals( name ) ) ||
              extFieldDefinitions.stream().anyMatch( f -> f.getName().equals( name ) )
          );
        }
      }
    }
  }

  private void addInnerEnumType( EnumTypeDefinition type, SrcLinkedClass enclosingType )
  {
    String identifier = makeIdentifier( type.getName(), false );
    SrcLinkedClass srcClass = new SrcLinkedClass( getFqn() + '.' + identifier, enclosingType, Enum )
      .modifiers( Modifier.PUBLIC )
      .addInterface( IBindingType.class.getSimpleName() );
    addActualNameAnnotation( srcClass, type.getName(), false );
    addSourcePositionAnnotation( srcClass, type, srcClass );
    for( EnumValueDefinition member: type.getEnumValueDefinitions() )
    {
      addEnumConstant( identifier, srcClass, member );
    }
    addEnumExtensions( type, srcClass );
    srcClass.addMethod(
      new SrcMethod( srcClass )
        .name( "toBindingValue" )
        .modifiers( Modifier.PUBLIC )
        .returns( Object.class )
        .body( "return name();" ) );
    enclosingType.addInnerClass( srcClass );
  }

  private void addEnumConstant( String identifier, SrcLinkedClass srcClass, EnumValueDefinition member )
  {
    SrcField enumConst = new SrcField( makeIdentifier( member.getName(), false ), identifier );
    addSourcePositionAnnotation( srcClass, member, enumConst );
    srcClass.addEnumConst( enumConst );
  }

  private void addEnumExtensions( EnumTypeDefinition type, SrcLinkedClass srcClass )
  {
    List<EnumTypeExtensionDefinition> enumExtensions = _model.getTypeRegistry().enumTypeExtensions().get( type.getName() );
    if( enumExtensions != null )
    {
      String declaringType = makeIdentifier( type.getName(), false );
      for( EnumTypeExtensionDefinition ext: enumExtensions )
      {
        for( EnumValueDefinition member: ext.getEnumValueDefinitions() )
        {
          addEnumConstant( declaringType, srcClass, member );
        }
      }
    }
  }

  private void addInnerUnionType( UnionTypeDefinition type, SrcLinkedClass enclosingType )
  {
    String identifier = makeIdentifier( type.getName(), false );
    String fqn = getFqn() + '.' + identifier;
    SrcLinkedClass srcClass = new SrcLinkedClass( fqn, enclosingType, Interface )
      .addInterface( GqlType.class.getSimpleName() )
      .addAnnotation( new SrcAnnotationExpression( Structural.class.getSimpleName() ) )
      .modifiers( Modifier.PUBLIC );
    addUnionInterfaces( type, srcClass );
    String lub = findLub( type );
    if( lub != null )
    {
      srcClass.addInterface( makeIdentifier( lub, false ) );
    }
    addActualNameAnnotation( srcClass, type.getName(), false );
    addSourcePositionAnnotation( srcClass, type, srcClass );
    addIntersectionMethods( srcClass, type );
    enclosingType.addInnerClass( srcClass );
  }

  private void addIntersectionMethods( SrcLinkedClass srcClass, UnionTypeDefinition union )
  {
    Set<Pair<String, String>> fieldDefs = new HashSet<>();
    Map<String, FieldDefinition> nameToFieldDef = new HashMap<>();
    for( Type memberType: union.getMemberTypes() )
    {
      TypeDefinition typeDef = findTypeDefinition( memberType );
      if( typeDef instanceof ObjectTypeDefinition )
      {
        if( fieldDefs.isEmpty() )
        {
          fieldDefs.addAll( ((ObjectTypeDefinition)typeDef).getFieldDefinitions()
            .stream().map( e -> {
              nameToFieldDef.put( e.getName(), e );
              return new Pair<>( e.getName(), e.getType().toString() );
            } ).collect( Collectors.toSet() ) );
        }
        else
        {
          fieldDefs.retainAll( ((ObjectTypeDefinition)typeDef).getFieldDefinitions()
            .stream().map( e -> new Pair<>( e.getName(), e.getType().toString() ) ).collect( Collectors.toSet() ) );
        }
      }
    }
    fieldDefs.forEach(
      fieldDef -> addMember( srcClass, null, nameToFieldDef.get( fieldDef.getFirst() ).getType(), fieldDef.getFirst(),
        name -> fieldDefs.stream().anyMatch( f -> f.getFirst().equals( name ) ) ) );
  }

  private void addInterfaces( SrcLinkedClass srcClass, List<Type> interfaces )
  {
    for( Type iface: interfaces )
    {
      if( iface instanceof TypeName )
      {
        srcClass.addInterface( makeIdentifier( ((TypeName)iface).getName(), false ) );
      }
    }
  }

  private void addQuerySelection( SrcLinkedClass srcClass, TypeDefinition ctx, Selection selection )
  {
    if( selection instanceof Field )
    {
      addQueryField( srcClass, ctx, (Field)selection );
    }
    else if( selection instanceof FragmentSpread )
    {
      String name = ((FragmentSpread)selection).getName();
      FragmentDefinition fragment = _model.getFragments().get( name );
      TypeDefinition fragmentCtx = fragment == null ? null : findTypeDefinition( fragment.getTypeCondition() );
      if( fragmentCtx != null )
      {
        mapFragmentUsageToQuery( srcClass, name, fragment );
        for( Selection fragSelection: fragment.getSelectionSet().getSelections() )
        {
          addQuerySelection( srcClass, fragmentCtx, fragSelection );
        }
      }
    }
    else if( selection instanceof InlineFragment )
    {
      InlineFragment inlineFragment = (InlineFragment)selection;
      TypeDefinition fragmentCtx = findTypeDefinition( inlineFragment.getTypeCondition() );
      if( fragmentCtx != null )
      {
        for( Selection fragSelection: inlineFragment.getSelectionSet().getSelections() )
        {
          addQuerySelection( srcClass, fragmentCtx, fragSelection );
        }
      }
    }
  }

  private void mapFragmentUsageToQuery( SrcLinkedClass srcClass, String name, FragmentDefinition fragment )
  {
    SrcLinkedClass operation = findOperation( srcClass );
    //noinspection unchecked
    Map<String, FragmentDefinition> fragments = (Map)operation.computeOrGetUserData( "fragments",
      key -> new LinkedHashMap<String, FragmentDefinition>() );
    fragments.put( name, fragment );
  }

  private SrcLinkedClass findOperation( SrcLinkedClass srcClass )
  {
    AbstractSrcClass enclosingClass = srcClass.getEnclosingClass();
    if( enclosingClass == null || enclosingClass.getEnclosingClass() == null )
    {
      // srcClass should be a query or mutation
      return srcClass;
    }
    return findOperation( (SrcLinkedClass)enclosingClass );
  }

  private void addQueryField( SrcLinkedClass srcClass, TypeDefinition ctx, Field field )
  {
    Optional<FieldDefinition> fieldDef;
    String alias = field.getAlias();
    String fieldName = field.getName();
    String name = alias == null ? fieldName : alias;
    if( ctx instanceof ObjectTypeDefinition )
    {
      fieldDef = ((ObjectTypeDefinition)ctx).getFieldDefinitions().stream()
        .filter( e -> e.getName().equals( fieldName ) ).findFirst();
    }
    else if( fieldName.equals( "__typename" ) )
    {
      // The discriminator for the union
      String propName = name;
      SrcType type = new SrcType( String.class.getSimpleName() );
      propName = makeIdentifier( propName, true );
      //noinspection unused
      StringBuilder propertyType = type.render( new StringBuilder(), 0, false );
      //noinspection unused
      StringBuilder componentType = getComponentType( type ).render( new StringBuilder(), 0, false );
      SrcGetProperty getter = new SrcGetProperty( propName, type );
      addActualNameAnnotation( getter, name, true );
      addSourcePositionAnnotation( srcClass, field, name, getter );
      srcClass.addGetProperty( getter ).modifiers( Modifier.PUBLIC );

      //## no setters for queries?
//      SrcSetProperty setter = new SrcSetProperty( propName, type )
//        .modifiers( Flags.DEFAULT )
//        .body( "getBindings().put(\"$name\", " + RuntimeMethods.class.getSimpleName() + ".coerceToBindingValue(${'$'}value));\n" );
//      addActualNameAnnotation( setter, name, true );
//      addSourcePositionAnnotation( srcClass, field, name, setter );
//      srcClass.addSetProperty( setter ).modifiers( Modifier.PUBLIC );

      return;
    }
    else if( ctx instanceof InterfaceTypeDefinition )
    {
      fieldDef = ((InterfaceTypeDefinition)ctx).getFieldDefinitions().stream()
        .filter( e -> e.getName().equals( fieldName ) ).findFirst();
    }
    else
    {
      throw new UnsupportedOperationException( ctx.getName() );
    }

    SelectionSet childSelections = field.getSelectionSet();
    FieldDefinition fieldDefStatic = fieldDef.orElse( null );
    if( fieldDefStatic == null )
    {
      if( ctx instanceof ObjectTypeDefinition )
      {
        fieldDefStatic = getFromExtensions( (ObjectTypeDefinition)ctx, fieldName );
      }
      else if( ctx instanceof InterfaceTypeDefinition )
      {
        fieldDefStatic = getFromExtensions( (InterfaceTypeDefinition)ctx, fieldName );
      }

      if( fieldDefStatic == null )
      {
        _model.addIssue( new ValidationError( ValidationErrorType.FieldUndefined, field.getSourceLocation(),
          "GraphQL field '$fieldName' is not defined in the type '${ctx.getName()}' from scope '${_model.getScope().getName()}'" ) );
        return;
      }
    }

    Type type = fieldDefStatic.getType();
    if( childSelections == null || childSelections.getSelections().isEmpty() )
    {
      String propName = name;
      SrcType type1 = makeSrcType( srcClass, type, false );
      propName = makeIdentifier( propName, true );
      //noinspection unused
      StringBuilder propertyType = type1.render( new StringBuilder(), 0, false );
      //noinspection unused
      StringBuilder componentType = getComponentType( type1 ).render( new StringBuilder(), 0, false );
      SrcGetProperty getter = new SrcGetProperty( propName, type1 );
      addActualNameAnnotation( getter, name, true );
      addSourcePositionAnnotation( srcClass, field, name, getter );
      srcClass.addGetProperty( getter ).modifiers( Modifier.PUBLIC );

//## no setters for queries?
//      SrcSetProperty setter = new SrcSetProperty( propName, type1 )
//        .modifiers( Flags.DEFAULT )
//        .body( "getBindings().put(\"$name\", " + RuntimeMethods.class.getSimpleName() + ".coerceToBindingValue(${'$'}value));\n" );
//      addActualNameAnnotation( setter, name, true );
//      addSourcePositionAnnotation( srcClass, field, name, setter );
//      srcClass.addSetProperty( setter ).modifiers( Modifier.PUBLIC );
    }
    else
    {
      // inner interface of Result interface
      String identifier = makeIdentifier( name, false );
      String fqn = srcClass.getName() + '.' + identifier;
      SrcLinkedClass srcInnerResult = new SrcLinkedClass( fqn, srcClass, Interface );
      srcInnerResult
        .addInterface( GqlType.class.getSimpleName() )
        .addAnnotation( new SrcAnnotationExpression( Structural.class.getSimpleName() ) )
        .modifiers( Modifier.PUBLIC );
      addActualNameAnnotation( srcInnerResult, name, false );
      addSourcePositionAnnotation( srcClass, field, name, srcInnerResult );

      for( Selection member: childSelections.getSelections() )
      {
        TypeDefinition typeDef = findTypeDefinition( type );
        if( typeDef != null )
        {
          addQuerySelection( srcInnerResult, typeDef, member );
        }
      }
      srcClass.addInnerClass( srcInnerResult );

      // getter property
      String propName = name;
      SrcType type1 = convertSrcType( srcClass, type, srcInnerResult.getSimpleName() );
      propName = makeIdentifier( propName, true );
      //noinspection unused
      StringBuilder propertyType = type1.render( new StringBuilder(), 0, false );
      //noinspection unused
      StringBuilder componentType = getComponentType( type1 ).render( new StringBuilder(), 0, false );
      SrcGetProperty getter = new SrcGetProperty( propName, type1 );
      addActualNameAnnotation( getter, name, true );
      addSourcePositionAnnotation( srcClass, field, name, getter );
      srcClass.addGetProperty( getter ).modifiers( Modifier.PUBLIC );

//## no setters for queries?
//      SrcSetProperty setter = new SrcSetProperty( propName, type1 )
//        .modifiers( Flags.DEFAULT )
//        .body( "getBindings().put(\"$name\", " + RuntimeMethods.class.getSimpleName() + ".coerceToBindingValue(${'$'}value));\n" );
//      addActualNameAnnotation( setter, name, true );
//      addSourcePositionAnnotation( srcClass, field, name, setter );
//      srcClass.addSetProperty( setter ).modifiers( Modifier.PUBLIC );
    }
  }

  private FieldDefinition getFromExtensions( ObjectTypeDefinition ctx, String fieldName )
  {
    List<ObjectTypeExtensionDefinition> objectExtensions = _model.getTypeRegistry().objectTypeExtensions().get( ctx.getName() );
    if( objectExtensions != null )
    {
      for( ObjectTypeExtensionDefinition ext : objectExtensions )
      {
        List<FieldDefinition> extFieldDefinitions = ext.getFieldDefinitions();
        for( FieldDefinition fieldDef : extFieldDefinitions )
        {
          if( fieldDef.getName().equals( fieldName ) )
          {
            return fieldDef;
          }
        }
      }
    }
    return null;
  }

  private FieldDefinition getFromExtensions( InterfaceTypeDefinition ctx, String fieldName )
  {
    List<InterfaceTypeExtensionDefinition> objectExtensions = _model.getTypeRegistry().interfaceTypeExtensions().get( ctx.getName() );
    if( objectExtensions != null )
    {
      for( InterfaceTypeExtensionDefinition ext : objectExtensions )
      {
        List<FieldDefinition> extFieldDefinitions = ext.getFieldDefinitions();
        for( FieldDefinition fieldDef : extFieldDefinitions )
        {
          if( fieldDef.getName().equals( fieldName ) )
          {
            return fieldDef;
          }
        }
      }
    }
    return null;
  }

  /**
   * Searches globally for a type i.e., across all .graphql files.
   */
  private TypeDefinition findTypeDefinition( Type type )
  {
    TypeName componentType = (TypeName)getComponentType( type );
    return _model.getScope().findTypeDefinition( componentType.getName() );
  }

  /**
   * Searches globally for a scalar type i.e., across all .graphql files.
   */
  private ScalarTypeDefinition findScalarTypeDefinition( TypeName type )
  {
    TypeName componentType = (TypeName)getComponentType( type );
    return _model.getScope().findScalarTypeDefinition( componentType.getName() );
  }

  private void addMember( SrcLinkedClass srcClass, FieldDefinition member, Predicate<String> duplicateChecker )
  {
    Type type = member.getType();
    String name = makeIdentifier( member.getName(), false );
    addMember( srcClass, member, type, name, duplicateChecker );
  }

  private void addMember( SrcLinkedClass srcClass, InputValueDefinition member, Predicate<String> duplicateChecker )
  {
    Type type = member.getType();
    String name = makeIdentifier( member.getName(), false );
    addMember( srcClass, member, type, name, duplicateChecker );
  }

  private void addMember( SrcLinkedClass srcClass, NamedNode member, Type type, String name,
                          Predicate<String> duplicateChecker )
  {
    SrcType getterType = makeSrcType( srcClass, type, false );
    SrcType setterType = makeSrcType( srcClass, type, false, true );
    String propName = makeIdentifier( name, true );
    if( !propName.equals( name ) && duplicateChecker.test( propName ) )
    {
      // There are two fields that differ in name only by the case of the first character "Foo" v. "foo".
      // Since the get/set methods capitalize the name, we must differentiate the method names
      // e.g., getFoo() and get_foo()
      propName = '_' + makeIdentifier( name, false );
    }
//    //noinspection unused
//    StringBuilder propertyType = getterType.render( new StringBuilder(), 0, false );
//    //noinspection unused
//    StringBuilder componentType = getComponentType( getterType ).render( new StringBuilder(), 0, false );
    SrcGetProperty getter = new SrcGetProperty( propName, getterType );
    addActualNameAnnotation( getter, name, true );
    if( member != null )
    {
      addSourcePositionAnnotation( srcClass, member, name, getter );
    }
    srcClass.addGetProperty( getter ).modifiers( Modifier.PUBLIC );

    SrcSetProperty setter = new SrcSetProperty( propName, setterType );
    addActualNameAnnotation( setter, name, true );
    if( member != null )
    {
      addSourcePositionAnnotation( srcClass, member, name, setter );
    }
    srcClass.addSetProperty( setter ).modifiers( Modifier.PUBLIC );
  }

  private String findLub( UnionTypeDefinition typeDef )
  {
    Set<Set<InterfaceTypeDefinition>> ifaces = new HashSet<>();
    for( Type t: typeDef.getMemberTypes() )
    {
      TypeDefinition td = findTypeDefinition( t );
      if( td instanceof ObjectTypeDefinition )
      {
        ifaces.add( ((ObjectTypeDefinition)td).getImplements().stream()
          .map( type -> (InterfaceTypeDefinition)findTypeDefinition( type ) )
          .collect( Collectors.toSet() ) );
      }
      else if( td instanceof InterfaceTypeDefinition )
      {
        ifaces.add( Collections.singleton( (InterfaceTypeDefinition)td ) );
      }
    }
    //noinspection OptionalGetWithoutIsPresent
    Set<InterfaceTypeDefinition> intersection =
      ifaces.stream().reduce( ( p1, p2 ) -> {
        p1.retainAll( p2 );
        return p1;
      } ).get();
    if( intersection.isEmpty() )
    {
      // no common interface
      return null;
    }
    else
    {
      // can only use one
      return intersection.iterator().next().getName();
    }
  }

  private ListType getListType( Type type )
  {
    if( type instanceof ListType )
    {
      return (ListType)type;
    }
    if( type instanceof TypeName )
    {
      return null;
    }
    return getListType( ((NonNullType)type).getType() );
  }

  private SrcType getComponentType( SrcType type )
  {
    List<SrcType> typeParams = type.getTypeParams();
    if( !typeParams.isEmpty() )
    {
      return getComponentType( typeParams.get( 0 ) );
    }
    return type;
  }

  private void addSourcePositionAnnotation( SrcLinkedClass srcClass, NamedNode node, SrcAnnotated srcAnno )
  {
    addSourcePositionAnnotation( srcClass, node, node::getName, srcAnno );
  }

  private void addSourcePositionAnnotation( SrcLinkedClass srcClass, Node node, Supplier<String> name, SrcAnnotated srcAnno )
  {
    addSourcePositionAnnotation( srcClass, node, name.get(), srcAnno );
  }

  private SourceLocation getActualSourceLocation( SrcLinkedClass srcClass, Node node )
  {
    final SourceLocation[] loc = {node.getSourceLocation()};
    srcClass.processContent( loc[0].getLine(), loc[0].getColumn(), ( content, offset) -> {
      int endComment;
      if( content.startsWith( "\"\"\"" ) )
      {
        endComment = content.indexOf( "\"\"\"", offset + 3 );
        if( endComment > 0 )
        {
          endComment += 3;
        }
      }
      else
      {
        endComment = offset;
      }
      loc[0] = adjustLocation( node, content, offset, endComment );
    } );
    return loc[0];
  }

  private SourceLocation adjustLocation( Node node, String content, Integer offset, int commentEnd )
  {
    SourceLocation loc = node.getSourceLocation();
    String name;
    if( node instanceof NamedNode && !content.startsWith( name = ensure$included( (NamedNode)node ), offset ) ||
        node instanceof OperationDefinition && !content.startsWith( name = ((OperationDefinition)node).getName(), offset )  )
    {
      int nameStart = content.indexOf( ' ' + name, commentEnd );
      if( nameStart > 0 )
      {
        nameStart++; // skip space
        int line = loc.getLine();
        int lastLinebreak = 0;
        for( int i = offset; i < nameStart; i++ )
        {
          if( content.charAt( i ) == '\n' )
          {
            line++;
            lastLinebreak = i;
          }
        }

        int column;
        if( lastLinebreak > 0 )
        {
          column = nameStart - lastLinebreak;
        }
        else
        {
          column = loc.getColumn();
          column += nameStart - commentEnd;
        }
        loc = new SourceLocation( line, column, loc.getSourceName() );
      }
    }
    return loc;
  }

  private void addSourcePositionAnnotation( SrcLinkedClass srcClass, Node node, String name, SrcAnnotated srcAnno )
  {
    SourceLocation loc = getActualSourceLocation( srcClass, node );
    srcClass.addSourcePositionAnnotation( srcAnno, name, loc.getLine(), loc.getColumn() );
  }

  @SuppressWarnings("unused")
  private String getStartSymbol( Node node )
  {
    String start = "";
    if( node instanceof TypeDefinition )
    {
      if( node instanceof ObjectTypeDefinition )
      {
        start = "type";
      }
      else if( node instanceof EnumTypeDefinition )
      {
        start = "enum";
      }
      else if( node instanceof InputObjectTypeDefinition )
      {
        start = "input";
      }
      else if( node instanceof InterfaceTypeDefinition )
      {
        start = "interface";
      }
      else if( node instanceof ScalarTypeDefinition )
      {
        start = "scalar";
      }
      else if( node instanceof UnionTypeDefinition )
      {
        start = "union";
      }
    }
    else if( node instanceof OperationDefinition )
    {
      start = ((OperationDefinition)node).getOperation().name().toLowerCase();
    }
    return start;
  }

  private Type getComponentType( Type type )
  {
    if( type instanceof ListType )
    {
      return getComponentType( ((ListType)type).getType() );
    }
    if( type instanceof NonNullType )
    {
      return getComponentType( ((NonNullType)type).getType() );
    }
    return type;
  }

  private String convertType( SrcLinkedClass owner, Type type, String component )
  {
    ListType listType = getListType( type );
    if( listType != null )
    {
      return List.class.getSimpleName() + '<' + convertType( owner, listType.getType(), component ) + '>';
    }
    return owner.getDisambiguatedNameInNest( component );
  }

  private SrcType convertSrcType( SrcLinkedClass owner, Type type, String component )
  {
    SrcType srcType = new SrcType( convertType( owner, type, component ) );
    if( type instanceof NonNullType )
    {
      srcType.addAnnotation( new SrcAnnotationExpression( NotNull.class.getSimpleName() ) );
    }
    return srcType;
  }

  private SrcType makeSrcType( SrcLinkedClass owner, Type type, boolean typeParam )
  {
    return makeSrcType( owner, type, typeParam, false );
  }
  private SrcType makeSrcType( SrcLinkedClass owner, Type type, boolean typeParam, boolean isParameter )
  {
    SrcType srcType;
    if( type instanceof ListType )
    {
      srcType = new SrcType( isParameter ? "List" : "IListBacked" );
      srcType.addTypeParam( makeSrcType( owner, ((ListType)type).getType(), true, isParameter ) );
    }
    else if( type instanceof TypeName )
    {
      String typeName = getJavaClassName( owner, (TypeName)type, typeParam );
      srcType = new SrcType( typeName );
      if( !typeParam )
      {
        Class<?> javaClass = getJavaClass( (TypeName)type, false );
        srcType.setPrimitive( javaClass != null && javaClass.isPrimitive() );
      }
    }
    else if( type instanceof NonNullType )
    {
      Type theType = ((NonNullType)type).getType();
      srcType = makeSrcType( owner, theType, typeParam, isParameter );
      if( !typeParam && !srcType.isPrimitive() )
      {
        srcType.addAnnotation( new SrcAnnotationExpression( NotNull.class.getSimpleName() ) );
      }
    }
    else
    {
      throw new IllegalStateException( "Unhandled type: " + type.getClass().getTypeName() );
    }
    return srcType;
  }

  private String getJavaClassName( SrcLinkedClass owner, TypeName type, boolean boxed )
  {
    ScalarTypeDefinition scalarType = findScalarTypeDefinition( type );
    if( scalarType == null )
    {
      // not a scalar type, therefore it must be a 'type'
      return makeIdentifier( owner.getDisambiguatedNameInNest( type.getName() ), false );
    }

    Class<?> cls = getJavaClass( type, boxed );
    return cls == null ? scalarType.getName() : getJavaName( cls );
  }

  @NotNull
  private String getJavaName( Class<?> cls )
  {
    if( cls == String.class )
    {
      return String.class.getSimpleName();
    }
    return cls.getTypeName();
  }

  @Nullable
  private Class<?> getJavaClass( TypeName type, boolean boxed )
  {
    Class<?> cls;
    switch( type.getName() )
    {
      case "String":
      case "ID":
        cls = String.class;
        break;
      case "Byte":
        cls = boxed ? Byte.class : byte.class;
        break;
      case "Char":
      case "Character":
        cls = boxed ? Character.class : char.class;
        break;
      case "Int":
      case "Integer":
        cls = boxed ? Integer.class : int.class;
        break;
      case "Long":
        cls = boxed ? Long.class : long.class;
        break;
      case "Float":
      case "Double":
        cls = boxed ? Double.class : double.class;
        break;
      case "Boolean":
        cls = boxed ? Boolean.class : boolean.class;
        break;
      case "BigInteger":
        cls = BigInteger.class;
        break;
      case "BigDecimal":
        cls = BigDecimal.class;
        break;
      // permit arbitrary bindings as a "json" object
      case "json":
      case "JSON":
        cls = Bindings.class;
        break;
      default:
        cls = findFormatType( type.getName() );
    }
    return cls;
  }

  private java.lang.Class<?> findFormatType( String formatName )
  {
    // must use *this* class loader so that, in the case of IJ plugin, any custom ICoercionProviders will be included
    ClassLoader prevLoader = Thread.currentThread().getContextClassLoader();
    ReflectUtil.setContextClassLoader( getClass().getClassLoader() );
    try
    {
      for( ICoercionProvider coercer : CoercionProviders.get() )
      {
        if( coercer instanceof IJsonFormatTypeCoercer )
        {
          Class<?> javaType = ((IJsonFormatTypeCoercer)coercer).getFormats().get( formatName );
          if( javaType != null )
          {
            return javaType;
          }
        }
      }
      return findJsonEquivalent( formatName );
    }
    finally
    {
      ReflectUtil.setContextClassLoader( prevLoader );
    }
  }

  private Class<?> findJsonEquivalent( String scalarName )
  {
    switch( scalarName )
    {
      case "Time":
      case "LocalTime":
        return findFormatType( "time" );
      case "Date":
      case "LocalDate":
        return findFormatType( "date" );
      case "DateTime":
      case "LocalDateTime":
        return findFormatType( "date-time" );
      case "Instant":
        return findFormatType( "utc-millisec" );
      case "Base64":
        return findFormatType( "binary" );
      case "Binary":
      case "Octet":
        return findFormatType( "byte" );
    }
    return String.class;
  }
}
