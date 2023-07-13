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
import manifold.rt.api.util.Pair;
import manifold.sql.rt.api.SchemaBuilder;
import manifold.sql.rt.api.ResultTable;
import manifold.sql.schema.api.*;

import javax.tools.DiagnosticListener;
import javax.tools.JavaFileManager;
import javax.tools.JavaFileObject;
import java.lang.reflect.Modifier;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

import static manifold.api.gen.AbstractSrcClass.Kind.Class;
import static manifold.api.gen.AbstractSrcClass.Kind.Interface;
import static manifold.api.gen.SrcLinkedClass.addActualNameAnnotation;
import static manifold.rt.api.util.ManIdentifierUtil.makeIdentifier;
import static manifold.rt.api.util.ManIdentifierUtil.makePascalCaseIdentifier;

/**
 * The top-level class enclosing all the types defined in a single ".dbconfig" file.
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
      .modifiers( Modifier.PUBLIC );
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
    sb.append( "        DataBindings _bindings = new DataBindings(new ConcurrentHashMap());\n" );
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
    sb.append( "        }\n" );

    sb.append( "        @Override public Bindings getBindings() { return _bindings; }\n" );

    sb.append( "      };" );
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

  private void addBuilder( SrcLinkedClass enclosingType, SchemaTable table )
  {
    String fqn = enclosingType.getName() + ".Builder";
    SrcLinkedClass srcInterface = new SrcLinkedClass( fqn, enclosingType, Interface )
      .addInterface( new SrcType( SchemaBuilder.class.getSimpleName() ).addTypeParam( enclosingType.getSimpleName() ) );
    enclosingType.addInnerClass( srcInterface );
    addWithMethods( srcInterface, table );
    addBuildMethod( enclosingType, srcInterface );
  }

  private void addBuildMethod( SrcLinkedClass enclosingType, SrcLinkedClass srcInterface )
  {
    String tableName = enclosingType.getSimpleName();
    SrcMethod method = new SrcMethod( srcInterface )
      .modifiers( Flags.DEFAULT )
      .name( "build" )
      .returns( new SrcType( tableName ) );
    srcInterface.addMethod( method );

    method.body(
      "return new $tableName() { @Override public Bindings getBindings() { return Builder.this.getBindings(); } };" );
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
    return !col.isNullable() && !col.isGenerated() && col.getDefaultValue() == null;
  }

  private void addImports( SrcLinkedClass srcClass )
  {
    srcClass.addImport( Pair.class );
    srcClass.addImport( Bindings.class );
    srcClass.addImport( DataBindings.class );
    srcClass.addImport( ResultTable.class );
    srcClass.addImport( SchemaBuilder.class );
    srcClass.addImport( ConcurrentHashMap.class );
    srcClass.addImport( ActualName.class );
    srcClass.addImport( DisableStringLiteralTemplates.class );
  }

  private void addInnerObjectType( SchemaTable table, SrcLinkedClass enclosingType )
  {
    String identifier = getSchema().getJavaTypeName( table.getName() );
    String fqn = getFqn() + '.' + identifier;
    SrcLinkedClass srcClass = new SrcLinkedClass( fqn, enclosingType, Interface )
      .addInterface( ResultTable.class.getSimpleName() )
      .modifiers( Modifier.PUBLIC );
    addActualNameAnnotation( srcClass, table.getName(), false );
    addBuilder( srcClass, table );
    addBuilderMethod( srcClass, table );

    for( SchemaColumn member: table.getColumns().values() )
    {
      addMember( srcClass, member );
    }

    for( List<SchemaForeignKey> fkEntry : table.getForeignKeys().values() )
    {
      for( SchemaForeignKey fk : fkEntry )
      {
        addFkFetcher( srcClass, fk );
      }
    }

    //## todo
//    addLoaderMethodsForForiegnKeys();
    enclosingType.addInnerClass( srcClass );
  }

  private void addMember( SrcLinkedClass srcInterface, SchemaColumn member )
  {
    Class<?> type = member.getType();
    addMember( srcInterface, member, type, member.getName() );
  }

  private void addMember( SrcLinkedClass srcInterface, SchemaColumn member, Class<?> type, String name )
  {
    SrcType getterType = makeSrcType( srcInterface, type, false );
//    SrcType setterType = makeSrcType( srcInterface, type, false, true );
    String propName = makePascalCaseIdentifier( name, true );
    //noinspection unused
    String colName = makeIdentifier( member.getName() );
//    //noinspection unused
//    StringBuilder propertyType = getterType.render( new StringBuilder(), 0, false );
//    //noinspection unused
//    StringBuilder componentType = getComponentType( getterType ).render( new StringBuilder(), 0, false );
    SrcGetProperty getter = new SrcGetProperty( propName, getterType );
    getter.modifiers( Flags.DEFAULT );
    if( type == String.class )
    {
      // handling string specially because retarded SQLite table metadata maps types like TIMESTAMP to VARCHAR,
      // but query metadata is correctly typed as TIMESTAMP, must coerce to string here :(
      getter.body( "Object value = getBindings().get(\"$colName\");" +
                   "return value == null ? null : String.valueOf(value);" );
    }
    else
    {
      StringBuilder retType = new StringBuilder();
      getterType.render( retType, 0, false ); // calling render to include array "[]"

      getter.body( "return ($retType)getBindings().get(\"$colName\");" );
    }
    addActualNameAnnotation( getter, name, true );
    srcInterface.addGetProperty( getter ).modifiers( Modifier.PUBLIC );

//    SrcSetProperty setter = new SrcSetProperty( propName, setterType );
//    addActualNameAnnotation( setter, name, true );
////    if( member != null )
////    {
////      addSourcePositionAnnotation( srcClass, member, name, setter );
////    }
//    srcInterface.addSetProperty( setter ).modifiers( Modifier.PUBLIC );
  }

  private void addFkFetcher( SrcLinkedClass srcClass, SchemaForeignKey fk )
  {
    SchemaTable table = fk.getReferencedTable();
    String tableFqn = getTableFqn( table );

    SrcType type = new SrcType( tableFqn );
    String name = fk.getName();
    String propName = makePascalCaseIdentifier( name, true );
    SrcMethod fkFetchMethod = new SrcMethod( srcClass )
      .name( "fetch" + propName )
      .modifiers( Flags.DEFAULT )
      .returns( type );
    StringBuilder sb = new StringBuilder( "return fetchFk($tableFqn.class, \"${table.getName()}\", " );
    List<SchemaColumn> columns = fk.getColumns();
    for( int i = 0; i < columns.size(); i++ )
    {
      //noinspection unused
      SchemaColumn column = columns.get( i );
      if( i > 0 )
      {
        sb.append( ", " );
      }
      //noinspection unused
      String colName = column.getName();
      sb.append( "new Pair<>(\"$colName\", getBindings().get(\"$colName\"))" );
    }
    sb.append( ");" );
    fkFetchMethod.body( sb.toString() );
    addActualNameAnnotation( fkFetchMethod, name, true );
    srcClass.addMethod( fkFetchMethod );
  }

  private String getTableFqn( SchemaTable table )
  {
    String schemaPackage = _model.getDbConfig().getSchemaPackage();
    String configName = table.getSchema().getDbConfig().getName();
    return schemaPackage + "." + configName + "." + getTableSimpleTypeName( table );
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
