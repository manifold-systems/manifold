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

import manifold.internal.javac.IIssue;
import manifold.internal.javac.IIssueContainer;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class SqlIssueContainer implements IIssueContainer
{
  private final List<IIssue> _issues;
  private final String _productName;

  @SuppressWarnings("WeakerAccess")
  public SqlIssueContainer( String productName, List<Exception> errors )
  {
    _productName = productName;
    _issues = new ArrayList<>();
    addIssues( errors );
  }

  @Override
  public List<IIssue> getIssues()
  {
    return _issues;
  }

  @Override
  public List<IIssue> getWarnings()
  {
    return getIssues().stream()
      .filter( issue -> issue.getKind() == IIssue.Kind.Warning )
      .collect( Collectors.toList() );
  }

  @Override
  public List<IIssue> getErrors()
  {
    return getIssues().stream()
      .filter( issue -> issue.getKind() == IIssue.Kind.Error )
      .collect( Collectors.toList() );
  }

  public void addIssue( IIssue.Kind kind, int offset, String msg )
  {
    _issues.add( new SqlIssue( kind, offset, msg ) );
  }

  @SuppressWarnings("WeakerAccess")
  public void addIssues( List<Exception> errors )
  {
    for( Exception e: errors )
    {
      int offset = findOffset( e );
      _issues.add( new SqlIssue( IIssue.Kind.Error, offset, e.getMessage() ) );
    }
  }

  private int findOffset( Exception e )
  {
    switch( _productName.toLowerCase() )
    {
      case "h2":
        return findOffset_h2( e );
      default:
        return 0;
    }
  }

  private int findOffset_h2( Exception e )
  {
    if( e instanceof SQLException )
    {
      String msg = e.getMessage();
      int start = msg.indexOf( '"' ) + 1;
      if( start > 0 )
      {
        int marker = msg.indexOf( "[*]" );
        if( marker >= 0 )
        {
          return marker - start;
        }
      }
    }
    return 0;
  }

  @Override
  public boolean isEmpty()
  {
    return _issues == null;
  }
}