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

package manifold.sql.query.type;

import com.sun.tools.javac.code.Flags;
import manifold.api.fs.IFileFragment;
import manifold.api.gen.*;
import manifold.api.host.IModule;
import manifold.internal.javac.HostKind;
import manifold.internal.javac.IIssue;
import manifold.json.rt.api.*;
import manifold.rt.api.*;
import manifold.rt.api.util.ManClassUtil;
import manifold.rt.api.util.ManEscapeUtil;
import manifold.rt.api.util.Pair;
import manifold.sql.api.DataElement;
import manifold.sql.query.api.ForeignKeyQueryRef;
import manifold.sql.query.api.QueryColumn;
import manifold.sql.query.api.QueryParameter;
import manifold.sql.query.api.QueryTable;
import manifold.sql.rt.api.DbConfig;
import manifold.sql.rt.api.Runner;
import manifold.sql.rt.api.Query;
import manifold.sql.rt.api.ResultRow;
import manifold.sql.schema.api.SchemaTable;

import javax.tools.DiagnosticListener;
import javax.tools.JavaFileManager;
import javax.tools.JavaFileObject;
import java.lang.reflect.Modifier;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static manifold.api.gen.AbstractSrcClass.Kind.*;
import static manifold.api.gen.SrcLinkedClass.addActualNameAnnotation;
import static manifold.api.gen.SrcLinkedClass.makeIdentifier;
import static manifold.rt.api.util.ManIdentifierUtil.makePascalCaseIdentifier;

/**
 * The top-level class defining an .sql file.
 */
class SqlParentType
{
  private static final String ANONYMOUS_TYPE = "Anonymous_";

  private final SqlModel _model;
  private int _anonCount;

  SqlParentType( SqlModel model )
  {
    _model = model;
  }

  private String getFqn()
  {
    return _model.getFqn();
  }

  void render( StringBuilder sb, JavaFileManager.Location location, IModule module, DiagnosticListener<JavaFileObject> errorHandler )
  {
    String name = getQueryName();
    //noinspection unused
    String identifier = makeIdentifier( name, false );
    SrcLinkedClass srcClass = new SrcLinkedClass( getFqn(), Interface, _model.getFile(), location, module, errorHandler )
      .addAnnotation( new SrcAnnotationExpression( DisableStringLiteralTemplates.class.getSimpleName() ) )
      .addInterface( new SrcType( "Query" ) )
      .modifiers( Modifier.PUBLIC );
    addActualNameAnnotation( srcClass, name, false );
    addImports( srcClass );
    addFlatRowType( srcClass );
    addRunMethods( srcClass );
    addFragmentValueMethod( srcClass );

    srcClass.render( sb, 0 );
  }

  private void addRunMethods( SrcLinkedClass srcClass )
  {
    //addRunMethod( srcClass, "runFlat", "FlatRow" );

    Pair<SchemaTable, List<QueryColumn>> selectedTable = getQuery().findSelectedTable();
    String rowType;
    if( selectedTable != null && selectedTable.getSecond().size() == getQuery().getColumns().size() )
    {
      // Single table query can be represented directly as TableResult e.g., SELECT * queries.
      SchemaTable table = selectedTable.getFirst();
      rowType = getTableFqn( table );
    }
    else
    {
      rowType = "Row";
      addRowType( srcClass );
    }

    addRunMethod( srcClass, "run", rowType );
  }

  private QueryTable getQuery()
  {
    return _model.getQuery();
  }

  private void addFragmentValueMethod( SrcLinkedClass srcClass )
  {
    if( !(_model.getFile() instanceof IFileFragment) )
    {
      return;
    }

    if( !isValueFragment( ((IFileFragment)_model.getFile()).getHostKind() ) )
    {
      return;
    }

    addValueMethodForOperation( srcClass );
  }

  private void addValueMethodForOperation( SrcLinkedClass srcClass )
  {
    srcClass.addAnnotation( new SrcAnnotationExpression( FragmentValue.class.getSimpleName() )
      .addArgument( "methodName", String.class, "fragmentValue" )
      .addArgument( "type", String.class, getFqn() ) );

    String simpleName = srcClass.getSimpleName();
    SrcMethod valueMethod = new SrcMethod( srcClass )
      .modifiers( Modifier.STATIC )
      .name( "fragmentValue" )
      .returns( simpleName )
      .body( "return new $simpleName() {};" );
    srcClass.addMethod( valueMethod );
  }

  private String getQueryName()
  {
    if( getQuery() == null )
    {
      return ManClassUtil.getShortClassName( getFqn() );
    }

    String name = getQuery().getName();
    return (name == null || name.isEmpty()) ? ANONYMOUS_TYPE + _anonCount++ : name;
  }

