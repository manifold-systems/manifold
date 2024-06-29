/*
 * Copyright (c) 2023 - Manifold Systems LLC
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
import manifold.json.rt.api.DataBindings;
import manifold.rt.api.ActualName;
import manifold.rt.api.DisableStringLiteralTemplates;
import manifold.rt.api.FragmentValue;
import manifold.rt.api.util.ManClassUtil;
import manifold.rt.api.util.ManEscapeUtil;
import manifold.sql.api.DataElement;
import manifold.sql.api.Parameter;
import manifold.sql.query.api.Command;
import manifold.sql.rt.api.*;
import manifold.sql.rt.api.TxScope.BatchSqlChangeCtx;
import manifold.sql.rt.api.TxScope.SqlChangeCtx;

import javax.tools.DiagnosticListener;
import javax.tools.JavaFileManager;
import javax.tools.JavaFileObject;
import java.lang.reflect.Modifier;
import java.sql.SQLException;
import java.util.List;

import static manifold.api.gen.AbstractSrcClass.Kind.Interface;
import static manifold.api.gen.SrcLinkedClass.addActualNameAnnotation;
import static manifold.api.gen.SrcLinkedClass.makeIdentifier;

/**
 * The top-level class defining an .sql file having a non-Select statement.
 */
class CommandParentType extends SqlParentType
{
  CommandParentType( SqlModel model )
  {
    super( model );
  }

  void render( StringBuilder sb, JavaFileManager.Location location, IModule module, DiagnosticListener<JavaFileObject> errorHandler )
  {
    String name = getCommandName();
    //noinspection unused
    String identifier = makeIdentifier( name, false );
    SrcLinkedClass srcClass = new SrcLinkedClass( getFqn(), Interface, _model.getFile(), location, module, errorHandler )
      .addAnnotation( new SrcAnnotationExpression( DisableStringLiteralTemplates.class.getSimpleName() ) )
      .addInterface( new SrcType( "SqlCommand" ) )
      .modifiers( Modifier.PUBLIC );
    addActualNameAnnotation( srcClass, name, false );
    addImports( srcClass );
    addExecuteMethods( srcClass );
    addFragmentValueMethod( srcClass );

    srcClass.render( sb, 0 );
  }

  private Command getCommand()
  {
    return (Command)_model.getSqlStatement();
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

  private String getCommandName()
  {
    if( getCommand() == null )
    {
      return ManClassUtil.getShortClassName( getFqn() );
    }

    String name = getCommand().getName();
    return (name == null || name.isEmpty()) ? ANONYMOUS_TYPE + _anonCount++ : name;
  }

  private void addExecuteMethods( SrcLinkedClass srcClass )
  {
    addExecuteMethod( srcClass, "execute", int.class );
  }
  @SuppressWarnings( "SameParameterValue" )
  private void addExecuteMethod( SrcLinkedClass srcClass, String methodName, Class<?> returnType )
  {
    SrcMethod method = new SrcMethod( srcClass )
      .name( methodName )
      .modifiers( isFragment() ? Flags.DEFAULT : Modifier.STATIC )
      .addParam( "ctx", SqlChangeCtx.class.getSimpleName() )
      .throwsList( SQLException.class )
      .returns( new SrcType( returnType ) );
    addParameters( method );
    StringBuilder sb = new StringBuilder();
    sb.append( "DataBindings paramBindings = new DataBindings();\n" );
    int i = 0;
    for( SrcParameter param : method.getParameters() )
    {
      if( i++ == 0 )
      {
        // skip ctx param
        continue;
      }
      //noinspection unused
      String paramName = makeIdentifier( param.getSimpleName(), false );
      sb.append( "    paramBindings.put(\"$paramName\", $paramName);\n" );
    }

    String command = getCommand() == null ? "<errant sql command>" : getCommand().getSqlSource();
    //noinspection UnusedAssignment
    command = ManEscapeUtil.escapeForJavaStringLiteral( command );
    //noinspection unused
    String simpleName = srcClass.getSimpleName();
    if( i == 1 )
    {
      sb.append( "    if(ctx instanceof BatchSqlChangeCtx) ((BatchSqlChangeCtx)ctx).setBatchId(\"_stmt_\");\n" );
    }
    else
    {
      sb.append( "    if(ctx instanceof BatchSqlChangeCtx) ((BatchSqlChangeCtx)ctx).setBatchId(\"${srcClass.getName()}\");\n" );
    }
    sb.append( "    return new Executor(ctx, ${getParameterInfo()}, paramBindings, \"$command\").$methodName();\n" );
    method.body( sb.toString() );
    srcClass.addMethod( method );
  }

  private boolean isFragment()
  {
    return _model.getFile() instanceof IFileFragment &&
      isValueFragment( ((IFileFragment)_model.getFile()).getHostKind() );
  }

  @SuppressWarnings( "unused" )
  private String getParameterInfo()
  {
    StringBuilder sb = new StringBuilder( "new ColumnInfo[]{");
    List<Parameter> parameters = getCommand().getParameters();
    for( int i = 0; i < parameters.size(); i++ )
    {
      Parameter p = parameters.get( i );
      if( i > 0 )
      {
        sb.append( ", " );
      }
      sb.append( "new ColumnInfo(\"${p.getName()}\", ${p.getJdbcType()}, \"{p.getSqlType()}\", ${p.getSize()})" );
    }
    return sb.append( "}" ).toString();
  }

  @SuppressWarnings( "unused" )
  private String getSqlParamTypes()
  {
    StringBuilder sb = new StringBuilder( "new String[]{");
    List<Parameter> parameters = getCommand().getParameters();
    for( int i = 0; i < parameters.size(); i++ )
    {
      Parameter p = parameters.get( i );
      if( i > 0 )
      {
        sb.append( "," );
      }
      sb.append( "\"" ).append( p.getSqlType() ).append( "\"" );
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

  private void addParameters( AbstractSrcMethod method )
  {
    if( getCommand() == null )
    {
      // handle errant query
      return;
    }

    for( Parameter param: getCommand().getParameters() )
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

  private java.lang.Class<?> getType( DataElement elem )
  {
    java.lang.Class<?> colType = elem.getType();
    if( colType == null )
    {
      //noinspection unused
      _model.addIssue( IIssue.Kind.Error, 0,
        "parameter type unknown for command '${getCommandName()}', parameter '${elem.getName()}', jdbcType '${elem.getJdbcType()}'" );
      return null;
    }
    return colType;
  }

  private void addImports( SrcLinkedClass srcClass )
  {
    srcClass.addImport( SqlCommand.class );
    srcClass.addImport( DataBindings.class );
    srcClass.addImport( ColumnInfo.class );
    srcClass.addImport( Executor.class );
    srcClass.addImport( OperableTxScope.class );
    srcClass.addImport( SqlChangeCtx.class );
    srcClass.addImport( BatchSqlChangeCtx.class );
    srcClass.addImport( ActualName.class );
    srcClass.addImport( DisableStringLiteralTemplates.class );
    srcClass.addImport( FragmentValue.class );
    importSchemaType( srcClass );
  }

  private void importSchemaType( SrcLinkedClass srcClass )
  {
    DbConfig dbconfig = _model.getScope().getDbconfig();
    srcClass.addImport( dbconfig.getSchemaPackage() + '.' + dbconfig.getName() );
  }
}
