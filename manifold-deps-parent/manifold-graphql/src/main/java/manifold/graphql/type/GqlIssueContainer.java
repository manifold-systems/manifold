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

import graphql.GraphQLError;
import graphql.language.SourceLocation;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import manifold.api.fs.IFile;
import manifold.api.fs.IFileUtil;
import manifold.internal.javac.IIssue;
import manifold.internal.javac.IIssueContainer;

public class GqlIssueContainer implements IIssueContainer
{
  private final IFile _file;
  private final List<IIssue> _issues;

  /**
   *
   */
  @SuppressWarnings("WeakerAccess")
  public GqlIssueContainer( List<GraphQLError> errors, IFile file )
  {
    _issues = new ArrayList<>();
    _file = file;

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
    return Collections.emptyList();
  }

  @Override
  public List<IIssue> getErrors()
  {
    return getIssues();
  }

  @SuppressWarnings("WeakerAccess")
  public void addIssues( List<GraphQLError> errors )
  {
    for( GraphQLError e: errors )
    {
      Optional<SourceLocation> loc = e.getLocations().stream().findFirst();
      int line = 0;
      int column = 0;
      int offset = 0;
      if( loc.isPresent() )
      {
        SourceLocation sourceLocation = loc.get();
        line = sourceLocation.getLine();
        column = sourceLocation.getColumn();
        offset = IFileUtil.findOffset( _file, line, column );
      }
      _issues.add( new GqlIssue( IIssue.Kind.Error, offset, line, column, e.getMessage() ) );
    }
  }

  @Override
  public boolean isEmpty()
  {
    return _issues == null;
  }
}