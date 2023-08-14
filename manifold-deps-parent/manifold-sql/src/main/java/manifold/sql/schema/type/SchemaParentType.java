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

package manifold.sql.schema.type;

import com.sun.tools.javac.code.Flags;
import manifold.api.gen.*;
import manifold.api.host.IModule;
import manifold.json.rt.api.*;
import manifold.rt.api.*;
import manifold.sql.api.Column;
import manifold.sql.rt.api.*;
import manifold.sql.rt.api.OperableTxScope;
import manifold.sql.rt.impl.DefaultTxScopeProvider;
import manifold.sql.schema.api.*;
import manifold.util.concurrent.LocklessLazyVar;

import javax.tools.DiagnosticListener;
import javax.tools.JavaFileManager;
import javax.tools.JavaFileObject;
import java.lang.reflect.Modifier;
import java.sql.SQLException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

import static manifold.api.gen.AbstractSrcClass.Kind.Class;
import static manifold.api.gen.AbstractSrcClass.Kind.Interface;
import static manifold.api.gen.SrcLinkedClass.addActualNameAnnotation;
import static manifold.rt.api.util.ManIdentifierUtil.makeIdentifier;
import static manifold.rt.api.util.ManIdentifierUtil.makePascalCaseIdentifier;

/**
 * The top-level class enclosing all the DDL types corresponding with a ".dbconfig" file.
 */
class SchemaParentType
{
  private final SchemaModel _model;

  SchemaParentType( SchemaModel model )
  {
    _model = model;
  }

  private String getFqn()
  {
    return _model.getFqn();
  }

  @SuppressWarnings( "unused" )
  boolean hasChild( String childName )
  {
    return getSchema() != null && getSchema().hasTable( childName );
  }

  private Schema getSchema()
  {
    return _model.getSchema();
  }

  SchemaTable getChild( String childName )
  {
    return getSchema() == null ? null : getSchema().getTable( childName );
  }

  void render( StringBuilder sb, JavaFileManager.Location location, IModule module, DiagnosticListener<JavaFileObject> errorHandler )
  {
    SrcLinkedClass srcClass = new SrcLinkedClass( getFqn(), Class, _model.getFile(), location, module, errorHandler )
      .addAnnotation( new SrcAnnotationExpression( DisableStringLiteralTemplates.class.getSimpleName() ) )
      .modifiers( Modifier.PUBLIC )
      .addInterface( new SrcType( SchemaType.class.getSimpleName() ) );
    addImports( srcClass );
    addDefaultScopeMethod( srcClass );
    addCommitMethod( srcClass );
    addNewScopeMethod( srcClass );
    addInnerTypes( srcClass );
    addFkColAssignMethod( srcClass );
    srcClass.render( sb, 0 );
  }

  private void addDefaultScopeMethod( SrcLinkedClass srcClass )
  {
    SrcMethod method = new SrcMethod( srcClass )
      .modifiers( Modifier.PRIVATE | Modifier.STATIC )
      .name( "defaultScope" )
      .returns( new SrcType( TxScope.class.getSimpleName() ) );
    method.body( "return DefaultTxScopeProvider.instance().defaultScope(${srcClass.getName()}.class);" );
    srcClass.addMethod( method );
  }

  private void addCommitMethod( SrcLinkedClass srcClass )
  {
    //noinspection unchecked
    SrcMethod method = new SrcMethod( srcClass )
      .modifiers( Modifier.PUBLIC | Modifier.STATIC )
      .name( "commit" )
      .throwsList( new SrcType( SQLException.class.getSimpleName() ) )
      .body( "defaultScope().commit();" );
    srcClass.addMethod( method );
  }

  private void addNewScopeMethod( SrcLinkedClass srcClass )
  {
    SrcMethod method = new SrcMethod( srcClass )
      .modifiers( Modifier.PUBLIC | Modifier.STATIC )
      .name( "newScope" )
      .returns( new SrcType( TxScope.class.getSimpleName() ) );
    method.body( "return ${Dependencies.class.getName()}.instance().getTxScopeProvider().newScope(${srcClass.getName()}.class);" );
    srcClass.addMethod( method );
  }

