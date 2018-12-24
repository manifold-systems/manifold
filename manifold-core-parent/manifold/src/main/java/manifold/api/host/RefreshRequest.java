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

package manifold.api.host;


import manifold.api.fs.IFile;

public class RefreshRequest
{
  public final IFile file;
  public final IModule module;
  public final RefreshKind kind;
  public final String[] types;

  public RefreshRequest( IFile file, String[] types, IModule module, RefreshKind kind )
  {
    this.file = file;
    this.kind = kind;
    this.types = types;
    this.module = module;
  }

  public RefreshRequest( String[] allTypes, RefreshRequest request, IModule module )
  {
    this( request.file, allTypes, module, request.kind );
  }

  @Override
  public String toString()
  {
    StringBuilder s = new StringBuilder( kind + " with " );
    for( String type : types )
    {
      s.append( type ).append( ", " );
    }
    s.append( "module: " ).append( module );
    return s.toString();
  }
}
