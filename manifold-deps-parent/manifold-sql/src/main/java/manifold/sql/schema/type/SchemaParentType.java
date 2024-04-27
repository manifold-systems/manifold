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
import manifold.api.fs.IFile;
import manifold.api.gen.*;
import manifold.api.host.IModule;
import manifold.api.util.cache.FqnCache;
import manifold.json.rt.api.*;
import manifold.rt.api.*;
import manifold.rt.api.util.ManClassUtil;
import manifold.rt.api.util.ManEscapeUtil;
import manifold.rt.api.util.Pair;
import manifold.rt.api.util.StreamUtil;
import manifold.sql.api.Column;
import manifold.sql.rt.api.*;
import manifold.sql.rt.api.OperableTxScope;
import manifold.sql.rt.util.DriverInfo;
import manifold.sql.schema.api.*;
import manifold.sql.schema.jdbc.JdbcSchemaForeignKey;
import manifold.sql.util.CombinationUtil;
import manifold.util.concurrent.LocklessLazyVar;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.tools.DiagnosticListener;
import javax.tools.JavaFileManager;
import javax.tools.JavaFileObject;
import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.reflect.Modifier;
import java.sql.SQLException;
import java.sql.Types;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static java.nio.charset.StandardCharsets.UTF_8;
import static manifold.api.gen.AbstractSrcClass.Kind.Class;
import static manifold.api.gen.AbstractSrcClass.Kind.Interface;
import static manifold.api.gen.SrcLinkedClass.addActualNameAnnotation;
import static manifold.rt.api.util.ManIdentifierUtil.makeIdentifier;
import static manifold.rt.api.util.ManIdentifierUtil.makePascalCaseIdentifier;
import static manifold.sql.rt.api.TxScope.*;
import static manifold.sql.rt.util.DriverInfo.Postgres;
import static manifold.sql.rt.util.DriverInfo.SQLite;

/**
 * The top-level class enclosing all the DDL types corresponding with a ".dbconfig" file.
 */
class SchemaParentType
{
  public static final String ENTITY = "Entity";

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

  void render( StringBuilder sb, JavaFileManager.Location location, IModule module, DiagnosticListener<JavaFileObject> errorHandler )
  {
    SrcLinkedClass srcClass = new SrcLinkedClass( getFqn(), Class, _model.getFile(), location, module, errorHandler )
      .addAnnotation( new SrcAnnotationExpression( DisableStringLiteralTemplates.class.getSimpleName() ) )
      .modifiers( Modifier.PUBLIC )
      .addInterface( SchemaType.class.getSimpleName() );
    addImports( srcClass );
    addTxScopeMethods( srcClass );
    addInnerTypes( srcClass );
    addFkColAssignMethod( srcClass );
    srcClass.render( sb, 0 );
  }

  private void addTxScopeMethods( SrcLinkedClass srcClass )
  {
    addNewScopeMethod( srcClass );

    addDefaultScopeMethod( srcClass );

    // these methods implement TxScope as static methods e.g., MyDatabase.commit() is shorthand for MyDatabase.defaultScope().commit()
    addGetDbConfigMethod( srcClass );
    addCommitMethod( srcClass );
    addRevertMethod( srcClass );
    addSqlChangeMethod( srcClass );
  }

  private void addDefaultScopeMethod( SrcLinkedClass srcClass )
  {
    SrcMethod method = new SrcMethod( srcClass )
      .modifiers( Modifier.PRIVATE | Modifier.STATIC )
      .name( "defaultScope" )
      .returns( new SrcType( TxScope.class.getSimpleName() ) )
      .body( "return ${Dependencies.class.getName()}.instance().getDefaultTxScopeProvider()" +
        ".defaultScope(${srcClass.getName()}.class);" );
    srcClass.addMethod( method );
  }

  private void addGetDbConfigMethod( SrcLinkedClass srcClass )
  {
    SrcMethod method = new SrcMethod( srcClass )
      .modifiers( Modifier.PUBLIC | Modifier.STATIC )
      .name( "getDbConfig" )
      .returns( DbConfig.class )
      .body( "return defaultScope().getDbConfig();" );
    srcClass.addMethod( method );
  }

  private void addCommitMethod( SrcLinkedClass srcClass )
  {
    SrcMethod method = new SrcMethod( srcClass )
      .modifiers( Modifier.PUBLIC | Modifier.STATIC )
      .name( "commit" )
      .throwsList( new SrcType( SQLException.class.getSimpleName() ) )
      .body( "defaultScope().commit();" );
    srcClass.addMethod( method );

    method = new SrcMethod( srcClass )
      .modifiers( Modifier.PUBLIC | Modifier.STATIC )
      .name( "commit" )
      .addParam( "changes", ScopeConsumer.class.getSimpleName() )
      .throwsList( new SrcType( SQLException.class.getSimpleName() ) )
      .body( "defaultScope().commit(changes);" );
    srcClass.addMethod( method );
  }

  private void addRevertMethod( SrcLinkedClass srcClass )
  {
    SrcMethod method = new SrcMethod( srcClass )
      .modifiers( Modifier.PUBLIC | Modifier.STATIC )
      .name( "revert" )
      .throwsList( new SrcType( SQLException.class.getSimpleName() ) )
      .body( "defaultScope().revert();" );
    srcClass.addMethod( method );
  }