  private void addInnerTypes( SrcLinkedClass srcClass )
  {
    if( getSchema() == null )
    {
      return;
    }

    for( SchemaTable type: getSchema().getTables().values() )
    {
      addInnerObjectType( type, srcClass );
    }
  }

  private void addInnerObjectType( SchemaTable table, SrcLinkedClass enclosingType )
  {
    String identifier = getSchema().getJavaTypeName( table.getName() );
    String fqn = getFqn() + '.' + identifier;
    SrcLinkedClass srcClass = new SrcLinkedClass( fqn, enclosingType, Interface )
      .addInterface( TableRow.class.getSimpleName() )
      .modifiers( Modifier.PUBLIC );
    addActualNameAnnotation( srcClass, table.getName(), false );
    addCreateMethods( srcClass, table );
    addReadMethods( srcClass, table );
    addDeleteMethod( srcClass );
    addBuilderType( srcClass, table );
    addBuilderMethod( srcClass, table );
    addTableInfoMethod( srcClass, table );

    addProperties( table, srcClass );

    enclosingType.addInnerClass( srcClass );
  }

  private void addProperties( SchemaTable table, SrcLinkedClass srcClass )
  {
    for( Map.Entry<SchemaTable, List<SchemaForeignKey>> entry : table.getForeignKeys().entrySet() )
    {
      List<SchemaForeignKey> fk = entry.getValue();
      for( SchemaForeignKey sfk : fk )
      {
        addFkProperty( srcClass, sfk );
      }
    }
    for( SchemaColumn col: table.getColumns().values() )
    {
      if( col.getForeignKey() == null )
      {
        addProperty( srcClass, col );
      }
    }
  }

  private void addTableInfoMethod( SrcLinkedClass srcClass, SchemaTable table )
  {
    SrcField tableInfoField = new SrcField( "myTableInfo", new SrcType( LocklessLazyVar.class.getSimpleName() ).addTypeParam( TableInfo.class ) );
    StringBuilder sb = new StringBuilder( "LocklessLazyVar.make(() -> {\n" );
    sb.append( "      LinkedHashMap<String, Integer> allCols = new LinkedHashMap<>();\n" );
    for( Map.Entry<String, SchemaColumn> entry : table.getColumns().entrySet() )
    {
      //noinspection unused
      String colName = entry.getKey();
      //noinspection unused
      int jdbcType = entry.getValue().getJdbcType();
      sb.append( "      allCols.put(\"$colName\", $jdbcType);\n");
    }
    sb.append( "      HashSet<String> pkCols = new HashSet<>();\n" );
    for( SchemaColumn pkCol : table.getPrimaryKey() )
    {
      //noinspection unused
      String pkColName = pkCol.getName();
      sb.append( "      pkCols.add(\"$pkColName\");\n\n" );
    }
    sb.append( "      HashSet<String> ukCols = new HashSet<>();\n" );
    for( Map.Entry<String, List<SchemaColumn>> entry : table.getNonNullUniqueKeys().entrySet() )
    {
      // just need one
      for( SchemaColumn ukCol : entry.getValue() )
      {
        //noinspection unused
        String ukColName = ukCol.getName();
        sb.append( "      ukCols.add(\"$ukColName\");\n\n" );
      }
      break;
    }
    //noinspection unused
    String ddlTableName = table.getName();
    sb.append( "      return new TableInfo(\"$ddlTableName\", pkCols, ukCols, allCols);\n" );
    sb.append( "    });\n" );
    tableInfoField.initializer( sb.toString() );
    srcClass.addField( tableInfoField );

    SrcMethod tableInfoMethod = new SrcMethod( srcClass )
      .modifiers( Flags.DEFAULT )
      .name( "tableInfo" )
      .returns( TableInfo.class );
    tableInfoMethod.body( "return myTableInfo.get();" );
    srcClass.addMethod( tableInfoMethod );
  }