  private void addRunMethod( SrcLinkedClass srcClass, String methodName, @SuppressWarnings( "unused" ) String rowType )
  {
    SrcMethod method = new SrcMethod( srcClass )
      .name( methodName )
      .returns( new SrcType( "Iterable<$rowType>" ) );
    if( _model.getFile() instanceof IFileFragment &&
      isValueFragment( ((IFileFragment)_model.getFile()).getHostKind() ) )
    {
      method.modifiers( Flags.DEFAULT );
    }
    else
    {
      method.modifiers( Modifier.STATIC );
    }
    addRequiredParameters( method );
    StringBuilder sb = new StringBuilder();
    sb.append( "    DataBindings paramBindings = new DataBindings(new ConcurrentHashMap());\n" );
    for( SrcParameter param : method.getParameters() )
    {
      //noinspection unused
      String paramName = makeIdentifier( param.getSimpleName(), false );
      sb.append( "    paramBindings.put(\"$paramName\", $paramName);\n" );
    }

    //noinspection unused
    String query = getQuery() == null ? "errant query" : getQuery().getQuerySource();
    query = ManEscapeUtil.escapeForJavaStringLiteral( query );
    //noinspection unused
    String configName = _model.getScope().getDbconfig().getName();
    //noinspection unused
    String simpleName = srcClass.getSimpleName();
    //noinspection unused
    String jdbcParamTypes = getJdbcParamTypes();
    sb.append(
      "    return new Runner<$rowType>($rowType.class, $jdbcParamTypes, paramBindings, \"$query\", \"$configName\", " +
      "      rowBindings -> new $rowType() {public Bindings getBindings() { return rowBindings; }}" +
      "    ).run();" );
    method.body( sb.toString() );
    srcClass.addMethod( method );
  }

  private String getJdbcParamTypes()
  {
    StringBuilder sb = new StringBuilder( "new int[]{");
    List<QueryParameter> parameters = getQuery().getParameters();
    for( int i = 0; i < parameters.size(); i++ )
    {
      QueryParameter p = parameters.get( i );
      if( i > 0 )
      {
        sb.append( "," );
      }
      sb.append( p.getJdbcType() );
    }
    return sb.append( "}" ).toString();
  }

  private boolean isValueFragment( HostKind hostKind )
  {
    switch( hostKind )
    {
      case DOUBLE_QUOTE_LITERAL:
      case TEXT_BLOCK_LITERAL:
        return true;
    }
    return false;
  }

  private void addRequiredParameters( AbstractSrcMethod method )
  {
    if( getQuery() == null )
    {
      // handle errant query
      return;
    }

    for( QueryParameter param: getQuery().getParameters() )
    {
      java.lang.Class<?> type = getType( param );
      if( type == null )
      {
        // errant condition
        type = Object.class;
      }
      method.addParam( makeIdentifier( param.getName(), false ), new SrcType( type ) );
    }
  }

  private void addFlatRowType( SrcLinkedClass enclosingType )
  {
    String fqn = enclosingType.getName() + ".FlatRow";
    SrcLinkedClass srcClass = new SrcLinkedClass( fqn, enclosingType, Interface )
      .addInterface( ResultRow.class.getSimpleName() )
      .modifiers( Modifier.PUBLIC );

    if( getQuery() != null )
    {
      for( QueryColumn column : getQuery().getColumns().values() )
      {
        addQueryGetter( srcClass, column );
      }

      for( ForeignKeyQueryRef fkRef : getQuery().findForeignKeyQueryRefs() )
      {
        addFkFetcher( srcClass, fkRef );
      }
    }
    enclosingType.addInnerClass( srcClass );
  }

  private void addRowType( SrcLinkedClass enclosingType )
  {
    String fqn = enclosingType.getName() + ".Row";
    SrcLinkedClass srcClass = new SrcLinkedClass( fqn, enclosingType, Interface )
      .addInterface( ResultRow.class.getSimpleName() )
      .modifiers( Modifier.PUBLIC );

    if( getQuery() != null )
    {
      Map<String, QueryColumn> columns = getQuery().getColumns();

      Pair<SchemaTable, List<QueryColumn>> selectedTable = getQuery().findSelectedTable();
      if( selectedTable != null )
      {
        addSelectedTableAccessor( srcClass, selectedTable );

        List<QueryColumn> selectedCols = selectedTable.getSecond();
        selectedCols.forEach( c -> columns.remove( c.getName() ) );
      }

      for( ForeignKeyQueryRef fkRef : getQuery().findForeignKeyQueryRefs() )
      {
        addFkFetcher( srcClass, fkRef );
      }

      for( QueryColumn column : columns.values() )
      {
        addQueryGetter( srcClass, column );
      }

      addFlatRowMethod( srcClass );
    }
    enclosingType.addInnerClass( srcClass );
  }

  private void addFlatRowMethod( SrcLinkedClass srcClass )
  {
    SrcType type = new SrcType( "FlatRow" );
    SrcMethod flatRowMethod = new SrcMethod( srcClass )
      .modifiers( Flags.DEFAULT )
      .name( "flatRow" )
      .returns( type )
      .body( "return new FlatRow() {public Bindings getBindings() { return Row.this.getBindings(); }};" );
    srcClass.addMethod( flatRowMethod );
  }