  private void addSqlChangeMethod( SrcLinkedClass srcClass )
  {
    SrcMethod method = new SrcMethod( srcClass )
      .modifiers( Modifier.PUBLIC | Modifier.STATIC )
      .name( "addSqlChange" )
      .addParam( "sqlChange", ScopeConsumer.class.getSimpleName() )
      .body( "defaultScope().addSqlChange(sqlChange);" );
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
      addTableInterfaces( type, srcClass );
    }
  }

  private void addTableInterfaces( SchemaTable table, SrcLinkedClass enclosingType )
  {
    String identifier = getSchema().getJavaTypeName( table.getName() );
    String fqn = getFqn() + '.' + identifier;
    SrcLinkedClass srcClass = new SrcLinkedClass( fqn, enclosingType, Interface )
      .addInterface( Entity.class.getSimpleName() )
      .modifiers( Modifier.PUBLIC );
    extendCustomInterface( srcClass );
    addCustomBaseInterface( srcClass );
    addActualNameAnnotation( srcClass, table.getName(), false );
    addEntityClass( srcClass );
    addCreateMethods( srcClass, table );
    addReadMethods( srcClass, table );
    addDeleteMethods( srcClass );
    addBuilderType( srcClass, table );
    addBuilderMethod( srcClass, table );
    addTableInfoMethod( srcClass, table );
    addProperties( table, srcClass );
    addOneToManyMethods( table, srcClass );
    addManyToManyMethods( table, srcClass );
    enclosingType.addInnerClass( srcClass );
  }

  private void extendCustomInterface( SrcLinkedClass srcClass )
  {
    FqnCache<IFile> javaFiles = _model.getSchemaManifold().getModule().getPathCache().getExtensionCache( "java" );
    javaFiles.visitDepthFirst( file -> {
      if( file == null )
      {
        return true;
      }
      // custom interface must be top-level and follow naming convention:
      //   public interface "Custom" + <table interface name> extends CustomEntity<table interface name> e.g.,
      //   public interface CustomActor extends CustomEntity<Actor> {...}
      String customSimpleName = "Custom" + srcClass.getSimpleName();
      if( file.getBaseName().equals( customSimpleName ) )
      {
        try
        {
          String content = StreamUtil.getContent( new InputStreamReader( file.openInputStream(), UTF_8 ) );
          if( content.contains( "public interface " + customSimpleName ) && content.contains( srcClass.getName() ) )
          {
            String fqnIface = _model.getSchemaManifold().getModule().getPathCache()
              .getFqnForFile( file ).iterator().next();
            srcClass.addInterface( fqnIface );
            addForwardingStaticMethods( fqnIface );
            return false;
          }
        }
        catch( IOException e )
        {
          throw new RuntimeException( e );
        }
      }
      return true;
    } );

  }

  private void addForwardingStaticMethods( String fqnIface )
  {
    //todo: find all static methods in the custom interface and add the same to the entity interface that forwards to the custom interface
    // Note, this is necessary because interface static methods are not inherited (although java really should make it work if there are no ambiguities).
    // Still chewing on this one. For now, just call your methods from your custom interface
  }

  private void addCustomBaseInterface( SrcLinkedClass srcClass )
  {
    // check for custom base interface from dbconfig, which is inherited by ALL table interfaces in this config

    String customBaseInterface = _model.getDbConfig().getCustomBaseInterface();
    if( customBaseInterface != null && !customBaseInterface.isEmpty() )
    {
      if( !ManClassUtil.isValidClassName( customBaseInterface ) )
      {
        throw new RuntimeException( "Invalid custom class name: '" + customBaseInterface + "'" );
      }

      srcClass.addInterface( customBaseInterface );
    }
  }

  private void addEntityClass( SrcLinkedClass interfaceType )
  {
    String identifier = interfaceType.getSimpleName() + ENTITY;
    String fqn = getFqn() + '.' + identifier;
    SrcLinkedClass srcClass = new SrcLinkedClass( fqn, interfaceType, Class )
      .addInterface( interfaceType.getSimpleName() );
    setSuperClass( srcClass );
    SrcConstructor ctor = new SrcConstructor( srcClass )
      .modifiers( Modifier.PUBLIC )
      .addParam( new SrcParameter( "bindings", TxBindings.class )
        .addAnnotation( NotNull.class.getSimpleName() ) )
      .body( "super(bindings);" );
    srcClass.addConstructor( ctor );

    interfaceType.addInnerClass( srcClass );
  }

  @SuppressWarnings( "unused" )
  private void setSuperClass( SrcLinkedClass srcClass )
  {
    // Check for a dbconfig specified custom base class

    String customBaseClass = _model.getDbConfig().getCustomBaseClass();
    if( customBaseClass != null && !customBaseClass.isEmpty() )
    {
      if( !ManClassUtil.isValidClassName( customBaseClass ) )
      {
        throw new RuntimeException( "Invalid custom class name: '" + customBaseClass + "'" );
      }

      srcClass.superClass( customBaseClass );
    }
    else
    {
      // Default to our base class

      srcClass.superClass( BaseEntity.class );
    }
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
      addProperty( srcClass, col );
    }
  }

  private void addTableInfoMethod( SrcLinkedClass srcClass, SchemaTable table )
  {
    SrcField tableInfoField = new SrcField( "myTableInfo", new SrcType( LocklessLazyVar.class.getSimpleName() ).addTypeParam( TableInfo.class ) );
    StringBuilder sb = new StringBuilder( "LocklessLazyVar.make(() -> {\n" );

    sb.append( "      LinkedHashMap<String, ColumnInfo> allCols = new LinkedHashMap<>();\n" );
    for( Map.Entry<String, SchemaColumn> entry : table.getColumns().entrySet() )
    {
      //noinspection unused
      String colName = entry.getKey();
      SchemaColumn col = entry.getValue();
      //noinspection unused
      int jdbcType = col.getJdbcType();
      //noinspection unused
      String sqlType = col.getSqlType();
      //noinspection unused
      Integer size = col.getSize();
      sb.append( "      allCols.put(\"$colName\", new ColumnInfo(\"$colName\", $jdbcType, \"$sqlType\", $size, ${isRequired(col)}));\n");
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
        sb.append( "      ukCols.add(\"$ukColName\");\n" );
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
    if( table.getKind() != SchemaTable.Kind.Table )
    {
      return;
    }
    SchemaColumn[] requiredForeignKeys = table.getColumns().values().stream().filter( col -> isRequired( col ) && col.getForeignKey() != null ).toArray(SchemaColumn[]::new);
    CombinationUtil.createAllCombinations( requiredForeignKeys ).forEach( columnsAsReference -> addBuilderMethod( srcClass, table, columnsAsReference ) );
  }

  private void addBuilderMethod( SrcLinkedClass srcClass, SchemaTable table, List<SchemaColumn> columnsAsReference )
  {
    SrcMethod method = new SrcMethod( srcClass )
      .modifiers( Modifier.STATIC )
      .name( "builder" )
      .returns( new SrcType( "Builder" ) );
    addRequiredParameters( table, method, columnsAsReference );
    srcClass.addMethod( method );

    StringBuilder sb = new StringBuilder();
    sb.append( "return new Builder() {\n" );
    sb.append( "        ${Bindings.class.getName()} _bindings = new DataBindings();\n" );
    sb.append( "        {\n" );
    initFromParameters( table, sb, "_bindings", columnsAsReference );
    sb.append( "        }\n" );

    sb.append( "        @Override public ${Bindings.class.getName()} getBindings() { return _bindings; }\n" );
    sb.append( "      };" );
    method.body( sb.toString() );
  }

  private void addCreateMethods( SrcLinkedClass srcClass, SchemaTable table )
  {
    SchemaColumn[] requiredForeignKeys = table.getColumns().values().stream().filter( col -> isRequired( col ) && col.getForeignKey() != null ).toArray(SchemaColumn[]::new);
    CombinationUtil.createAllCombinations( requiredForeignKeys ).forEach( columnsAsReference -> addCreateMethods( srcClass, table, columnsAsReference ) );
  }

  private void addCreateMethods( SrcLinkedClass srcClass, SchemaTable table, List<SchemaColumn> columnsAsReference )
  {
    if( table.getKind() != SchemaTable.Kind.Table )
    {
      return;
    }

    String tableName = getTableFqn( table );
    SrcMethod method = new SrcMethod( srcClass )
      .modifiers( Modifier.STATIC )
      .name( "create" )
      .returns( new SrcType( tableName ) );
    addRequiredParameters(table, method, columnsAsReference);
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
      .addParam( new SrcParameter( "txScope", new SrcType( TxScope.class.getSimpleName() ) )
        .addAnnotation( NotNull.class.getSimpleName() ) );
    addRequiredParameters(table, method, columnsAsReference);
    srcClass.addMethod( method );

    sb = new StringBuilder();
    sb.append( "DataBindings args = new DataBindings();\n" );
    initFromParameters( table, sb, "args", columnsAsReference );
    sb.append( "      TxBindings bindings = new BasicTxBindings(txScope, TxKind.Insert, args);\n" );
    sb.append( "      $tableName customRow = ${Dependencies.class.getName()}.instance().getCustomEntityFactory().newInstance(bindings, $tableName.class);\n" );
    sb.append( "      $tableName entity = customRow != null ? customRow : new ${ManClassUtil.getShortClassName(tableName)}Entity(bindings);\n" );
    sb.append( "      ((OperableTxBindings)entity.getBindings()).setOwner(entity);\n" );
    sb.append( "      ((OperableTxScope)txScope).addRow(entity);\n" );
    sb.append( "      return entity;" );
    method.body( sb.toString() );
  }

  private void initFromParameters( SchemaTable table, StringBuilder sb, @SuppressWarnings( "unused" ) String bindingsVar, List<SchemaColumn> columnsAsReference )
  {
    Set<SchemaColumn> fkCovered = new HashSet<>();
    for( List<SchemaForeignKey> fk : table.getForeignKeys().values() )
    {
      for( SchemaForeignKey sfk : fk )
      {
        List<SchemaColumn> fkCols = sfk.getColumns();
        if( fkCols.stream().anyMatch( c -> isRequired( c ) && columnsAsReference.contains(c)) )
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

  private void addRequiredParameters( SchemaTable table, AbstractSrcMethod method, List<SchemaColumn> columnsAsReference )
  {
    // Note, parameters are added in order of appearance as they are with just columns, fks consolidate params

    Set<SchemaForeignKey> visited = new HashSet<>();
    for( SchemaColumn col: table.getColumns().values() )
    {
      if( isRequired( col ) )
      {
        if( col.getForeignKey() != null && columnsAsReference.contains(col) )
        {
          // Add fk ref param

          Set<SchemaForeignKey> sfkSet = getfk( table, col );
          for( SchemaForeignKey sfk : sfkSet )
          {
            if( !visited.contains( sfk ) )
            {
              visited.add( sfk );
              addFkParam( method, sfk );
            }
          }
        }
        else
        {
          // Add column param

          SrcParameter param = new SrcParameter( makePascalCaseIdentifier( col.getName(), false ), col.getType() );
          if( !col.getType().isPrimitive() )
          {
            param.addAnnotation( NotNull.class.getSimpleName() );
          }
          method.addParam( param );
        }
      }
    }
  }

  private void addFkParam( AbstractSrcMethod method, SchemaForeignKey sfk )
  {
    List<SchemaColumn> fkCols = sfk.getColumns();
    if( fkCols.stream().anyMatch( c -> isRequired( c ) ) )
    {
      String tableFqn = getTableFqn( sfk.getReferencedTable() );
      SrcType srcType = new SrcType( tableFqn );
      method.addParam( new SrcParameter( makePascalCaseIdentifier( sfk.getName(), false ), srcType )
        .addAnnotation( NotNull.class.getSimpleName() ) );
    }
  }

  private Set<SchemaForeignKey> getfk( SchemaTable table, SchemaColumn col )
  {
    Set<SchemaForeignKey> sfkSet = new LinkedHashSet<>();
    for( List<SchemaForeignKey> sfks : table.getForeignKeys().values() )
    {
      for( SchemaForeignKey sfk : sfks )
      {
        for( SchemaColumn column : sfk.getColumns() )
        {
          if( column == col )
          {
            sfkSet.add( sfk );
            break;
          }
        }
      }
    }
    return sfkSet;
  }

  private void addBuilderType( SrcLinkedClass enclosingType, SchemaTable table )
  {
    if( table.getKind() != SchemaTable.Kind.Table )
    {
      return;
    }

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
      .addParam( new SrcParameter( "txScope", new SrcType( TxScope.class.getSimpleName() ) )
        .addAnnotation( NotNull.class.getSimpleName() ) )
      .returns( new SrcType( tableName ) );
    srcInterface.addMethod( method );
    method.body(
        "BasicTxBindings bindings = new BasicTxBindings(txScope, TxKind.Insert, Builder.this.getBindings());\n" +
        "        $tableName customRow = ${Dependencies.class.getName()}.instance().getCustomEntityFactory().newInstance(bindings, $tableName.class);\n" +
        "        $tableName entity = customRow != null ? customRow : new ${ManClassUtil.getShortClassName(tableName)}Entity(bindings);\n" +
        "        ((OperableTxBindings)entity.getBindings()).setOwner(entity);\n" +
        "        ((OperableTxScope)txScope).addRow(entity);\n" +
        "        return entity;" );
  }

  private void addWithMethods( SrcLinkedClass srcClass, SchemaTable table )
  {
    for( Map.Entry<SchemaTable, List<SchemaForeignKey>> entry : table.getForeignKeys().entrySet() )
    {
      List<SchemaForeignKey> fk = entry.getValue();
      for( SchemaForeignKey sfk : fk )
      {
        List<SchemaColumn> fkCols = sfk.getColumns();
        if( fkCols.stream().noneMatch( c -> isRequired( c ) ) )
        {
          String tableFqn = getTableFqn( sfk.getReferencedTable() );
          SrcType srcType = new SrcType( tableFqn );

          //noinspection unused
          String propName = makePascalCaseIdentifier( sfk.getName(), true );
          SrcMethod withMethod = new SrcMethod()
            .modifiers( Flags.DEFAULT )
            .name( "with$propName" )
            .addParam( "${'$'}value", srcType )
            .returns( new SrcType( srcClass.getSimpleName() ) );
          addActualNameAnnotation( withMethod, sfk.getActualName(), true );
          StringBuilder sb = new StringBuilder();
          //noinspection unused
          for( SchemaColumn fkCol : fkCols )
          {
            //noinspection unused
            String colName = fkCol.getName();
            //noinspection unused
            String keyColName = fkCol.getForeignKey().getName();
            sb.append( "assignFkBindingValues(${'$'}value, \"${'$'}value\", \"$keyColName\", \"$colName\", getBindings());" );
          }
          sb.append( "return this;" );
          withMethod.body( sb.toString() );
          srcClass.addMethod( withMethod );
        }
      }
    }

    for( SchemaColumn col: table.getColumns().values() )
    {
      if( !isRequired( col ) )
      {
        //noinspection unused
        String actualName = col.getName();
        //noinspection unused
        String propName = makePascalCaseIdentifier( actualName, true );
        SrcMethod withMethod = new SrcMethod()
          .modifiers( Flags.DEFAULT )
          .name( "with$propName" )
          .addParam( "${'$'}value", col.getType() )
          .returns( new SrcType( srcClass.getSimpleName() ) );
        addActualNameAnnotation( withMethod, actualName, true );
        withMethod.body( "getBindings().put(\"$actualName\", ${'$'}value); return this;" );
        srcClass.addMethod( withMethod );
      }
    }
  }

  private boolean isRequired( SchemaColumn col )
  {
    return !col.isNullable() &&
      !col.isGenerated() &&
      !col.isAutoIncrement() &&
      col.getDefaultValue() == null &&
      !bullshit( col );
  }

  private boolean mayBeNullBeforeCommit( SchemaColumn col )
  {
    return !col.getType().isPrimitive() &&
           (col.isNullable() ||
            // these are relevant when entity is not yet committed
            col.isGenerated() ||
            col.isAutoIncrement() ||
            col.getDefaultValue() != null);
  }

  private boolean bullshit( SchemaColumn col )
  {
    // Sqlite: a generated, auto-increment id, but since sqlite is retarded at all levels...
    DriverInfo driver = col.getOwner().getSchema().getDriverInfo();
    boolean isSqliteRowId = driver == SQLite &&
      col.isNonNullUniqueId() && col.isPrimaryKeyPart() && col.getJdbcType() == Types.INTEGER;
    if( isSqliteRowId )
    {
      return true;
    }

    // Postgres: tsvector is always assigned, s/b 'generated always', but sometimes triggers are used :\
    boolean isPostgresTsvector = driver == Postgres && col.getSqlType().equalsIgnoreCase( "tsvector" );
    //noinspection RedundantIfStatement
    if( isPostgresTsvector )
    {
      return true;
    }

    return false;
  }

  private void addImports( SrcLinkedClass srcClass )
  {
    srcClass.addImport( Bindings.class );
    srcClass.addImport( TxBindings.class );
    srcClass.addImport( TxScope.class );
    srcClass.addImport( ScopeConsumer.class );
    srcClass.addImport( OperableTxScope.class );
    srcClass.addImport( OperableTxBindings.class );
    srcClass.addImport( BasicTxBindings.class );
    srcClass.addImport( TxKind.class );
    srcClass.addImport( DataBindings.class );
    srcClass.addImport( Entity.class );
    srcClass.addImport( TableInfo.class );
    srcClass.addImport( ColumnInfo.class );
    srcClass.addImport( SchemaType.class );
    srcClass.addImport( SchemaBuilder.class );
    srcClass.addImport( QueryContext.class );
    srcClass.addImport( CrudProvider.class );
    srcClass.addImport( Runner.class );
    srcClass.addImport( TxScopeProvider.class );
    srcClass.addImport( KeyRef.class );
    srcClass.addImport( LocklessLazyVar.class );
    srcClass.addImport( Collections.class );
    srcClass.addImport( LinkedHashMap.class );
    srcClass.addImport( List.class );
    srcClass.addImport( Map.class );
    srcClass.addImport( Set.class );
    srcClass.addImport( HashSet.class );
    srcClass.addImport( SQLException.class );
    srcClass.addImport( Nullable.class );
    srcClass.addImport( NotNull.class );
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
      .name( "fetch" + propName )
      .modifiers( Flags.DEFAULT )
      .returns( type );
    boolean isNullable = sfk.getColumns().stream().allMatch( c -> c.isNullable() );
    fkFetchMethod.addAnnotation( isNullable ? Nullable.class.getSimpleName() : NotNull.class.getSimpleName() );
    StringBuilder sb = new StringBuilder();
    sb.append( "DataBindings paramBindings = new DataBindings();\n" );
    List<SchemaColumn> columns = sfk.getColumns();
    for( int i = 0; i < columns.size(); i++ )
    {
      SchemaColumn col = columns.get( i );
      //noinspection unused
      Column referencedCol = col.getForeignKey();
      if( i == 0 )
      {
        // get assigned ref that is newly created and not committed yet
        sb.append( "    Object maybeRef = getBindings().get(\"${col.getName()}\");\n" )
          .append( "    if(maybeRef instanceof ${Entity.class.getSimpleName()}) {return ($tableFqn)maybeRef;}\n" );
      }
      sb.append( "    paramBindings.put(\"${referencedCol.getName()}\", getBindings().get(\"${col.getName()}\"));\n" );
    }

    //noinspection unused
    String columnInfo = getColumnInfo( sfk.getColumns() );
    //noinspection unused
    String configName = _model.getDbConfig().getName();
    sb.append( "    return ${Dependencies.class.getName()}.instance().getCrudProvider().readOne(" +
      "new QueryContext<$tableFqn>(getBindings().getTxScope(), $tableFqn.class, \"${table.getName()}\", myTableInfo.get().getAllCols(), $columnInfo, paramBindings, \"$configName\", " +
      "rowBindings -> {" +
      "  $tableFqn customRow = ${Dependencies.class.getName()}.instance().getCustomEntityFactory().newInstance(rowBindings, $tableFqn.class);\n" +
      "  return customRow != null ? customRow : new $tableFqn.${ManClassUtil.getShortClassName(tableFqn)}Entity(rowBindings);" +
      "} ));" );
    fkFetchMethod.body( sb.toString() );
    addActualNameAnnotation( fkFetchMethod, name, true );
    srcClass.addMethod( fkFetchMethod );

    SrcMethod fkSetter = new SrcMethod( srcClass )
      .modifiers( Flags.DEFAULT )
      .name( "set" + propName );
    SrcParameter param = new SrcParameter( "ref", new SrcType( tableFqn ) );
    fkSetter.addParam( param );
    if( !isNullable )
    {
      param.addAnnotation( NotNull.class.getSimpleName() );
    }
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
      .addParam( "ref", new SrcType( Entity.class ) )
      .addParam( "propName", new SrcType( String.class ) )
      .addParam( "keyColName", new SrcType( String.class ) )
      .addParam( "colName", new SrcType( String.class ) )
      .addParam( "bindings", new SrcType( Bindings.class ) );
      method.body( "if(ref == null) throw new NullPointerException(\"Expecting non-null value for: \" + propName );\n" +
        "    Object keyColValue = ref.getBindings().get(keyColName);\n" +
        "    bindings.put(colName, keyColValue != null ? keyColValue : new KeyRef(ref, keyColName));" );
    srcClass.addMethod( method );
  }

  private void addProperty( SrcLinkedClass srcInterface, SchemaColumn col )
  {
    Class<?> type = col.getType();
    String name = col.getName();

    SrcType propType = makeSrcType( type );
    String propName = makePascalCaseIdentifier( name, true );
    //noinspection unused
    String colName = makeIdentifier( name );

    SrcGetProperty getter = new SrcGetProperty( propName, propType, true )
      .modifiers( Flags.DEFAULT );
    if( !col.getType().isPrimitive() )
    {
      getter.addAnnotation( mayBeNullBeforeCommit( col ) ? Nullable.class.getSimpleName() : NotNull.class.getSimpleName() );
    }
    StringBuilder retType = new StringBuilder();
    propType.render( retType, 0, false ); // calling render to include array "[]"
    if( col.getForeignKey() != null )
    {
      getter.body( "Object value = getBindings().get(\"$colName\");\n" +
                   "    return value instanceof ${Entity.class.getSimpleName()} ? null : ($retType)value;" );
    }
    else
    {
      getter.body( "return ($retType)getBindings().get(\"$colName\");" );
    }
    addActualNameAnnotation( getter, name, true );
    srcInterface.addGetProperty( getter );

    if( !col.isGenerated() && !col.isAutoIncrement() )
    {
      SrcSetProperty setter = new SrcSetProperty( propName, propType )
        .modifiers( Flags.DEFAULT );
      if( !col.isNullable() )
      {
        setter.getParameters().get( 0 ).addAnnotation( NotNull.class.getSimpleName() );
      }
      setter.body( "getBindings().put(\"$colName\", ${'$'}value);" );
      addActualNameAnnotation( setter, name, true );
      srcInterface.addSetProperty( setter );
    }
  }

  private void addReadMethods( SrcLinkedClass srcClass, SchemaTable table )
  {
    addFetchPkParamsMethods( srcClass, table );
    addFetchByRequiredParamMethods( srcClass, table );
    addFetchAllMethods( srcClass, table );
  }

  private void addFetchAllMethods(SrcLinkedClass srcClass, SchemaTable table )
  {
    String tableFqn = getTableFqn( table );
    SrcMethod method = new SrcMethod( srcClass )
            .modifiers( Modifier.STATIC )
            .name( "fetchAll" )
            .returns( new SrcType( "Iterable<$tableFqn>" ) )
            .body("return fetchAll(defaultScope());");
    srcClass.addMethod( method );

    method = new SrcMethod( srcClass )
            .modifiers( Modifier.STATIC )
            .name( "fetchAll" )
            .returns( new SrcType( "Iterable<$tableFqn>" ) )
            .addParam( new SrcParameter( "txScope", new SrcType( TxScope.class.getSimpleName() ) )
                    .addAnnotation( NotNull.class.getSimpleName() ) );

            String query = "select * from ${table.getEscapedName()}";

            //noinspection UnusedAssignment
            query = ManEscapeUtil.escapeForJavaStringLiteral( query );
            //noinspection unused
            String simpleName = srcClass.getSimpleName();
            String columnInfo = getColumnInfo( new ArrayList<>( table.getColumns().values() ) );
            String configName = _model.getDbConfig().getName();
            StringBuilder sb = new StringBuilder();
            sb.append(
                    "    return new Runner<$tableFqn>(" +
                            "new QueryContext<>(txScope, $tableFqn.class, null, myTableInfo.get().getAllCols(), $columnInfo, DataBindings.EMPTY_BINDINGS, \"$configName\",\n" +
                            "      rowBindings -> " );
            sb.append(
                    "{\n" +
                    "        $tableFqn customRow = ${Dependencies.class.getName()}.instance().getCustomEntityFactory().newInstance(rowBindings, $tableFqn.class);\n" +
                    "        return customRow != null ? customRow : new $tableFqn.${ManClassUtil.getShortClassName(tableFqn)}Entity(rowBindings);\n" +
                    "      }" );
            sb.append(
                    " ),\n" +
                            "      \"$query\"\n" +
                            "    ).fetch();" );
            method.body( sb.toString() );
            srcClass.addMethod( method );
  }

  // Foo.fetchBy(...);
  private void addFetchByRequiredParamMethods( SrcLinkedClass srcClass, SchemaTable table )
  {
    for( SchemaColumn col: table.getColumns().values() )
    {
//      if( isRequired( col ) )
//      {
        addFetchByRequiredParamMethod( srcClass, table, col );
//      }
    }
  }
  private void addFetchByRequiredParamMethod( SrcLinkedClass srcClass, SchemaTable table, SchemaColumn column )
  {
    //noinspection unused
    String tableFqn = getTableFqn( table );
    //noinspection unused
    String propName = makePascalCaseIdentifier( column.getName(), true );
    SrcMethod method = new SrcMethod( srcClass )
      .modifiers( Modifier.STATIC )
      .name( "fetchBy$propName" )
      .returns( new SrcType( "List<$tableFqn>" ) );
    String paramName = makePascalCaseIdentifier( column.getName(), false );
    SrcParameter param = new SrcParameter( paramName, column.getType() );
    if( !column.getType().isPrimitive() && isRequired( column ) )
    {
      param.addAnnotation( NotNull.class.getSimpleName() );
    }
    method.addParam( param );
    List<SchemaColumn> whereCols = Collections.singletonList( column );
    StringBuilder sb = new StringBuilder();
    sb.append( "return fetchBy$propName(defaultScope()" );
    sb.append( method.getParameters().isEmpty() ? "" : ", " );
    method.forwardParameters( sb );
    sb.append( ");" );
    method.body( sb.toString() );
    srcClass.addMethod( method );


    method = new SrcMethod( srcClass )
      .modifiers( Modifier.STATIC )
      .name( "fetchBy$propName" )
      .returns( new SrcType( "List<$tableFqn>" ) )
      .addParam( new SrcParameter( "txScope", new SrcType( TxScope.class.getSimpleName() ) )
        .addAnnotation( NotNull.class.getSimpleName() ) );
    param = new SrcParameter( paramName, column.getType() );
    if( !column.getType().isPrimitive() && isRequired( column ) )
    {
      param.addAnnotation( NotNull.class.getSimpleName() );
    }
    method.addParam( param );
    //noinspection unused
    String columnInfo = getColumnInfo( whereCols );
    //noinspection unused
    String configName = _model.getDbConfig().getName();
    sb = new StringBuilder();
    sb.append( "DataBindings paramBindings = new DataBindings();\n" );
    sb.append( "    paramBindings.put(\"${column.getName()}\", $paramName);\n" );
    sb.append( "    return ${Dependencies.class.getName()}.instance().getCrudProvider().readMany(new QueryContext<$tableFqn>(txScope, $tableFqn.class,\n" +
      "      \"${table.getName()}\", myTableInfo.get().getAllCols(), $columnInfo, paramBindings, \"$configName\",\n" +
      "      rowBindings -> {" +
      "        $tableFqn customRow = ${Dependencies.class.getName()}.instance().getCustomEntityFactory().newInstance(rowBindings, $tableFqn.class);\n" +
      "        return customRow != null ? customRow : new $tableFqn.${ManClassUtil.getShortClassName(tableFqn)}Entity(rowBindings);" +
      "      } ));" );
    method.body( sb.toString() );
    srcClass.addMethod( method );
  }

  // Foo.fetch(...);
  // Foo.fetch(txScope, ...);
  private void addFetchPkParamsMethods( SrcLinkedClass srcClass, SchemaTable table )
  {
    String tableFqn = getTableFqn( table );
    SrcMethod method = new SrcMethod( srcClass )
      .modifiers( Modifier.STATIC )
      .name( "fetch" )
      .returns( new SrcType( tableFqn ) );
    List<SchemaColumn> whereCols = addPkParameters( table, method );
    if( whereCols.isEmpty() )
    {
      // no pk and no uk, no read method, instead use type-safe sql query :)
      return;
    }
    StringBuilder sb = new StringBuilder();
    sb.append( "return fetch(defaultScope()" );
    sb.append( method.getParameters().isEmpty() ? "" : ", " );
    method.forwardParameters( sb );
    sb.append( ");" );
    method.body( sb.toString() );
    srcClass.addMethod( method );


    method = new SrcMethod( srcClass )
      .modifiers( Modifier.STATIC )
      .name( "fetch" )
      .returns( new SrcType( tableFqn ) )
      .addParam( new SrcParameter( "txScope", new SrcType( TxScope.class.getSimpleName() ) )
        .addAnnotation( NotNull.class.getSimpleName() ) );
    whereCols = addPkParameters( table, method );
    //noinspection unused
    String columnInfo = getColumnInfo( whereCols );
    //noinspection unused
    String configName = _model.getDbConfig().getName();
    sb = new StringBuilder();
    sb.append( "DataBindings paramBindings = new DataBindings();\n" );
    for( SchemaColumn col : whereCols )
    {
      //noinspection unused
      String paramName = makePascalCaseIdentifier( col.getName(), false );
      sb.append( "    paramBindings.put(\"${col.getName()}\", $paramName);\n" );
    }
    sb.append( "    return ${Dependencies.class.getName()}.instance().getCrudProvider().readOne(new QueryContext<$tableFqn>(txScope, $tableFqn.class,\n" +
      "      \"${table.getName()}\", myTableInfo.get().getAllCols(), $columnInfo, paramBindings, \"$configName\",\n" +
      "      rowBindings -> {" +
      "        $tableFqn customRow = ${Dependencies.class.getName()}.instance().getCustomEntityFactory().newInstance(rowBindings, $tableFqn.class);\n" +
      "        return customRow != null ? customRow : new $tableFqn.${ManClassUtil.getShortClassName(tableFqn)}Entity(rowBindings);" +
      "      } ));" );
    method.body( sb.toString() );
    srcClass.addMethod( method );
  }

  private List<SchemaColumn> addPkParameters( SchemaTable table, AbstractSrcMethod method )
  {
    List<SchemaColumn> pk = table.getPrimaryKey();
    if( !pk.isEmpty() )
    {
      addParameters( method, pk );
      return pk;
    }
    else
    {
      for( Map.Entry<String, List<SchemaColumn>> entry : table.getNonNullUniqueKeys().entrySet() )
      {
        addParameters( method, entry.getValue() );
        return entry.getValue();
      }
    }
    return Collections.emptyList();
  }

  private static void addParameters( AbstractSrcMethod method, List<SchemaColumn> keyCols )
  {
    for( SchemaColumn col : keyCols )
    {
      SrcParameter param = new SrcParameter( makePascalCaseIdentifier( col.getName(), false ), col.getType() );
      if( !col.isNullable() )
      {
        param.addAnnotation( NotNull.class.getSimpleName() );
      }
      method.addParam( param );
    }
  }

  private void addDeleteMethods( SrcLinkedClass srcClass )
  {
    SrcMethod delete = new SrcMethod( srcClass )
      .modifiers( Flags.DEFAULT )
      .name( "delete" );
    delete.body( "((OperableTxBindings)getBindings()).setDelete(true);" );
    srcClass.addMethod( delete );

    SrcMethod undelete = new SrcMethod( srcClass )
      .modifiers( Flags.DEFAULT )
      .name( "undelete" );
    undelete.body( "((OperableTxBindings)getBindings()).setDelete(false);" );
    srcClass.addMethod( undelete );
  }

  private void addOneToManyMethods( SchemaTable table, SrcLinkedClass srcClass )
  {
    for( SchemaForeignKey sfk : table.getOneToMany() )
    {
      addOneToManyFetcher( srcClass, sfk );
//      addAddToMany( srcClass, sfk );
//      addRemoveFromMany( srcClass, sfk );
    }
  }
  private void addOneToManyFetcher( SrcLinkedClass srcClass, SchemaForeignKey fkToThis )
  {
    // e.g., for a Post that has comments there will be a link table PostComment where, fetchPostCommentList() { SELECT * FROM post_comment WHERE post_comment.post_id = :post_id }
    //noinspection unused
    String tableFqn = getTableFqn( fkToThis.getOwnTable() );
    //noinspection unused
    SrcType type = new SrcType( tableFqn );
    //noinspection unused
    String propName = makePascalCaseIdentifier( fkToThis.getQualifiedName(), true );
    SrcMethod method = new SrcMethod( srcClass )
      .modifiers( Flags.DEFAULT )
      .name( "fetch${propName}s" )
      .returns( new SrcType( "List<$tableFqn>" ) );
    //noinspection unused
    String configName = fkToThis.getReferencedTable().getSchema().getDbConfig().getName();
    //noinspection unused
    String tableName = fkToThis.getOwnTable().getName();
    //noinspection unused
    String columnInfo = getColumnInfo( fkToThis.getColumns() );
    StringBuilder sb = new StringBuilder();
    sb.append( "DataBindings paramBindings = new DataBindings();\n" );
    for( SchemaColumn col : fkToThis.getColumns() )
    {
      //noinspection unused
      Column referencedCol = col.getForeignKey();
      sb.append( "    Object value = getBindings().get(\"${col.getName()}\");\n" )
        .append( "    if(value instanceof ${Entity.class.getSimpleName()}) return Collections.emptyList();\n" )
        .append( "    paramBindings.put(\"${referencedCol.getName()}\", value);\n" );
    }
    sb.append( "    return ${Dependencies.class.getName()}.instance().getCrudProvider().readMany(" +
      "      new QueryContext<$tableFqn>(getBindings().getTxScope(), $tableFqn.class, \"$tableName\", myTableInfo.get().getAllCols(), $columnInfo, paramBindings, \"$configName\", " +
      "      rowBindings -> {" +
      "        $tableFqn customRow = ${Dependencies.class.getName()}.instance().getCustomEntityFactory().newInstance(rowBindings, $tableFqn.class);\n" +
      "        return customRow != null ? customRow : new $tableFqn.${ManClassUtil.getShortClassName(tableFqn)}Entity(rowBindings);" +
      "      } ));" );
    method.body( sb.toString() );
    srcClass.addMethod( method );
  }

  private void addManyToManyMethods( SchemaTable table, SrcLinkedClass srcClass )
  {
    for( Pair<SchemaColumn, SchemaColumn> uk : table.getManyToMany() )
    {
      addManyToManyFetcher( srcClass, table, uk );
    }
  }
  private void addManyToManyFetcher( SrcLinkedClass srcClass, SchemaTable table, Pair<SchemaColumn, SchemaColumn> uk )
  {
    SchemaColumn fkToMe;
    SchemaColumn fkToOther;
    if( uk.getFirst().getForeignKey().getOwner() == table )
    {
      fkToMe = uk.getFirst();
      fkToOther = uk.getSecond();
    }
    else
    {
      fkToMe = uk.getSecond();
      fkToOther = uk.getFirst();
    }

    // e.g., select * from CATEGORY join FILM_CATEGORY on CATEGORY.CATEGORY_ID = FILM_CATEGORY.CATEGORY_ID where FILM_CATEGORY.FILM_ID = :FILM_ID;
    //noinspection unused
    String tableFqn = getTableFqn( fkToOther.getForeignKey().getOwner() );
    //noinspection unused
    String propName = makePascalCaseIdentifier( JdbcSchemaForeignKey.removeId( fkToOther.getName() ) + "_ref", true );
    SrcMethod method = new SrcMethod( srcClass )
      .modifiers( Flags.DEFAULT )
      .name( "fetch${propName}s" )
      .returns( new SrcType( "List<$tableFqn>" ) );
    //noinspection unused
    String configName = fkToOther.getForeignKey().getOwner().getSchema().getDbConfig().getName();
    //noinspection unused
    String otherTable = fkToOther.getForeignKey().getOwner().getEscapedName();
    //noinspection unused
    String linkTable = fkToOther.getOwner().getEscapedName();

    //noinspection unused
    String sql = ManEscapeUtil.escapeForJavaStringLiteral("select * from $otherTable " +
      "join $linkTable on ${makeJoinOn( fkToOther )} " +
      "where ${makeJoinWhere( fkToMe )}");

    //noinspection unused
    String columnInfo = getColumnInfo( Collections.singletonList( fkToMe.getForeignKey() ) );

    StringBuilder sb = new StringBuilder();
    sb.append( "DataBindings paramBindings = new DataBindings();\n" );
    //noinspection unused
    SchemaColumn referencedCol = fkToMe.getForeignKey();
    sb.append( "      Object value = getBindings().get(\"${fkToMe.getName()}\");\n" )
      .append( "      if(value instanceof ${Entity.class.getSimpleName()}) return Collections.emptyList();\n" )
      .append( "      paramBindings.put(\"${referencedCol.getName()}\", value);\n" );
    sb.append( "      return new ${Runner.class.getName()}<$tableFqn>(\n" +
      "          new QueryContext<$tableFqn>(getBindings().getTxScope(), $tableFqn.class, null, myTableInfo.get().getAllCols(), $columnInfo, paramBindings, \"$configName\", \n" +
      "          rowBindings -> {" +
      "            $tableFqn customRow = ${Dependencies.class.getName()}.instance().getCustomEntityFactory().newInstance(rowBindings, $tableFqn.class);\n" +
      "            return customRow != null ? customRow : new $tableFqn.${ManClassUtil.getShortClassName(tableFqn)}Entity(rowBindings);" +
      "          } ), \"$sql\")\n" +
      "        .fetch().toList();" );
    method.body( sb.toString() );
    srcClass.addMethod( method );
  }

  @SuppressWarnings( "unused" )
  private String makeJoinOn( SchemaColumn fkToOther )
  {
    StringBuilder sb = new StringBuilder();
    SchemaColumn refCol = fkToOther.getForeignKey();
    sb.append( refCol.getOwner().getEscapedName() ).append( '.' ).append( refCol.getEscapedName() ).append( " = " )
      .append( fkToOther.getOwner().getEscapedName() ).append( '.' ).append( fkToOther.getEscapedName() );
    return sb.toString();
  }

  @SuppressWarnings( "unused" )
  private String makeJoinWhere( SchemaColumn fkToMe )
  {
    StringBuilder sb = new StringBuilder();
    SchemaColumn refCol = fkToMe.getForeignKey();
    sb.append( fkToMe.getOwner().getEscapedName() ).append( '.' ).append( fkToMe.getEscapedName() ).append( " = ?" );
    return sb.toString();
  }

//  private void addAddToMany( SrcLinkedClass srcClass, SchemaForeignKey fkToThis )
//  {
//    //todo: add add<fk-to-this>() method.
//    // For one-to-many e.g., for a Post table that has Comments, addComment(String text) { Comment.create(this, text)... }
//    // This method will have the same parameters as the fk table's create method, minus the fk's fk to this table's id, which we can set directly in the body of the method
//    // ...or not.  After further thought, I think it's better to just rely on create() or, if an existing object is changing it's fk, then let that happen naturally.
//    // no need for addToXxxRefs()
//    // For many-to-many e.g., for a Blog table that has Subscribers via link table BlogSubscriber, addBlogSubscriber(Subscriber s)
//    // Same basic logic, no need for addTo/removeFrom stuff. Better to let it happen naturally.
//  }
//  // many-to-many is special with remove. since it involves another instance, we delete via id, whereas with a Comment a simple delete(), no need for a removeXxx method
//  private void addRemoveFromMany()
//  {
//    //todo: add remove<fk-to-this>() method.
//    // e.g., for a Blog table that has Subscribers there will be a link table BlogSubscriber,
//    // removeBlogSubscriber(Subscriber s) { delete() method uses a local delete stmt: delete from BlogSubscriber where blog_id = :blog_id AND subscriber_id = :subscriber_id }
//  }

  private String getColumnInfo( List<SchemaColumn> parameters )
  {
    StringBuilder sb = new StringBuilder( "new ColumnInfo[]{");
    for( int i = 0; i < parameters.size(); i++ )
    {
      //noinspection unused
      Column p = parameters.get( i );
      if( i > 0 )
      {
        sb.append( ", " );
      }
      sb.append( "new ColumnInfo(\"${p.getName()}\", ${p.getJdbcType()}, \"{p.getSqlType()}\", ${p.getSize()})" );
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

  private SrcType makeSrcType( Class<?> type )
  {
    String typeName = getJavaName( type );
    SrcType srcType = new SrcType( typeName );
    srcType.setPrimitive( type.isPrimitive() );
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