  private void addBuilderMethod( SrcLinkedClass srcClass, SchemaTable table )
  {
    SrcMethod method = new SrcMethod( srcClass )
      .modifiers( Modifier.STATIC )
      .name( "builder" )
      .returns( new SrcType( "Builder" ) );
    addRequiredParameters( srcClass, table, method );
    srcClass.addMethod( method );

    StringBuilder sb = new StringBuilder();
    sb.append( "return new Builder() {\n" );
    sb.append( "        Bindings _bindings = new DataBindings(new ConcurrentHashMap<>());\n" );
    sb.append( "        {\n" );
    initFromParameters( table, sb, "_bindings" );
    sb.append( "        }\n" );
    
    sb.append( "        @Override public Bindings getBindings() { return _bindings; }\n" );
    sb.append( "      };" );
    method.body( sb.toString() );
  }

  private void addCreateMethods( SrcLinkedClass srcClass, SchemaTable table )
  {
    String tableName = getTableFqn( table );
    SrcMethod method = new SrcMethod( srcClass )
      .modifiers( Modifier.STATIC )
      .name( "create" )
      .returns( new SrcType( tableName ) );
    addRequiredParameters( srcClass, table, method );
    StringBuilder sb = new StringBuilder();
    sb.append( "return create(defaultScope()" );
    sb.append( method.getParameters().isEmpty() ? "" : ", " );
    method.forwardParameters( sb );
    sb.append( ");" );
    method.body( sb.toString() );
    srcClass.addMethod( method );


    method = new SrcMethod( srcClass )
      .modifiers( Modifier.STATIC )
      .name( "create" )
      .returns( new SrcType( tableName ) )
      .addParam( "txScope", new SrcType( TxScope.class.getSimpleName() ) );
    addRequiredParameters( srcClass, table, method );
    srcClass.addMethod( method );

    sb = new StringBuilder();
    sb.append( "DataBindings args = new DataBindings(new ConcurrentHashMap<>());\n" );
    initFromParameters( table, sb, "args" );
    sb.append( "      TxBindings bindings = new BasicTxBindings(txScope, TxKind.Insert, args);\n");
    sb.append( "      $tableName tableRow = new $tableName() { @Override public TxBindings getBindings() { return bindings; } };\n" );
    sb.append( "      tableRow.getBindings().setOwner(tableRow);\n" );
    sb.append( "      ((OperableTxScope)txScope).addRow(tableRow);\n" );
    sb.append( "      return tableRow;" );
    method.body( sb.toString() );
  }

  private void initFromParameters( SchemaTable table, StringBuilder sb, @SuppressWarnings( "unused" ) String bindingsVar )
  {
    Set<SchemaColumn> fkCovered = new HashSet<>();
    for( Map.Entry<SchemaTable, List<SchemaForeignKey>> entry : table.getForeignKeys().entrySet() )
    {
      List<SchemaForeignKey> fk = entry.getValue();
      for( SchemaForeignKey sfk : fk )
      {
        List<SchemaColumn> fkCols = sfk.getColumns();
        if( fkCols.stream().anyMatch( c -> isRequired( c ) ) )
        {
          //noinspection unused
          String fkParamName = makePascalCaseIdentifier( sfk.getName(), false );
          for( SchemaColumn fkCol : fkCols )
          {
            //noinspection unused
            String colName = fkCol.getName();
            //noinspection unused
            String keyColName = fkCol.getForeignKey().getName();
            sb.append( "assignFkBindingValues($fkParamName, \"$fkParamName\", \"$keyColName\", \"$colName\", $bindingsVar);" );
          }
          fkCovered.addAll( fkCols );
        }
      }
    }
    for( SchemaColumn col: table.getColumns().values() )
    {
      if( isRequired( col ) && !fkCovered.contains( col ) )
      {
        //noinspection unused
        String colName = col.getName();
        //noinspection unused
        String paramName = makePascalCaseIdentifier( col.getName(), false );
        sb.append( "$bindingsVar.put(\"$colName\", $paramName);\n" );
      }
    }
  }

