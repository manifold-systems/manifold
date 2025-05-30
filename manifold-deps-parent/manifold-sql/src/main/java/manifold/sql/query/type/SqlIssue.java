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

import manifold.internal.javac.IIssue;

public class SqlIssue extends RuntimeException implements IIssue
{
  private final Kind _kind;
  private final String _msg;
  private final int _offset;

  SqlIssue( Kind kind, int offset, String msg )
  {
    super( msg );
    _kind = kind;
    _offset = offset;
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
    return 0;
  }

  @Override
  public int getLine()
  {
    return 0;
  }

  @Override
  public int getColumn()
  {
    return 0;
  }

  @Override
  public String getMessage()
  {
    return _msg;
  }
}