  private void addFkFetcher( SrcLinkedClass srcClass, ForeignKeyQueryRef fkRef )
  {
    SchemaTable table = fkRef.getFk().getReferencedTable();
    String tableFqn = getTableFqn( table );

    SrcType type = new SrcType( tableFqn );
    String name = fkRef.getName();
    String propName = makePascalCaseIdentifier( name, true );
    SrcMethod fkFetchMethod = new SrcMethod( srcClass )
      .name( "fetch" + propName )
      .modifiers( Flags.DEFAULT )
      .returns( type );
    StringBuilder sb = new StringBuilder( "return fetchFk($tableFqn.class, \"${table.getName()}\", " );
    List<QueryColumn> queryCols = fkRef.getQueryCols();
    for( int i = 0; i < queryCols.size(); i++ )
    {
      //noinspection unused
      QueryColumn queryCol = queryCols.get( i );
      if( i > 0 )
      {
        sb.append( ", " );
      }
      //noinspection unused
      String colName = queryCol.getName();
      sb.append( "new Pair<>(\"$colName\", getBindings().get(\"$colName\"))" );
    }
    sb.append( ");" );
    fkFetchMethod.body( sb.toString() );
    addActualNameAnnotation( fkFetchMethod, name, true );
    srcClass.addMethod( fkFetchMethod );
  }

  private void addSelectedTableAccessor( SrcLinkedClass srcClass, Pair<SchemaTable, List<QueryColumn>> selectedTable )
  {
    SchemaTable table = selectedTable.getFirst();
    String tableName = getTableSimpleTypeName( table );
    String tableFqn = getTableFqn( table );

    String propName = tableName;
    boolean tableNameMatchesColumnNameOfRemainingColumns = getQuery().getColumns().values().stream()
      .filter( c -> !selectedTable.getSecond().contains( c ) )
      .map( c -> makePascalCaseIdentifier( c.getName(), true ) )
      .anyMatch( name -> name.equalsIgnoreCase( tableName ) );
    if( tableNameMatchesColumnNameOfRemainingColumns )
    {
      // disambiguate selected table obj and query column
      propName += "Ref";
    }
    SrcType type = new SrcType( tableFqn );
    SrcGetProperty getter = new SrcGetProperty( propName, type )
      .modifiers( Flags.DEFAULT )
      .body( "return new $tableFqn() {@Override public Bindings getBindings() {return Row.this.getBindings(); }};" );
    srcClass.addGetProperty( getter ).modifiers( Modifier.PUBLIC );
  }

  private String getTableSimpleTypeName( SchemaTable table )
  {
    return table.getSchema().getJavaTypeName( table.getName() );
  }

  private String getTableFqn( SchemaTable table )
  {
    String schemaPackage = _model.getScope().getDbconfig().getSchemaPackage();
    String configName = table.getSchema().getDbConfig().getName();
    return schemaPackage + "." + configName + "." + getTableSimpleTypeName( table );
  }

  private void addImports( SrcLinkedClass srcClass )
  {
    srcClass.addImport( Query.class );
    srcClass.addImport( ResultRow.class );
    srcClass.addImport( Runner.class );
    srcClass.addImport( Bindings.class );
    srcClass.addImport( DataBindings.class );
    srcClass.addImport( ConcurrentHashMap.class );
    srcClass.addImport( ActualName.class );
    srcClass.addImport( DisableStringLiteralTemplates.class );
    srcClass.addImport( FragmentValue.class );
    importSchemaTypes( srcClass );
  }

  private void importSchemaTypes( SrcLinkedClass srcClass )
  {
    DbConfig dbconfig = _model.getScope().getDbconfig();
    srcClass.addStaticImport( dbconfig.getSchemaPackage() + '.' + dbconfig.getName() + ".*" );
  }

  private void addQueryGetter( SrcLinkedClass srcClass, QueryColumn column )
  {
    Class<?> colType = getType( column );
    if( colType == null )
    {
      return;
    }
    SrcType type = new SrcType( colType );
    StringBuilder retType = new StringBuilder();
    type.render( retType, 0, false ); // calling render to include array "[]"

    String name = column.getName();
    String propName = makePascalCaseIdentifier( column.getName(), true );
    SrcGetProperty getter = new SrcGetProperty( propName, type )
      .modifiers( Flags.DEFAULT )
      .body( "return ($retType)getBindings().get(\"$name\");" );
    addActualNameAnnotation( getter, name, true );
    srcClass.addGetProperty( getter ).modifiers( Modifier.PUBLIC );
  }

  private java.lang.Class<?> getType( DataElement elem )
  {
    java.lang.Class<?> colType = elem.getType();
    if( colType == null )
    {
      //noinspection unused
      String label = elem instanceof QueryColumn ? "column" : "parameter";
      _model.addIssue( IIssue.Kind.Error,
        "$label type unknown for query '${getQueryName()}', $label '${elem.getName()}', jdbcType '${elem.getJdbcType()}'" );
      return null;
    }
    return colType;
  }
}