  private void addRequiredParameters( SrcLinkedClass owner, SchemaTable table, AbstractSrcMethod method )
  {
    Set<SchemaColumn> fkCovered = new HashSet<>();
    for( Map.Entry<SchemaTable, List<SchemaForeignKey>> entry : table.getForeignKeys().entrySet() )
    {
      List<SchemaForeignKey> fk = entry.getValue();
      for( SchemaForeignKey sfk : fk )
      {
        List<SchemaColumn> fkCols = sfk.getColumns();
        if( fkCols.stream().anyMatch( c -> isRequired( c ) ) )
        {
          fkCovered.addAll( fkCols );
          String tableFqn = getTableFqn( sfk.getReferencedTable() );
          SrcType srcType = new SrcType( tableFqn );
          method.addParam( makePascalCaseIdentifier( sfk.getName(), false ), srcType );
        }
      }
    }

    for( SchemaColumn col: table.getColumns().values() )
    {
      if( isRequired( col ) && !fkCovered.contains( col ) )
      {
        SrcType srcType = makeSrcType( owner, col.getType(), false, true );
        method.addParam( makePascalCaseIdentifier( col.getName(), false ), srcType );
      }
    }
  }

  private void addBuilderType( SrcLinkedClass enclosingType, SchemaTable table )
  {
    String fqn = enclosingType.getName() + ".Builder";
    SrcLinkedClass srcInterface = new SrcLinkedClass( fqn, enclosingType, Interface )
      .addInterface( new SrcType( SchemaBuilder.class.getSimpleName() ).addTypeParam( getTableFqn( table ) ) );
    enclosingType.addInnerClass( srcInterface );
    addWithMethods( srcInterface, table );
    addBuildMethods( srcInterface, table );
  }

  private void addBuildMethods( SrcLinkedClass srcInterface, SchemaTable table )
  {
    String tableName = getTableFqn( table );
    SrcMethod method = new SrcMethod( srcInterface )
      .modifiers( Flags.DEFAULT )
      .name( "build" )
      .returns( new SrcType( tableName ) )
      .body( "return build(defaultScope());" );
    srcInterface.addMethod( method );

    method = new SrcMethod( srcInterface )
      .modifiers( Flags.DEFAULT )
      .name( "build" )
      .addParam( "txScope", new SrcType( TxScope.class.getSimpleName() ) )
      .returns( new SrcType( tableName ) );
    srcInterface.addMethod( method );
    method.body(
        "BasicTxBindings bindings = new BasicTxBindings(txScope, TxKind.Insert, Builder.this.getBindings());\n" +
        "$tableName tableRow = new $tableName() { @Override public TxBindings getBindings() { return bindings; } };\n" +
        "tableRow.getBindings().setOwner(tableRow);\n" +
        "((OperableTxScope)txScope).addRow(tableRow);\n" +
        "return tableRow;" );
  }

  private void addWithMethods( SrcLinkedClass srcClass, SchemaTable table )
  {
    for( SchemaColumn col: table.getColumns().values() )
    {
      if( isRequired( col ) )
      {
        continue;
      }

      addWithMethod( srcClass, col );
    }
  }

  private void addWithMethod( SrcLinkedClass srcClass, SchemaColumn col )
  {
    //noinspection unused
    String actualName = col.getName();
    SrcType type = makeSrcType( srcClass, col.getType(), false, true );
    //noinspection unused
    String propName = makePascalCaseIdentifier( actualName, true );
    SrcMethod withMethod = new SrcMethod()
      .modifiers( Flags.DEFAULT )
      .name( "with$propName" )
      .addParam( "${'$'}value", type )
      .returns( new SrcType( srcClass.getSimpleName() ) );
    addActualNameAnnotation( withMethod, actualName, true );
    withMethod.body( "getBindings().put(\"$actualName\", ${'$'}value); return this;" );
    srcClass.addMethod( withMethod );
  }

