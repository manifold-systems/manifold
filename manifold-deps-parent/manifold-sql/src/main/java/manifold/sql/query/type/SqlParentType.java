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

import manifold.api.host.IModule;

import javax.tools.DiagnosticListener;
import javax.tools.JavaFileManager;
import javax.tools.JavaFileObject;

abstract class SqlParentType
{
  static final String ANONYMOUS_TYPE = "Anonymous_";

  final SqlModel _model;
  int _anonCount;

  SqlParentType( SqlModel model )
  {
    _model = model;
  }

  String getFqn()
  {
    return _model.getFqn();
  }

  abstract void render( StringBuilder sb, JavaFileManager.Location location, IModule module, DiagnosticListener<JavaFileObject> errorHandler );
}
