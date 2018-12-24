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

package manifold.ext.producer.sample;

import manifold.api.fs.IFile;

class Token
{
  int _pos;
  StringBuilder _value;
  IFile _file;

  Token( int pos, IFile file )
  {
    _value = new StringBuilder();
    _pos = pos;
    _file = file;
  }

  void append( char c )
  {
    _value.append( c );
  }

  public String toString()
  {
    return _value.toString();
  }
}