  private boolean isRequired( SchemaColumn col )
  {
    return !col.isNullable() &&
      !col.isGenerated() &&
      !col.isAutoIncrement() &&
      col.getDefaultValue() == null;
  }

  private void addImports( SrcLinkedClass srcClass )
  {
    srcClass.addImport( Bindings.class );
    srcClass.addImport( TxBindings.class );
    srcClass.addImport( TxScope.class );
    srcClass.addImport( OperableTxScope.class );
    srcClass.addImport( BasicTxBindings.class );
    srcClass.addImport( BasicTxBindings.TxKind.class );
    srcClass.addImport( DataBindings.class );
    srcClass.addImport( TableRow.class );
    srcClass.addImport( TableInfo.class );
    srcClass.addImport( SchemaType.class );
    srcClass.addImport( SchemaBuilder.class );
    srcClass.addImport( QueryContext.class );
    srcClass.addImport( CrudProvider.class );
    srcClass.addImport( TxScopeProvider.class );
    srcClass.addImport( DefaultTxScopeProvider.class );
    srcClass.addImport( ConcurrentHashMap.class );
    srcClass.addImport( LinkedHashMap.class );
    srcClass.addImport( KeyRef.class );
    srcClass.addImport( Map.class );
    srcClass.addImport( Set.class );
    srcClass.addImport( HashSet.class );
    srcClass.addImport( LocklessLazyVar.class );
    srcClass.addImport( SQLException.class );
    srcClass.addImport( ActualName.class );
    srcClass.addImport( DisableStringLiteralTemplates.class );
  }

  private void addFkProperty( SrcLinkedClass srcClass, SchemaForeignKey sfk )
  {
    SchemaTable table = sfk.getReferencedTable();
    String tableFqn = getTableFqn( table );

    SrcType type = new SrcType( tableFqn );
    String name = sfk.getName();
    String propName = makePascalCaseIdentifier( name, true );
    SrcMethod fkFetchMethod = new SrcMethod( srcClass )
      .name( "get" + propName )
      .modifiers( Flags.DEFAULT )
      .returns( type );
    StringBuilder sb = new StringBuilder();
    sb.append( "DataBindings paramBindings = new DataBindings(new ConcurrentHashMap<>());\n" );
    for( SchemaColumn col : sfk.getColumns() )
    {
      //noinspection unused
      Column referencedCol = col.getForeignKey();
      sb.append( "    paramBindings.put(\"${referencedCol.getName()}\", getBindings().get(\"${col.getName()}\"));\n" );
    }

    //noinspection unused
    String jdbcParamTypes = getJdbcParamTypes( sfk.getColumns() );
    //noinspection unused
    String configName = _model.getDbConfig().getName();
    sb.append( "    return ${Dependencies.class.getName()}.instance().getCrudProvider().read(" +
      "new QueryContext<$tableFqn>(getBindings().getTxScope(), $tableFqn.class, \"${table.getName()}\", $jdbcParamTypes, paramBindings, \"$configName\", " +
      "rowBindings -> new $tableFqn() {public TxBindings getBindings() { return rowBindings; }}));" );
    fkFetchMethod.body( sb.toString() );
    addActualNameAnnotation( fkFetchMethod, name, true );
    srcClass.addMethod( fkFetchMethod );

    SrcMethod fkSetter = new SrcMethod( srcClass )
      .modifiers( Flags.DEFAULT )
      .name( "set" + propName )
      .addParam( "ref", new SrcType( tableFqn ) );
    for( SchemaColumn fkCol : sfk.getColumns() )
    {
      //noinspection unused
      String colName = fkCol.getName();
      //noinspection unused
      String keyColName = fkCol.getForeignKey().getName();
      fkSetter.body( "assignFkBindingValues(ref, \"$propName\", \"$keyColName\", \"$colName\", getBindings());" );
    }
    addActualNameAnnotation( fkSetter, name, true );
    srcClass.addMethod( fkSetter );
  }

