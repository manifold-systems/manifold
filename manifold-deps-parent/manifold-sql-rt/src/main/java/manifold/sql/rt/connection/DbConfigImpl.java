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

package manifold.sql.rt.connection;

import manifold.rt.api.Bindings;
import manifold.sql.rt.api.DbConfig;

public class DbConfigImpl implements DbConfig
{
  private final Bindings _bindings;

  public DbConfigImpl( Bindings bindings )
  {
    _bindings = bindings;
  }

  @Override
  public String getName()
  {
    return (String)_bindings.get( "name" );
  }

  @Override
  public String getPath()
  {
    return (String)_bindings.get( "path" );
  }

  @Override
  public String getDriverClass()
  {
    return (String)_bindings.get( "driverClass" );
  }

  @Override
  public String getUrl()
  {
    return (String)_bindings.get( "url" );
  }

  @Override
  public String getBuildUrl()
  {
    return (String)_bindings.get( "buildUrl" );
  }

  @Override
  public String getUser()
  {
    return (String)_bindings.get( "user" );
  }

  @Override
  public String getPassword()
  {
    return (String)_bindings.get( "password" );
  }

  @Override
  public boolean isDefault()
  {
    Boolean isDefault = (Boolean)_bindings.get( "isDefault" );
    return isDefault != null && isDefault;
  }

  @Override
  public String getSchemaPackage()
  {
    return (String)_bindings.get( "schemaPackage" );
  }

  @Override
  public Bindings getProperties()
  {
    return (Bindings)_bindings.get( "properties" );
  }

  @Override
  public boolean equals( Object o )
  {
    if( this == o ) return true;
    if( !(o instanceof DbConfigImpl) ) return false;
    DbConfigImpl dbConfig = (DbConfigImpl)o;
    return _bindings.equals( dbConfig._bindings );
  }

  @Override
  public int hashCode()
  {
    return _bindings.hashCode();
  }
}
