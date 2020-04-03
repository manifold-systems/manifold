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

package manifold.api.gen;

import javax.tools.DiagnosticListener;
import javax.tools.JavaFileManager;
import javax.tools.JavaFileObject;
import manifold.api.host.IModule;

/**
 *
 */
public class SrcClass extends AbstractSrcClass<SrcClass>
{
  public SrcClass( String fqn, Kind kind )
  {
    super( fqn, kind );
  }

  public SrcClass( String fqn, AbstractSrcClass enclosingClass, Kind kind )
  {
    super( fqn, enclosingClass, kind );
  }


  public SrcClass( String fqn, Kind kind, JavaFileManager.Location location, IModule module, DiagnosticListener<JavaFileObject> errorHandler )
  {
    super( fqn, null, kind, location, module, errorHandler );
  }
}