  private void addFkColAssignMethod( SrcLinkedClass srcClass )
  {
    SrcMethod method = new SrcMethod( srcClass )
      .modifiers( Modifier.PRIVATE | Modifier.STATIC )
      .name( "assignFkBindingValues" )
      .addParam( "ref", new SrcType( TableRow.class ) )
      .addParam( "propName", new SrcType( String.class ) )
      .addParam( "keyColName", new SrcType( String.class ) )
      .addParam( "colName", new SrcType( String.class ) )
      .addParam( "bindings", new SrcType( Bindings.class ) );
      method.body( "if(ref == null) throw new NullPointerException(\"Expecting non-null value for: \" + propName );\n" +
        "Object keyColValue = ref.getBindings().get(keyColName);\n" +
        "bindings.put(colName, keyColValue != null ? keyColValue : new KeyRef(ref, keyColName));" );
    srcClass.addMethod( method );
  }

  private void addProperty( SrcLinkedClass srcInterface, SchemaColumn col )
  {
    Class<?> type = col.getType();
    String name = col.getName();

    SrcType propType = makeSrcType( srcInterface, type, false );
//    SrcType setterType = makeSrcType( srcInterface, type, false, true );
    String propName = makePascalCaseIdentifier( name, true );
    //noinspection unused
    String colName = makeIdentifier( name );

    SrcGetProperty getter = new SrcGetProperty( propName, propType );
    getter.modifiers( Flags.DEFAULT );
    StringBuilder retType = new StringBuilder();
    propType.render( retType, 0, false ); // calling render to include array "[]"
    getter.body( "return ($retType)getBindings().get(\"$colName\");" );
    addActualNameAnnotation( getter, name, true );
    srcInterface.addGetProperty( getter );

    if( !col.isGenerated() && !col.isAutoIncrement() )
    {
      SrcSetProperty setter = new SrcSetProperty( propName, propType )
        .modifiers( Flags.DEFAULT );
      setter.body( "getBindings().put(\"$colName\", ${'$'}value);" );
      addActualNameAnnotation( setter, name, true );
      srcInterface.addSetProperty( setter );
    }
  }

  // Foo foo = Foo.create(txScope, ...);
  // Foo foo = Foo.read(txScope, ...);
  // foo.delete();
  //...
  // txScope.commit();

  private void addReadMethods( SrcLinkedClass srcClass, SchemaTable table )
  {
    String tableFqn = getTableFqn( table );
    SrcMethod method = new SrcMethod( srcClass )
      .modifiers( Modifier.STATIC )
      .name( "read" )
      .returns( new SrcType( tableFqn ) );
    List<SchemaColumn> whereCols = addSelectParameters( srcClass, table, method );
    if( whereCols.isEmpty() )
    {
      // no pk and no pk, no read method, instead use type-safe sql query :)
      return;
    }
    StringBuilder sb = new StringBuilder();
    sb.append( "return read(defaultScope()" );
    sb.append( method.getParameters().isEmpty() ? "" : ", " );
    method.forwardParameters( sb );
    sb.append( ");" );
    method.body( sb.toString() );
    srcClass.addMethod( method );


    method = new SrcMethod( srcClass )
      .modifiers( Modifier.STATIC )
      .name( "read" )
      .returns( new SrcType( tableFqn ) )
      .addParam( "txScope", new SrcType( TxScope.class.getSimpleName() ) );
    whereCols = addSelectParameters( srcClass, table, method );
    //noinspection unused
    String jdbcParamTypes = getJdbcParamTypes( whereCols );
    //noinspection unused
    String configName = _model.getDbConfig().getName();
    sb = new StringBuilder();
    sb.append( "DataBindings paramBindings = new DataBindings(new ConcurrentHashMap<>());\n" );
    for( SchemaColumn col : whereCols )
    {
      //noinspection unused
      String paramName = makePascalCaseIdentifier( col.getName(), false );
      sb.append( "    paramBindings.put(\"${col.getName()}\", $paramName);\n" );
    }
    sb.append( "    return ${Dependencies.class.getName()}.instance().getCrudProvider().read(new QueryContext<$tableFqn>(txScope, $tableFqn.class,\n" +
      "\"${table.getName()}\", $jdbcParamTypes, paramBindings, \"$configName\",\n" +
      "rowBindings -> new $tableFqn() {public TxBindings getBindings() { return rowBindings; }}));" );
    method.body( sb.toString() );
    srcClass.addMethod( method );
  }

