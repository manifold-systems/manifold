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

import manifold.internal.javac.IIssue;

public class TemplateIssue implements IIssue
{
  private Kind _kind;
  private final int _offset;
  private int _line;
  private int _column;
  private String _msg;

  public TemplateIssue( Kind kind, int offset, int line, int column, String msg )
  {
    _kind = kind;
    _offset = offset;
    _line = line;
    _column = column;
    _msg = msg;
  }

  @Override
  public Kind getKind()
  {
    return _kind;
  }

  @Override
  public int getStartOffset()
  {
    return _offset;
  }

  @Override
  public int getEndOffset()
  {
    return _offset;
  }

  @Override
  public int getLine()
  {
    return _line;
  }

  @Override
  public int getColumn()
  {
    return _column;
  }

  @Override
  public String getMessage()
  {
    return _msg;
  }
}
