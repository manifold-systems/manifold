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
import manifold.sql.schema.api.*;
import manifold.util.concurrent.LocklessLazyVar;

import javax.tools.DiagnosticListener;
import javax.tools.JavaFileManager;
import javax.tools.JavaFileObject;
import java.lang.reflect.Modifier;
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
      .addInterface( SchemaType.class );
    addImports( srcClass );
    addInnerTypes( srcClass );
    srcClass.render( sb, 0 );
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
    addCreateMethod( srcClass, table );
    addReadMethod( srcClass, table );
    addDeleteMethod( srcClass, table );
    addBuilderType( srcClass, table );
    addBuilderMethod( srcClass, table );
    addTableInfoMethod( srcClass, table );

    for( SchemaColumn member: table.getColumns().values() )
    {
      addProperty( srcClass, member );
    }

    for( List<SchemaForeignKey> fkEntry : table.getForeignKeys().values() )
    {
      for( SchemaForeignKey fk : fkEntry )
      {
        addFkFetcher( srcClass, fk );
      }
    }

    enclosingType.addInnerClass( srcClass );
  }

  private void addTableInfoMethod( SrcLinkedClass srcClass, SchemaTable table )
  {
    SrcField tableInfoField = new SrcField( "myTableInfo", new SrcType( LocklessLazyVar.class ).addTypeParam( TableInfo.class ) );
    StringBuilder sb = new StringBuilder( "LocklessLazyVar.make(() -> {\n" );
    sb.append( "      Map<String, Integer> allCols = new LinkedHashMap<>();\n" );
    for( Map.Entry<String, SchemaColumn> entry : table.getColumns().entrySet() )
    {
      //noinspection unused
      String colName = entry.getKey();
      //noinspection unused
      int jdbcType = entry.getValue().getJdbcType();
      sb.append( "      allCols.put(\"$colName\", $jdbcType);\n");
    }
    sb.append( "      Set<String> pkCols = new HashSet<>();\n" );
    for( SchemaColumn pkCol : table.getPrimaryKey() )
    {
      //noinspection unused
      String pkColName = pkCol.getName();
      sb.append( "      pkCols.add(\"$pkColName\");\n\n" );
    }
    sb.append( "      Set<String> ukCols = new HashSet<>();\n" );
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
      .addParam( "txScope", TxScope.class )
      .returns( new SrcType( "Builder" ) );
    addRequiredParameters( srcClass, table, method );
    srcClass.addMethod( method );

    StringBuilder sb = new StringBuilder();
    sb.append( "return new Builder() {\n" );
    sb.append( "        Bindings _bindings = new DataBindings(new ConcurrentHashMap<>());\n" );
    sb.append( "        {\n" );
    int i = 0;
    for( SchemaColumn col: table.getColumns().values() )
    {
      if( isRequired( col ) )
      {
        //noinspection unused
        String colName = col.getName();
        SrcParameter param = method.getParameters().get( i++ );
        //noinspection unused
        String paramName = param.getSimpleName();
        sb.append( "          _bindings.put(\"$colName\", $paramName);\n" );
      }
    }
    sb.append( "          _bindings = new BasicTxBindings(txScope, TxKind.Insert, _bindings);\n" );
    sb.append( "        }\n" );

    sb.append( "        @Override public TxBindings getBindings() { return (TxBindings)_bindings; }\n" );

    sb.append( "      };" );
    method.body( sb.toString() );
  }

  private void addCreateMethod( SrcLinkedClass srcClass, SchemaTable table )
  {
    String tableName = getTableFqn( table );
    SrcMethod method = new SrcMethod( srcClass )
      .modifiers( Modifier.STATIC )
      .name( "create" )
      .returns( new SrcType( tableName ) )
      .addParam( "txScope", TxScope.class );
    addRequiredParameters( srcClass, table, method );
    srcClass.addMethod( method );

    StringBuilder sb = new StringBuilder();
    sb.append( "DataBindings args = new DataBindings(new ConcurrentHashMap<>());\n" );
    int i = 0;
    for( SchemaColumn col: table.getColumns().values() )
    {
      if( isRequired( col ) )
      {
        //noinspection unused
        String colName = col.getName();
        SrcParameter param = method.getParameters().get( i++ );
        //noinspection unused
        String paramName = param.getSimpleName();
        sb.append( "      args.put(\"$colName\", $paramName);\n" );
      }
    }
    sb.append( "      TxBindings bindings = new BasicTxBindings(txScope, TxKind.Insert, args);\n");
    sb.append( "      $tableName tableRow = new $tableName() { @Override public TxBindings getBindings() { return bindings; } };\n" );
    sb.append( "      tableRow.getBindings().setOwner(tableRow);\n" );
    sb.append( "      ((OperableTxScope)txScope).addRow(tableRow);\n" );
    sb.append( "      return tableRow;" );
    method.body( sb.toString() );
  }

  private void addRequiredParameters( SrcLinkedClass owner, SchemaTable table, AbstractSrcMethod method )
  {
    for( SchemaColumn col: table.getColumns().values() )
    {
      if( isRequired( col ) )
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
    addBuildMethod( enclosingType, srcInterface, table );
  }

  private void addBuildMethod( SrcLinkedClass enclosingType, SrcLinkedClass srcInterface, SchemaTable table )
  {
    String tableName = getTableFqn( table );
    SrcMethod method = new SrcMethod( srcInterface )
      .modifiers( Flags.DEFAULT )
      .name( "build" )
      .returns( new SrcType( tableName ) );
    srcInterface.addMethod( method );

    method.body(
      "$tableName tableRow = new $tableName() { @Override public TxBindings getBindings() { return Builder.this.getBindings(); } };\n" +
        "    tableRow.getBindings().setOwner(tableRow);\n" +
        "    ((OperableTxScope)tableRow.getBindings().getTxScope()).addRow(tableRow);\n" +
        "    return tableRow;" );
  }

  private void addWithMethods( SrcLinkedClass srcClass, SchemaTable table )
  {
    for( SchemaColumn col: table.getColumns().values() )
    {
      if( isRequired( col ) )
      {
        continue;
      }

      addWithMethod( srcClass, col, table );
    }
  }

  private void addWithMethod( SrcLinkedClass srcClass, SchemaColumn col, SchemaTable table )
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
    srcClass.addImport( OperableTxScope.class );
    srcClass.addImport( BasicTxBindings.class );
    srcClass.addImport( BasicTxBindings.TxKind.class );
    srcClass.addImport( DataBindings.class );
    srcClass.addImport( TableRow.class );
    srcClass.addImport( TableInfo.class );
    srcClass.addImport( SchemaBuilder.class );
    srcClass.addImport( QueryContext.class );
    srcClass.addImport( CrudProvider.class );
    srcClass.addImport( ConcurrentHashMap.class );
    srcClass.addImport( LinkedHashMap.class );
    srcClass.addImport( Map.class );
    srcClass.addImport( Set.class );
    srcClass.addImport( HashSet.class );
    srcClass.addImport( LocklessLazyVar.class );
    srcClass.addImport( ActualName.class );
    srcClass.addImport( DisableStringLiteralTemplates.class );
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
    srcInterface.addGetProperty( getter ).modifiers( Modifier.PUBLIC );

    if( !col.isGenerated() && !col.isAutoIncrement() )
    {
      SrcSetProperty setter = new SrcSetProperty( propName, propType );
      setter.modifiers( Flags.DEFAULT );
      setter.body( "getBindings().put(\"$colName\", ${'$'}value);" );
      addActualNameAnnotation( setter, name, true );
      srcInterface.addSetProperty( setter ).modifiers( Modifier.PUBLIC );

      SchemaColumn pkCol = col.getForeignKey();
      if( pkCol != null && pkCol.isNonNullUniqueId() )
      {
        // add setXxx(Xxx) to set a foreign key id from a table instance
        // if the table instance's pk is null, the table instance is not yet inserted, as a consequence the fk id value
        // is temporarily set to the table instance where the TxScope will make that work by inserting the table instance
        // first

        String tableFqn = getTableFqn( pkCol.getTable() );
        SrcSetProperty fkSetter = new SrcSetProperty( propName, new SrcType( tableFqn ) );
        getter.modifiers( Flags.DEFAULT );
        //noinspection unused
        String pkPropName = makePascalCaseIdentifier( pkCol.getName(), true );
        getter.body( "getBindings().put(\"$colName\", ${'$'}value.get$pkPropName() != null ? ${'$'}value.get$pkPropName() : ${'$'}value);" );
        addActualNameAnnotation( setter, name, true );
        srcInterface.addSetProperty( fkSetter ).modifiers( Modifier.PUBLIC );
      }
    }
  }

  // Foo foo = Foo.create(txScope, ...);
  // Foo foo = Foo.read(txScope, ...);
  // foo.delete();
  //...
  // txScope.commit();

  private void addReadMethod( SrcLinkedClass srcClass, SchemaTable table )
  {
    String tableFqn = getTableFqn( table );
    SrcMethod method = new SrcMethod( srcClass )
      .modifiers( Modifier.STATIC )
      .name( "read" )
      .returns( new SrcType( tableFqn ) )
      .addParam( "txScope", TxScope.class );
    List<SchemaColumn> whereCols = addSelectParameters( srcClass, table, method );
    if( whereCols.isEmpty() )
    {
      // no pk and no pk, no read method, instead use type-safe sql query :)
      return;
    }

    //noinspection unused
    String jdbcParamTypes = getJdbcParamTypes( whereCols );
    //noinspection unused
    String configName = _model.getDbConfig().getName();
    StringBuilder sb = new StringBuilder();
    sb.append( "DataBindings paramBindings = new DataBindings(new ConcurrentHashMap<>());\n" );
    for( SchemaColumn col : whereCols )
    {
      //noinspection unused
      String paramName = makePascalCaseIdentifier( col.getName(), false );
      sb.append( "    paramBindings.put(\"${col.getName()}\", $paramName);\n" );
    }
    sb.append( "    return CrudProvider.instance().read(new QueryContext<$tableFqn>(txScope, $tableFqn.class,\n" +
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
      int i = 0;
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

  private void addDeleteMethod( SrcLinkedClass srcClass, SchemaTable table )
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

  private void addFkSetter( SrcLinkedClass srcClass, SchemaForeignKey fk )
  {
    //todo: add setter for foreign keys
    //todo: if the fk is auto-generated/incremented and the set value is newly created, set the fk column value to the reference value
    // then the TxScope can handle the setting of the actual id by inserting the fk reference first and then getting the id and assigning it as an on-hold value
    // Also, must handle the case where the value is created, assigned to this fk, then deleted.
  }
  private void addFkFetcher( SrcLinkedClass srcClass, SchemaForeignKey fk )
  {
    //todo: if the fk is auto-generated/incremented and is set and is a newly created value, it will be assigned directly
    // to the fk-id field, just return that here

    SchemaTable table = fk.getReferencedTable();
    String tableFqn = getTableFqn( table );

    SrcType type = new SrcType( tableFqn );
    String name = fk.getName();
    String propName = makePascalCaseIdentifier( name, true );
    SrcMethod fkFetchMethod = new SrcMethod( srcClass )
      .name( "fetch" + propName )
      .modifiers( Flags.DEFAULT )
      .returns( type );
    StringBuilder sb = new StringBuilder();
    sb.append( "DataBindings paramBindings = new DataBindings(new ConcurrentHashMap<>());\n" );
    for( SchemaColumn col : fk.getColumns() )
    {
      //noinspection unused
      Column referencedCol = col.getForeignKey();
      sb.append( "    paramBindings.put(\"${referencedCol.getName()}\", getBindings().get(\"${col.getName()}\"));\n" );
    }

    //noinspection unused
    String jdbcParamTypes = getJdbcParamTypes( fk.getColumns() );
    //noinspection unused
    String configName = _model.getDbConfig().getName();
    sb.append( "    return CrudProvider.instance().read(" +
      "new QueryContext<$tableFqn>(getBindings().getTxScope(), $tableFqn.class, \"${table.getName()}\", $jdbcParamTypes, paramBindings, \"$configName\", " +
      "rowBindings -> new $tableFqn() {public TxBindings getBindings() { return rowBindings; }}));" );
    fkFetchMethod.body( sb.toString() );
    addActualNameAnnotation( fkFetchMethod, name, true );
    srcClass.addMethod( fkFetchMethod );
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
