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

package manifold.sql.rt.api;

import manifold.rt.api.Bindings;

import java.util.Properties;

/**
 * Represents config information for connecting to a database. Maps directly to data obtained from a .dbconfig file, or
 * if the config is provided from a DbConfigProvider SPI implementation, it is obtained directly.
 */
public interface DbConfig
{
  /** (Provided) Name corresponding with config file. MyDatabase.dbconfig -> "MyDatabase" **/
  String getName();

  /** (Provided) Location of dbconfig file corresponding with this class */
  String getPath();

  /** (Optional) Qualified name of Java class serving as a driver, may be null */
  String getDriverClass();

  /** (Required) JDBC URL for database (run time). If {@link #getBuildUrl()} returns null, this URL is used both for runtime and build time. */
  String getUrl();

  /** (Optional) JDBC URL for database (build time). This database may be empty, it is used solely for metadata during compilation. */
  String getBuildUrl();

  /** (Required) The fully qualified package name where schema types will be generated */
  String getSchemaPackage();

  /** (Optional) Username for database account, may be null */
  String getUser();
  /** (Optional) Password for database account, may be null */
  String getPassword();

  /** (Optional) If true, this dbconfig is applied to SQL queries that do not specify a dbconfig name */
  boolean isDefault();

  /** (Optional) JDBC connection properties in JSON format. See {@link #toProperties()}. */
  Bindings getProperties();

  /** Returns the build URL if provided, otherwise the runtime URL */
  default String getBuildUrlOtherwiseRuntimeUrl()
  {
    String buildUrl = getBuildUrl();
    return buildUrl == null || buildUrl.isEmpty() ? getUrl() : buildUrl;
  }

  /** Convert the connection related config attributes to a Properties object */
  default Properties toProperties()
  {
    Properties props = new Properties();
    Bindings properties = getProperties();
    if( properties != null )
    {
      props.putAll( properties );
    }

    // override direct user, password if provided

    String user = getUser();
    if( user != null )
    {
      props.put( "user", user );
    }

    String password = getPassword();
    if( password != null )
    {
      props.put( "password", password );
    }

    return props;
  }
}
