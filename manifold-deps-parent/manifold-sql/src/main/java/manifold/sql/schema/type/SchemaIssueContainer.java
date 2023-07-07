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

import manifold.internal.javac.IIssue;
import manifold.internal.javac.IIssueContainer;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class SchemaIssueContainer implements IIssueContainer
{
  private final List<IIssue> _issues;

  @SuppressWarnings("WeakerAccess")
  public SchemaIssueContainer()
  {
    _issues = new ArrayList<>();
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

  public void addIssue( IIssue.Kind kind, String msg )
  {
    _issues.add( new SchemaIssue( kind, msg ) );
  }

  @SuppressWarnings("WeakerAccess")
  public void addIssues( List<Exception> errors )
  {
    for( Exception e: errors )
    {
      _issues.add( new SchemaIssue( IIssue.Kind.Error, e.getMessage() ) );
    }
  }

  @Override
  public boolean isEmpty()
  {
    return _issues.isEmpty();
  }
}