  private List<SchemaColumn> addSelectParameters( SrcLinkedClass owner, SchemaTable table, AbstractSrcMethod method )
  {
    List<SchemaColumn> pk = table.getPrimaryKey();
    if( !pk.isEmpty() )
    {
      for( SchemaColumn col : pk )
      {
        SrcType srcType = makeSrcType( owner, col.getType(), false, true );
        method.addParam( makePascalCaseIdentifier( col.getName(), false ), srcType );
      }
      return pk;
    }
    else
    {
      for( Map.Entry<String, List<SchemaColumn>> entry : table.getNonNullUniqueKeys().entrySet() )
      {
        for( SchemaColumn col : entry.getValue() )
        {
          SrcType srcType = makeSrcType( owner, col.getType(), false, true );
          method.addParam( makePascalCaseIdentifier( col.getName(), false ), srcType );
        }
        return entry.getValue();
      }
    }
    return Collections.emptyList();
  }

  private void addDeleteMethod( SrcLinkedClass srcClass )
  {
    SrcMethod method = new SrcMethod( srcClass )
      .modifiers( Flags.DEFAULT )
      .name( "delete" )
      .addParam( "delete", boolean.class );
    method.body( "getBindings().setDelete(delete);" );
    srcClass.addMethod( method );
  }

  private void addOneToManyFetcher( SrcLinkedClass srcClass, SchemaForeignKey fkToThis )
  {
    //todo: add fetch<fk-to-this>List() method
    // Note, we have to reconcile changes to the the fk fields from referring table rows, any
  }

  private String getJdbcParamTypes( List<SchemaColumn> parameters )
  {
    StringBuilder sb = new StringBuilder( "new int[]{");
    for( int i = 0; i < parameters.size(); i++ )
    {
      Column p = parameters.get( i );
      if( i > 0 )
      {
        sb.append( "," );
      }
      sb.append( p.getJdbcType() );
    }
    return sb.append( "}" ).toString();
  }

  // qualifying name with outer class name (config name) to prevent collisions with other class names that could be imported
  private String getTableFqn( SchemaTable table )
  {
//    String schemaPackage = _model.getDbConfig().getSchemaPackage();
    String configName = table.getSchema().getDbConfig().getName();
//    return schemaPackage + "." + configName + "." + getTableSimpleTypeName( table );
    return configName + "." + getTableSimpleTypeName( table );
  }

  private String getTableSimpleTypeName( SchemaTable table )
  {
    return table.getSchema().getJavaTypeName( table.getName() );
  }

  private SrcType makeSrcType( SrcLinkedClass owner, Class<?> type, boolean typeParam )
  {
    return makeSrcType( owner, type, typeParam, false );
  }
  private SrcType makeSrcType( SrcLinkedClass owner, Class<?> type, boolean typeParam, boolean isParameter )
  {
    String typeName = getJavaName( type );
    SrcType srcType = new SrcType( typeName );
    if( !typeParam )
    {
      srcType.setPrimitive( type.isPrimitive() );
    }
    return srcType;
  }

  private String getJavaName( Class<?> cls )
  {
    if( cls == String.class )
    {
      return String.class.getSimpleName();
    }
    return cls.getTypeName();
  }
}
