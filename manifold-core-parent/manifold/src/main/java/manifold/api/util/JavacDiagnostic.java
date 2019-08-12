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

package manifold.api.util;

import java.util.Locale;
import javax.tools.Diagnostic;
import javax.tools.JavaFileObject;

/**
 */
public class JavacDiagnostic implements Diagnostic<JavaFileObject>
{
  private final JavaFileObject _file;
  private final Kind _kind;
  private final long _offset;
  private final long _line;
  private final long _column;
  private final String _message;

  public JavacDiagnostic( JavaFileObject file, Kind kind, long offset, long line, long column, String message )
  {
    _file = file;
    _kind = kind;
    _offset = offset;
    _line = line;
    _column = column;
    _message = message;
  }

  @Override
  public Kind getKind()
  {
    return _kind;
  }

  @Override
  public JavaFileObject getSource()
  {
    return _file;
  }

  @Override
  public long getPosition()
  {
    return _offset;
  }

  @Override
  public long getStartPosition()
  {
    return _offset;
  }

  @Override
  public long getEndPosition()
  {
    return _offset;
  }

  @Override
  public long getLineNumber()
  {
    return _line;
  }

  @Override
  public long getColumnNumber()
  {
    return _column;
  }

  @Override
  public String getCode()
  {
    return null;
  }

  @Override
  public String getMessage( Locale locale )
  {
    return _message;
  }
}
