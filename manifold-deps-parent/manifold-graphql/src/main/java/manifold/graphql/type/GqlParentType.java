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
import graphql.language.AstPrinter;
import graphql.language.Definition;
import graphql.language.EnumTypeDefinition;
import graphql.language.EnumValueDefinition;
import graphql.language.Field;
import graphql.language.FieldDefinition;
import graphql.language.FragmentDefinition;
import graphql.language.FragmentSpread;
import graphql.language.InlineFragment;
import graphql.language.InputObjectTypeDefinition;
import graphql.language.InputValueDefinition;
import graphql.language.InterfaceTypeDefinition;
import graphql.language.ListType;
import graphql.language.NamedNode;
import graphql.language.Node;
import graphql.language.NonNullType;
import graphql.language.ObjectTypeDefinition;
import graphql.language.OperationDefinition;
import graphql.language.OperationTypeDefinition;
import graphql.language.ScalarTypeDefinition;
import graphql.language.SchemaDefinition;
import graphql.language.Selection;
import graphql.language.SelectionSet;
import graphql.language.SourceLocation;
import graphql.language.Type;
import graphql.language.TypeDefinition;
import graphql.language.TypeName;
import graphql.language.UnionTypeDefinition;
import graphql.language.VariableDefinition;
import graphql.schema.idl.TypeDefinitionRegistry;
import java.lang.Class;
import java.lang.reflect.Modifier;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import javax.script.Bindings;
import manifold.api.fs.IFile;
import manifold.api.gen.SrcAnnotated;
import manifold.api.gen.SrcAnnotationExpression;
import manifold.api.gen.SrcConstructor;
import manifold.api.gen.SrcField;
import manifold.api.gen.SrcGetProperty;
import manifold.api.gen.SrcLinkedClass;
import manifold.api.gen.SrcMethod;
import manifold.api.gen.SrcParameter;
import manifold.api.gen.SrcRawStatement;
import manifold.api.gen.SrcSetProperty;
import manifold.api.gen.SrcStatementBlock;
import manifold.api.gen.SrcType;
import manifold.api.json.IJsonBindingsBacked;
import manifold.api.json.schema.FormatTypeResolvers;
import manifold.api.json.schema.IJsonFormatTypeResolver;
import manifold.api.json.schema.JsonFormatType;
import manifold.api.templ.DisableStringLiteralTemplates;
import manifold.api.type.ActualName;
import manifold.api.type.SourcePosition;
import manifold.ext.DataBindings;
import manifold.ext.RuntimeMethods;
import manifold.ext.api.IBindingType;
import manifold.ext.api.IProxyFactory;
import manifold.ext.api.Structural;
import manifold.graphql.request.Executor;
import manifold.util.ManEscapeUtil;
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
  private final String _fqn;
  private final SchemaDefinition _schemaDefinition;
  private final TypeDefinitionRegistry _registry;
  private final Map<String, OperationDefinition> _operations;
  private final Map<String, FragmentDefinition> _fragments;
  private final IFile _file;
  private final GqlManifold _gqlManifold;

  GqlParentType( String fqn, SchemaDefinition schemaDefinition, TypeDefinitionRegistry registry,
                 Map<String, OperationDefinition> operations, Map<String, FragmentDefinition> fragments,
                 IFile file, GqlManifold gqlManifold )
  {
    _fqn = fqn;
    _schemaDefinition = schemaDefinition;
    _registry = registry;
    _operations = operations;
    _fragments = fragments;
    _file = file;
    _gqlManifold = gqlManifold;
  }

  private String getFqn()
  {
    return _fqn;
  }

  boolean hasChild( String childName )
  {
    return _registry.getType( childName ).isPresent();
  }

  void render( StringBuilder sb )
  {
    SrcLinkedClass srcClass = new SrcLinkedClass( getFqn(), Interface, _file )
      .modifiers( Modifier.PUBLIC );
    addImports( srcClass );
    addInnerTypes( srcClass );
    addInnerOperations( srcClass );
    srcClass.render( sb, 0 );
  }

  private void addInnerTypes( SrcLinkedClass srcClass )
  {
    for( TypeDefinition type: _registry.types().values() )
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
    }
  }

  private void addInnerOperations( SrcLinkedClass srcClass )
  {
    for( OperationDefinition operation: _operations.values() )
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

  private void addProxyClasses( SrcLinkedClass enclosingType )
  {
    addProxy( enclosingType );
    addProxyFactory( enclosingType );
  }

  /**
   * Not so much a "proxy" as a substitute for a structural proxy that is otherwise generated dynamically at runtime.
   * Essentially this class is a compile-time substitute that vastly improves the first-time load performance of
   * GraphQL types.
   */
  private void addProxy( SrcLinkedClass enclosingType )
  {
    String fqn = enclosingType.getName() + ".Proxy";
    SrcLinkedClass srcClass = new SrcLinkedClass( fqn, enclosingType, Class )
      .addInterface( enclosingType.getName() )
      .addField( new SrcField( "_bindings", Bindings.class )
        .modifiers( Modifier.PRIVATE | Modifier.FINAL ) )
      .addConstructor( new SrcConstructor()
        .modifiers( Modifier.PRIVATE )
        .addParam( "bindings", Bindings.class )
        .body( new SrcStatementBlock().addStatement( new SrcRawStatement().rawText( "_bindings = bindings;" ) ) ) )
      .addGetProperty( new SrcGetProperty( "Bindings", Bindings.class )
        .modifiers( Modifier.PUBLIC )
        .body( new SrcStatementBlock().addStatement( new SrcRawStatement().rawText( "return _bindings;" ) ) ) );
    enclosingType.addInnerClass( srcClass );
  }

  private void addProxyFactory( SrcLinkedClass enclosingType )
  {
    String fqn = enclosingType.getName() + ".ProxyFactory";
    SrcLinkedClass srcClass = new SrcLinkedClass( fqn, enclosingType, Class )
      .addInterface( new SrcType( "IProxyFactory<Map, ${enclosingType.getSimpleName()}>" ) )
      .addMethod( new SrcMethod()
        .modifiers( Modifier.PUBLIC )
        .name( "proxy" )
        .returns( new SrcType( enclosingType.getSimpleName() ) )
        .addParam( "bindings", Map.class )
        .addParam( "iface", new SrcType( "Class<${enclosingType.getSimpleName()}>" ) )
        .body( new SrcStatementBlock()
          .addStatement( new SrcRawStatement().rawText(
            "if(!(bindings instanceof Bindings)) {bindings = new DataBindings(bindings);}" ) )
          .addStatement( new SrcRawStatement().rawText(
            "return new Proxy((Bindings)bindings);" ) ) ) );
    enclosingType.addInnerClass( srcClass );
  }

  private void addQueryType( OperationDefinition operation, SrcLinkedClass enclosingType )
  {
    String fqn = getFqn() + '.' + operation.getName();
    SrcLinkedClass srcClass = new SrcLinkedClass( fqn, enclosingType, Interface )
      .addInterface( IJsonBindingsBacked.class )
      .addAnnotation( new SrcAnnotationExpression( Structural.class )
        .addArgument( "factoryClass", Class.class, fqn + ".ProxyFactory.class" ) )
      .modifiers( Modifier.PUBLIC );
    addSourcePositionAnnotation( srcClass, operation, operation::getName, srcClass );
    addProxyClasses( srcClass );
    addQueryResultType( operation, getRoot( operation.getOperation() ), srcClass );
    addBuilder( srcClass, operation );
    addCreateMethod( srcClass, operation );
    addBuilderMethod( srcClass, operation );
    addRequestMethod( srcClass, operation );

    for( VariableDefinition varDef: operation.getVariableDefinitions() )
    {
      addProperty( srcClass, varDef, remove$( varDef.getName() ), varDef.getName(),
        makeSrcType( varDef.getType(), false ) );
    }

    enclosingType.addInnerClass( srcClass );
  }

  private String remove$( String name )
  {
    if( name.charAt( 0 ) == '$' )
    {
      return name.substring( 1 );
    }
    return name;
  }

  private void addRequestMethod( SrcLinkedClass srcClass, OperationDefinition operation )
  {
    //noinspection unused
    String query = ManEscapeUtil.escapeForJavaStringLiteral( AstPrinter.printAstCompact( operation ) );
    srcClass.addMethod( new SrcMethod()
      .addAnnotation( new SrcAnnotationExpression( DisableStringLiteralTemplates.class.getSimpleName() ) )
      .modifiers( Flags.DEFAULT )
      .name( "request" )
      .addParam( "url", String.class )
      .returns( new SrcType( "Executor<Result>" ) )
      .body( new SrcStatementBlock().addStatement( new SrcRawStatement().rawText(
        "return new Executor<Result>(url, \"$query\", getBindings());"
      ) ) ) );
  }
  
  private void addCreateMethod( SrcLinkedClass srcClass, Definition definition )
  {
    String simpleName = srcClass.getSimpleName();
    SrcMethod method = new SrcMethod( srcClass )
      .modifiers( Modifier.PUBLIC | Modifier.STATIC )
      .name( "create" )
      .returns( simpleName );
    for( NamedNode node: getDefinitions( definition ) )
    {
      if( isRequiredVar( node ) )
      {
        method.addParam( remove$( node.getName() ), makeSrcType( getType( node ), false ) );
      }
    }
    srcClass.addMethod( method );

    SrcStatementBlock block = new SrcStatementBlock();
    block.addStatement( new SrcRawStatement().rawText( "$simpleName thiz = ($simpleName)new DataBindings();" ) );
    for( NamedNode node: getDefinitions( definition ) )
    {
      String name = remove$( node.getName() );
      //noinspection unused
      String Prop = makeIdentifier( name, true );
      if( isRequiredVar( node ) )
      {
        block.addStatement( new SrcRawStatement().rawText( "thiz.set$Prop($name);" ) );
      }
    }
    block.addStatement( new SrcRawStatement().rawText( "return thiz;" ) );
    method.body( block );
  }

  private void addBuilderMethod( SrcLinkedClass srcClass, Definition definition )
  {
    SrcMethod method = new SrcMethod( srcClass )
      .modifiers( Modifier.PUBLIC | Modifier.STATIC )
      .name( "builder" )
      .returns( new SrcType( "Builder" ) );
    for( NamedNode node: getDefinitions( definition ) )
    {
      if( isRequiredVar( node ) )
      {
        method.addParam( remove$( node.getName() ), makeSrcType( getType( node ), false ) );
      }
    }
    srcClass.addMethod( method );

    StringBuilder sb = new StringBuilder();
    sb.append( "return new Builder(" );
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

    SrcStatementBlock block = new SrcStatementBlock();
    block.addStatement( new SrcRawStatement().rawText( sb.toString() ) );
    method.body( block );
  }

  private void addBuilder( SrcLinkedClass enclosingType, Definition definition )
  {
    SrcConstructor ctor;
    String fqn = enclosingType.getName() + ".Builder";
    SrcLinkedClass srcClass = new SrcLinkedClass( fqn, enclosingType, Class )
      .modifiers( Modifier.STATIC )
      .addField( new SrcField( "_result", new SrcType( enclosingType.getSimpleName() ) )
        .modifiers( Modifier.PRIVATE | Modifier.FINAL ) )
      .addConstructor( ctor = new SrcConstructor()
        .modifiers( Modifier.PRIVATE ) )
      .addMethod( new SrcMethod()
        .modifiers( Modifier.PUBLIC )
        .name( "build" )
        .returns( enclosingType.getSimpleName() )
        .body( new SrcStatementBlock().addStatement( new SrcRawStatement().rawText(
          "return _result;"
        ) ) ) );

    enclosingType.addInnerClass( srcClass );

    for( NamedNode node: getDefinitions( definition ) )
    {
      if( isRequiredVar( node ) )
      {
        ctor.addParam( remove$( node.getName() ), makeSrcType( getType( node ), false ) );
      }
    }
    ctor.body( new SrcStatementBlock().addStatement(
      new SrcRawStatement().rawText( addBuilderConstructorBody( ctor ) ) ) );

    addWithMethods( srcClass, definition );
  }

  private String addBuilderConstructorBody( SrcConstructor ctor )
  {
    StringBuilder sb = new StringBuilder();
    sb.append( "_result = create(" );
    int count = 0;
    for( SrcParameter param: ctor.getParameters() )
    {
      if( count++ > 0 )
      {
        sb.append( ", " );
      }
      //noinspection unused
      sb.append( param.getSimpleName() );
    }
    sb.append( ");" );
    return sb.toString();
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
      TypeDefinition typeDef = findTypeDefinition( type );
      if( typeDef instanceof UnionTypeDefinition )
      {
        addUnionOverloads_With( srcClass, node, propName, type, (UnionTypeDefinition)typeDef );

        addWithMethod( srcClass, node, propName, convertSrcType( type, findLub( (UnionTypeDefinition)typeDef ) ) );
      }
      else
      {
        addWithMethod( srcClass, node, propName, makeSrcType( type, false ) );
      }
    }
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

  private void addWithMethod( SrcLinkedClass srcClass, NamedNode node, String propName, SrcType type )
  {
    //noinspection unused
    String actualName = node.getName();
    //noinspection unused
    StringBuilder propertyType = type.render( new StringBuilder(), 0, false );
    SrcMethod withMethod = new SrcMethod()
      .modifiers( Flags.PUBLIC )
      .name( "with$propName" )
      .addParam( "${'$'}value", type )
      .returns( new SrcType( "Builder" ) )
      .body( new SrcStatementBlock().addStatement( new SrcRawStatement().rawText(
        "_result.getBindings().put(\"$actualName\", " + RuntimeMethods.class.getSimpleName() + ".coerceToBindingValue(${'$'}value));" ) )
        .addStatement( new SrcRawStatement().rawText(
          "return this;"
        ) ) );
    addActualNameAnnotation( withMethod, actualName, true );
    addSourcePositionAnnotation( srcClass, node, actualName, withMethod );

    srcClass.addMethod( withMethod );
  }

  private void addUnionOverloads_With( SrcLinkedClass srcClass, NamedNode node, String propName,
                                       Type maybeListType, UnionTypeDefinition typeDef )
  {
    if( getListType( maybeListType ) != null )
    {
      // can't have heterogeneous list (no union support for type args)
      // however the caller can instead get the LUB of the union type (if the type's implement a common interface)
      return;
    }

    for( Type t: typeDef.getMemberTypes() )
    {
      TypeDefinition td = findTypeDefinition( t );
      if( td instanceof UnionTypeDefinition )
      {
        addUnionOverloads_With( srcClass, node, propName, maybeListType, (UnionTypeDefinition)td );
      }
      else
      {
        SrcType unionMemberType = convertSrcType( maybeListType, makeSrcType( t, false ).getName() );
        String propertyName = "${propName}As${makeIdentifier( unionMemberType.getFqName(), true )}";
        addWithMethod( srcClass, node, propertyName, unionMemberType );
      }
    }
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
    SchemaDefinition schemaDefinition = findSchemaDefinition();
    if( schemaDefinition != null )
    {
      Optional<OperationTypeDefinition> rootQueryType =
        schemaDefinition.getOperationTypeDefinitions().stream()
          .filter( e -> e.getName().equals( getOperationKey( operation ) ) ).findFirst();
      if( rootQueryType.isPresent() )
      {
        Type type = rootQueryType.get().getType();
        root = findTypeDefinition( type );
      }
    }
    if( root == null )
    {
      // e.g., by convention a 'type' named "Query" is considered the root query type
      // if one is not specified in the 'schema'
      root = _gqlManifold.findTypeDefinition( getOperationDefaultTypeName( operation ) );
    }
    return root;
  }

  private String getOperationDefaultTypeName( OperationDefinition.Operation operation )
  {
    switch( operation )
    {
      case QUERY:
        return "Query";
      case MUTATION:
        return "Mutation";
      case SUBSCRIPTION:
        return "Subscription";
    }
    throw new IllegalStateException();
  }

  @NotNull
  private String getOperationKey( OperationDefinition.Operation operation )
  {
    switch( operation )
    {
      case QUERY:
        return "query";
      case MUTATION:
        return "mutation";
      case SUBSCRIPTION:
        return "subscription";
    }
    throw new IllegalStateException();
  }

  private void addQueryResultType( OperationDefinition operation, TypeDefinition ctx, SrcLinkedClass enclosingType )
  {
    String fqn = enclosingType.getName() + ".Result";
    SrcLinkedClass srcClass = new SrcLinkedClass( fqn, enclosingType, Interface )
      .addInterface( IJsonBindingsBacked.class )
      .addAnnotation( new SrcAnnotationExpression( Structural.class )
        .addArgument( "factoryClass", Class.class, fqn + ".ProxyFactory.class" ) )
      .modifiers( Modifier.PUBLIC );

    addProxyClasses( srcClass );

    for( Selection member: operation.getSelectionSet().getSelections() )
    {
      addQuerySelection( srcClass, ctx, member );
    }

    enclosingType.addInnerClass( srcClass );
  }

  private void addImports( SrcLinkedClass srcClass )
  {
    importAllOtherGqlTypes( srcClass );
    srcClass.addImport( Bindings.class );
    srcClass.addImport( Executor.class );
    srcClass.addImport( DataBindings.class );
    srcClass.addImport( IBindingType.class );
    srcClass.addImport( IJsonBindingsBacked.class );
    srcClass.addImport( IProxyFactory.class );
    srcClass.addImport( List.class );
    srcClass.addImport( Map.class );
    srcClass.addImport( RuntimeMethods.class );
    srcClass.addImport( NotNull.class );
    srcClass.addImport( ActualName.class );
    srcClass.addImport( DisableStringLiteralTemplates.class );
    srcClass.addImport( SourcePosition.class );
    srcClass.addImport( Structural.class );
  }

  private void importAllOtherGqlTypes( SrcLinkedClass srcClass )
  {
    _gqlManifold.getAllTypeNames().forEach( fqn -> {
      if( !fqn.equals( getFqn() ) )
      {
        srcClass.addStaticImport( "$fqn.*" );
      }
    } );
  }

  private void addInnerObjectType( ObjectTypeDefinition type, SrcLinkedClass enclosingType )
  {
    String fqn = getFqn() + '.' + type.getName();
    SrcLinkedClass srcClass = new SrcLinkedClass( fqn, enclosingType, Interface )
      .addInterface( IJsonBindingsBacked.class )
      .addAnnotation( new SrcAnnotationExpression( Structural.class )
        .addArgument( "factoryClass", Class.class, fqn + ".ProxyFactory.class" ) )
      .modifiers( Modifier.PUBLIC );
    addSourcePositionAnnotation( srcClass, type, srcClass );
    List<Type> interfaces = type.getImplements();
    addInterfaces( srcClass, interfaces );
    addProxyClasses( srcClass );
    for( FieldDefinition member: type.getFieldDefinitions() )
    {
      addMember( srcClass, member );
    }
    enclosingType.addInnerClass( srcClass );
  }

  private void addInnerInputType( InputObjectTypeDefinition type, SrcLinkedClass enclosingType )
  {
    String fqn = getFqn() + '.' + type.getName();
    SrcLinkedClass srcClass = new SrcLinkedClass( fqn, enclosingType, Interface )
      .addInterface( IJsonBindingsBacked.class )
      .addAnnotation( new SrcAnnotationExpression( Structural.class )
        .addArgument( "factoryClass", Class.class, fqn + ".ProxyFactory.class" ) )
      .modifiers( Modifier.PUBLIC );
    addSourcePositionAnnotation( srcClass, type, srcClass );
    addProxyClasses( srcClass );
    addBuilder( srcClass, type );
    addCreateMethod( srcClass, type );
    addBuilderMethod( srcClass, type );
    for( InputValueDefinition member: type.getInputValueDefinitions() )
    {
      addMember( srcClass, member );
    }
    enclosingType.addInnerClass( srcClass );
  }

  private void addInnerInterfaceType( InterfaceTypeDefinition type, SrcLinkedClass enclosingType )
  {
    String fqn = getFqn() + '.' + type.getName();
    SrcLinkedClass srcClass = new SrcLinkedClass( fqn, enclosingType, Interface )
      .addInterface( IJsonBindingsBacked.class )
      .addAnnotation( new SrcAnnotationExpression( Structural.class )
        .addArgument( "factoryClass", Class.class, fqn + ".ProxyFactory.class" ) )
      .modifiers( Modifier.PUBLIC );
    addSourcePositionAnnotation( srcClass, type, srcClass );
    addProxyClasses( srcClass );
    for( FieldDefinition member: type.getFieldDefinitions() )
    {
      addMember( srcClass, member );
    }
    enclosingType.addInnerClass( srcClass );
  }

  private void addInnerEnumType( EnumTypeDefinition type, SrcLinkedClass enclosingType )
  {
    SrcLinkedClass srcClass = new SrcLinkedClass( getFqn() + '.' + type.getName(), enclosingType, Enum )
      .modifiers( Modifier.PUBLIC )
      .addInterface( IBindingType.class );
    addSourcePositionAnnotation( srcClass, type, srcClass );
    for( EnumValueDefinition member: type.getEnumValueDefinitions() )
    {
      SrcField enumConst = new SrcField( member.getName(), type.getName() );
      srcClass.addEnumConst( enumConst );
    }
    srcClass.addMethod(
      new SrcMethod( srcClass )
        .name( "toBindingValue" )
        .modifiers( Modifier.PUBLIC )
        .returns( Object.class )
        .body( new SrcStatementBlock()
          .addStatement( new SrcRawStatement().rawText( "return name();" ) ) ) );
    enclosingType.addInnerClass( srcClass );
  }

  private void addInterfaces( SrcLinkedClass srcClass, List<Type> interfaces )
  {
    for( Type iface: interfaces )
    {
      if( iface instanceof TypeName )
      {
        srcClass.addInterface( ((TypeName)iface).getName() );
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
      FragmentDefinition fragment = _fragments.get( ((FragmentSpread)selection).getName() );
      TypeDefinition fragmentCtx = findTypeDefinition( fragment.getTypeCondition() );
      assert fragmentCtx != null;
      for( Selection fragSelection: fragment.getSelectionSet().getSelections() )
      {
        addQuerySelection( srcClass, fragmentCtx, fragSelection );
      }
    }
    else if( selection instanceof InlineFragment )
    {
      InlineFragment inlineFragment = (InlineFragment)selection;
      TypeDefinition fragmentCtx = findTypeDefinition( inlineFragment.getTypeCondition() );
      assert fragmentCtx != null;
      for( Selection fragSelection: inlineFragment.getSelectionSet().getSelections() )
      {
        addQuerySelection( srcClass, fragmentCtx, fragSelection );
      }
    }
  }

  private void addQueryField( SrcLinkedClass srcClass, TypeDefinition ctx, Field field )
  {
    Optional<FieldDefinition> fieldDef;
    String alias = field.getAlias();
    String fieldName = field.getName();
    if( ctx instanceof ObjectTypeDefinition )
    {
      fieldDef = ((ObjectTypeDefinition)ctx).getFieldDefinitions().stream()
        .filter( e -> e.getName().equals( fieldName ) ).findFirst();
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
    String name = alias == null ? fieldName : alias;
    FieldDefinition fieldDefStatic = fieldDef.orElse( null );
    if( fieldDefStatic == null )
    {
      // assume the parser exposes the error
      return;
    }
    Type type = fieldDefStatic.getType();
    if( childSelections == null || childSelections.getSelections().isEmpty() )
    {
      addProperty( srcClass, field, name, name, makeSrcType( type, false ) );
    }
    else
    {
      addProperty( srcClass, field, name, name, convertSrcType( type, name ) );

      // inner interface of Result interface
      String fqn = srcClass.getName() + '.' + name;
      SrcLinkedClass srcInnerResult = new SrcLinkedClass( fqn, srcClass, Interface )
        .addInterface( IJsonBindingsBacked.class )
        .addAnnotation( new SrcAnnotationExpression( Structural.class )
          .addArgument( "factoryClass", Class.class, fqn + ".ProxyFactory.class" ) )
        .modifiers( Modifier.PUBLIC );
      addSourcePositionAnnotation( srcClass, field, () -> name, srcInnerResult );

      addProxyClasses( srcInnerResult );

      for( Selection member: childSelections.getSelections() )
      {
        TypeDefinition typeDef = findTypeDefinition( type );
        assert typeDef != null;
        addQuerySelection( srcInnerResult, typeDef, member );
      }

      srcClass.addInnerClass( srcInnerResult );
    }
  }

  /**
   * Searches globally for a type i.e., across all .graphql files.
   */
  private TypeDefinition findTypeDefinition( Type type )
  {
    TypeName componentType = (TypeName)getComponentType( type );
    TypeDefinition typeDefinition = _registry.getType( componentType ).orElse( null );
    if( typeDefinition != null )
    {
      return typeDefinition;
    }

    return _gqlManifold.findTypeDefinition( componentType.getName() );
  }

  /**
   * Searches globally for a scalar type i.e., across all .graphql files.
   */
  private ScalarTypeDefinition findScalarTypeDefinition( TypeName type )
  {
    TypeName componentType = (TypeName)getComponentType( type );
    ScalarTypeDefinition typeDefinition = _registry.scalars().get( componentType.getName() );
    if( typeDefinition != null )
    {
      return typeDefinition;
    }

    return _gqlManifold.findScalarTypeDefinition( componentType.getName() );
  }

  /**
   * Searches globally for the schema definition i.e., across all .graphql files.
   * Assuming only one is defined. todo: verify if 'schema' s/b defined only once
   */
  private SchemaDefinition findSchemaDefinition()
  {
    if( _schemaDefinition != null )
    {
      return _schemaDefinition;
    }

    return _gqlManifold.findSchemaDefinition();
  }

  private void addMember( SrcLinkedClass srcClass, FieldDefinition member )
  {
    Type type = member.getType();
    String name = makeIdentifier( member.getName(), false );
    addMember( srcClass, member, type, name );
  }

  private void addMember( SrcLinkedClass srcClass, InputValueDefinition member )
  {
    Type type = member.getType();
    String name = makeIdentifier( member.getName(), false );
    addMember( srcClass, member, type, name );
  }

  private void addMember( SrcLinkedClass srcClass, NamedNode member, Type type, String name )
  {
    TypeDefinition typeDef = findTypeDefinition( getComponentType( type ) );
    if( typeDef instanceof UnionTypeDefinition )
    {
      addUnionOverloads( srcClass, member, type, (UnionTypeDefinition)typeDef );

      addProperty( srcClass, member, name, name, convertSrcType( type, findLub( (UnionTypeDefinition)typeDef ) ) );
    }
    else
    {
      addProperty( srcClass, member, name, name, makeSrcType( type, false ) );
    }
  }

  @NotNull
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
      return Object.class.getSimpleName();
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

  private void addProperty( SrcLinkedClass srcClass, Node node, String propName, String actualName, SrcType type )
  {
    propName = makeIdentifier( propName, true );
    //noinspection unused
    StringBuilder propertyType = type.render( new StringBuilder(), 0, false );
    SrcGetProperty getter = new SrcGetProperty( propName, type )
      .modifiers( Flags.DEFAULT )
      .body( new SrcStatementBlock().addStatement( new SrcRawStatement().rawText(
        type.getTypeParams().isEmpty()
        ? "return ($propertyType)" + RuntimeMethods.class.getSimpleName() + ".coerce(getBindings().get(\"$actualName\"), ${propertyType}.class);"
        : "return ($propertyType)getBindings().get(\"$actualName\");" ) ) ); // List type, no coercion necessary
    if( node != null )
    {
      addActualNameAnnotation( getter, actualName, true );
      addSourcePositionAnnotation( srcClass, node, actualName, getter );
    }
    srcClass.addGetProperty( getter ).modifiers( Modifier.PUBLIC );

    SrcSetProperty setter = new SrcSetProperty( propName, type )
      .modifiers( Flags.DEFAULT )
      .body( new SrcStatementBlock().addStatement( new SrcRawStatement().rawText(
        "getBindings().put(\"$actualName\", " + RuntimeMethods.class.getSimpleName() + ".coerceToBindingValue(${'$'}value));\n" ) ) );
    if( node != null )
    {
      addActualNameAnnotation( setter, actualName, true );
      addSourcePositionAnnotation( srcClass, node, actualName, setter );
    }
    srcClass.addSetProperty( setter ).modifiers( Modifier.PUBLIC );
  }

  private void addUnionOverloads( SrcLinkedClass srcClass, NamedNode member, Type maybeListType, UnionTypeDefinition typeDef )
  {
    if( getListType( maybeListType ) != null )
    {
      // can't have heterogeneous list (no union support for type args)
      // however the caller can instead get the LUB of the union type (if the type's implement a common interface)
      return;
    }

    for( Type t: typeDef.getMemberTypes() )
    {
      TypeDefinition td = findTypeDefinition( t );
      if( td instanceof UnionTypeDefinition )
      {
        addUnionOverloads( srcClass, member, maybeListType, (UnionTypeDefinition)td );
      }
      else
      {
        SrcType unionMemberType = convertSrcType( maybeListType, makeSrcType( t, false ).getName() );
        String memberName = member.getName();
        String propertyName =
          "${makeIdentifier( memberName, true )}As${makeIdentifier( unionMemberType.getFqName(), true )}";
        addProperty( srcClass, member, propertyName, memberName, unionMemberType );
      }
    }
  }

  private void addSourcePositionAnnotation( SrcLinkedClass srcClass, NamedNode node, SrcAnnotated srcAnno )
  {
    addSourcePositionAnnotation( srcClass, node, node::getName, srcAnno );
  }

  private void addSourcePositionAnnotation( SrcLinkedClass srcClass, Node node, Supplier<String> name, SrcAnnotated srcAnno )
  {
    addSourcePositionAnnotation( srcClass, node, name.get(), srcAnno );
  }

  private void addSourcePositionAnnotation( SrcLinkedClass srcClass, Node node, String name, SrcAnnotated srcAnno )
  {
    SourceLocation loc = node.getSourceLocation();
    srcClass.addSourcePositionAnnotation( srcAnno, name, loc.getLine(), loc.getColumn() );
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

  private String convertType( Type type, String component )
  {
    ListType listType = getListType( type );
    if( listType != null )
    {
      return List.class.getSimpleName() + '<' + convertType( listType.getType(), component ) + '>';
    }
    return component;
  }

  private SrcType convertSrcType( Type type, String component )
  {
    SrcType srcType = new SrcType( convertType( type, component ) );
    if( type instanceof NonNullType )
    {
      srcType.addAnnotation( new SrcAnnotationExpression( NotNull.class ) );
    }
    return srcType;
  }

  private SrcType makeSrcType( Type type, boolean typeParam )
  {
    SrcType srcType;
    if( type instanceof ListType )
    {
      srcType = new SrcType( "List" );
      srcType.addTypeParam( makeSrcType( ((ListType)type).getType(), true ) );
    }
    else if( type instanceof TypeName )
    {
      String typeName = getJavaClassName( (TypeName)type, typeParam );
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
      srcType = makeSrcType( theType, typeParam );
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

  private String getJavaClassName( TypeName type, boolean boxed )
  {
    ScalarTypeDefinition scalarType = findScalarTypeDefinition( type );
    if( scalarType == null )
    {
      // not a scalar type, therefore it must be a 'type'
      return type.getName();
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
        cls = boxed ? Float.class : float.class;
        break;
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
      default:
        cls = findFormatType( type.getName() );
    }
    return cls;
  }

  private java.lang.Class<?> findFormatType( String scalarName )
  {
    for( IJsonFormatTypeResolver resolver: Objects.requireNonNull( FormatTypeResolvers.get() ) )
    {
      JsonFormatType resolvedType = resolver.resolveType( scalarName );
      if( resolvedType != null )
      {
        return resolvedType.getJavaType();
      }
    }
    return findJsonEquivalent( scalarName );
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
