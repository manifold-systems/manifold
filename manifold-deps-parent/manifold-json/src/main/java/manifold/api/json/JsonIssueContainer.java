/*
 * Copyright (c) 2018 - Manifold Systems LLC
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

package manifold.api.json;

import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.StringTokenizer;
import javax.script.ScriptException;
import manifold.api.fs.IFile;
import manifold.api.json.schema.IllegalSchemaTypeName;
import manifold.internal.javac.IIssue;
import manifold.internal.javac.IIssueContainer;
import manifold.api.util.StreamUtil;

/**
 */
public class JsonIssueContainer implements IIssueContainer
{
  private final IFile _file;
  private final List<IIssue> _issues;

  @SuppressWarnings("unused")
  public JsonIssueContainer()
  {
    _issues = Collections.emptyList();
    _file = null;
  }

  /**
   * Format of errors reported in ScriptException is:
   * <pre>
   * Found Errors:\n
   * [line:column] first error\n
   * [line:column] second error\n
   * ...
   * </pre>
   */
  @SuppressWarnings("WeakerAccess")
  public JsonIssueContainer( ScriptException cause, IFile file )
  {
    _issues = new ArrayList<>();
    _file = file;

    addIssues( cause );
  }

  @SuppressWarnings("WeakerAccess")
  public JsonIssueContainer( IFile file )
  {
    _issues = new ArrayList<>();
    _file = file;
  }

  private int findOffset( IFile file, int lineNum, int column )
  {
    try
    {
      int offset = 0;
      String content = StreamUtil.getContent( new InputStreamReader( file.openInputStream() ) );
      for( int i = 1; i < lineNum; i++ )
      {
        if( content.length() > offset )
        {
          offset = content.indexOf( '\n', offset ) + 1;
        }
      }
      return offset + column - 1;
    }
    catch( IOException e )
    {
      throw new RuntimeException( e );
    }
  }

  private int parseNum( String line, char delim )
  {
    StringBuilder num = new StringBuilder();
    for( int i = 0; i < line.length(); i++ )
    {
      char c = line.charAt( i );
      if( c != delim )
      {
        num.append( c );
      }
      else
      {
        try
        {
          return Integer.parseInt( num.toString() );
        }
        catch( Exception e )
        {
          return -1;
        }
      }
    }
    return -1;
  }

  @Override
  public List<IIssue> getIssues()
  {
    return _issues;
  }

  @Override
  public List<IIssue> getWarnings()
  {
    return Collections.emptyList();
  }

  @Override
  public List<IIssue> getErrors()
  {
    return getIssues();
  }

  @SuppressWarnings("WeakerAccess")
  public void addIssues( ScriptException cause )
  {
    String message = cause.getMessage();
    for( StringTokenizer tokenizer = new StringTokenizer( message, "\r\n" ); tokenizer.hasMoreTokens(); )
    {
      String line = tokenizer.nextToken();
      if( line.startsWith( "[" ) )
      {
        int lineNum = parseNum( line.substring( 1 ), ':' );
        int column = parseNum( line.substring( line.indexOf( ':' ) + 1 ) + 1, ']' );
        int offset = findOffset( _file, lineNum, column );
        String msg = line.substring( line.indexOf( ']' ) + 1 );
        _issues.add( new JsonIssue( IIssue.Kind.Error, offset, lineNum, column, msg ) );
      }
    }
  }

  @SuppressWarnings("WeakerAccess")
  public void addIssues( IllegalSchemaTypeName cause )
  {
    Token token = cause.getToken();
    int lineNum = token.getLineNumber();
    int column = token.getColumn();
    int offset = token.getOffset();
    String msg = "Unknown schema type: " + cause.getTypeName();
    _issues.add( new JsonIssue( IIssue.Kind.Error, offset, lineNum, column, msg ) );
  }

  @Override
  public boolean isEmpty()
  {
    return _issues == null;
  }
}
