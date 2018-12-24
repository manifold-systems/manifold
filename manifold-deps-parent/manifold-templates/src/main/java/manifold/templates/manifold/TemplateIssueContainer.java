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

package manifold.templates.manifold;

import java.util.Collections;
import java.util.List;
import manifold.internal.javac.IIssue;
import manifold.internal.javac.IIssueContainer;

public class TemplateIssueContainer implements IIssueContainer
{
  private final List<TemplateIssue> _issues;

  public TemplateIssueContainer()
  {
    _issues = Collections.emptyList();
  }

  public TemplateIssueContainer( List<TemplateIssue> issues )
  {
    _issues = issues;
  }

  @Override
  public List<IIssue> getIssues()
  {
    return (List)_issues;
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

  @Override
  public boolean isEmpty()
  {
    return _issues.isEmpty();
  }
}
