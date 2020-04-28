/*
 * Copyright (c) 2020 - Manifold Systems LLC
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

package manifold.api.json;

import manifold.api.csv.Csv;
import manifold.ext.api.This;
import manifold.json.extensions.java.net.URL.ManUrlExt;

import java.io.IOException;
import java.net.*;
import java.util.Map;

/**
 * Represents a URL endpoint with an optional proxy (HTTP, SOCKS, or direct if no proxy is specified).
 * Abstracts HTTP request operations involving common formats such as JSON, XML, YAML, CSV, and plain text.
 * Typically, the methods of the {@code Endpoint} class are called indirectly through higher level APIs.
 */
public class Endpoint
{
  private final String _urlBase;
  private final Proxy _proxy;

  /**
   * @param urlBase The base URL for the endpoint. Can have name=value arguments.
   */
  public Endpoint( String urlBase )
  {
    _urlBase = urlBase;
    _proxy = Proxy.NO_PROXY;
  }

  /**
   * Makes an {@code HTTP} proxied endpoint.
   *
   * @param urlBase The base URL for the endpoint. Can have name=value arguments.
   * @param proxyAddr The address of the proxy server, such as "http://my.proxyserver.com".
   * @param proxyPort The port number of the proxy server, typically 8080.
   */
  public Endpoint( String urlBase, String proxyAddr, int proxyPort )
  {
    this( urlBase, proxyAddr, proxyPort, Proxy.Type.HTTP );
  }
  /**
   * Makes an proxied endpoint on port {@code 8080}.
   *
   * @param urlBase The base URL for the endpoint. Can have name=value arguments.
   * @param proxyAddr The address of the proxy server, such as "http://my.proxyserver.com".
   * @param proxyType The proxy type: {@code HTTP or SOCKS}. Use {@link #Endpoint(String)} if there is no
   *                  proxy server.
   */
  public Endpoint( String urlBase, String proxyAddr, Proxy.Type proxyType )
  {
    this( urlBase, proxyAddr, 8080, proxyType );
  }

  /**
   * Makes a proxied endpoint at the specified {@code urlBase}.
   *
   * @param urlBase The base URL for the endpoint. Can have name=value arguments.
   * @param proxyAddr The address of the proxy server, such as "http://my.proxyserver.com".
   * @param proxyPort The port number of the proxy server, typically 8080.
   * @param proxyType The proxy type: {@code HTTP or SOCKS}. Use {@link #Endpoint(String)} if there is no
   *                  proxy server.
   */
  public Endpoint( String urlBase, String proxyAddr, int proxyPort, Proxy.Type proxyType )
  {
    _urlBase = urlBase;
    _proxy = new Proxy( proxyType, new InetSocketAddress( proxyAddr, proxyPort ) );
  }

  private Endpoint( String urlBase, Proxy proxy )
  {
    _urlBase = urlBase;
    _proxy = proxy;
  }

  /**
   * Creates a new {@code Endpoint} with a base URL consisting of this endpoint's base URL + {@code urlSuffix}.
   *
   * @param urlSuffix Use the suffix to make a new url consisting of this endpoint's base URL + {@code urlSuffix}.
   * @return A new {@code Endpoint} with a base URL consisting of this endpoint's base URL + {@code urlSuffix}.
   */
  public Endpoint withUrlSuffix( String urlSuffix )
  {
    return new Endpoint( _urlBase + urlSuffix, _proxy );
  }

  /**
   * Opens a connection on this endpoint's URL and Proxy.
   *
   * @see URL#openConnection(Proxy)
   */
  public URLConnection openConnection() throws IOException
  {
    return new URL( _urlBase ).openConnection( _proxy );
  }

  /**
   * Use HTTP GET, POST, PUT, or PATCH to send JSON bindings to the endpoint with a CSV response.
   *
   * @param httpMethod The HTTP method to use: "GET", "POST", "PUT", or "PATCH"
   * @param jsonValue A JSON value to send (primitive/boxed type, String, List of JSON values, or Bindings of String/JSON value)
   *
   * @return A JSON bindings value parsed from the CSV response.
   */
  public Object sendCsvRequest( String httpMethod, Object jsonValue, Map<String, String> headers, int timeout )
  {
    try
    {
      return ManUrlExt.sendCsvRequest( new URL( _urlBase ), _proxy, httpMethod, jsonValue, headers, timeout );
    }
    catch( MalformedURLException e )
    {
      throw new RuntimeException( e );
    }
  }

  /**
   * Use HTTP GET, POST, PUT, or PATCH to send JSON bindings to the endpoint with a JSON response.
   *
   * @param httpMethod The HTTP method to use: "GET", "POST", "PUT", or "PATCH"
   * @param jsonValue A JSON value to send (primitive/boxed type, String, List of JSON values, or Bindings of String/JSON value)
   *
   * @return A JSON value parsed from the JSON response.
   */
  public Object sendJsonRequest( String httpMethod, Object jsonValue, Map<String, String> headers, int timeout )
  {
    try
    {
      return ManUrlExt.sendJsonRequest( new URL( _urlBase ), _proxy, httpMethod, jsonValue, headers, timeout );
    }
    catch( MalformedURLException e )
    {
      throw new RuntimeException( e );
    }
  }

  /**
   * Use HTTP GET, POST, PUT, or PATCH to send JSON bindings to a URL with a plain text response.
   *
   * @param httpMethod The HTTP method to use: "GET", "POST", "PUT", or "PATCH"
   * @param jsonValue A JSON value to send (primitive/boxed type, String, List of JSON values, or Bindings of String/JSON value)
   *
   * @return The raw response body as plain text
   */
  public Object sendPlainTextRequest( String httpMethod, Object jsonValue, Map<String, String> headers, int timeout )
  {
    try
    {
      return ManUrlExt.sendPlainTextRequest( new URL( _urlBase ), _proxy, httpMethod, jsonValue, headers, timeout );
    }
    catch( MalformedURLException e )
    {
      throw new RuntimeException( e );
    }
  }

  /**
   * Use HTTP GET, POST, PUT, or PATCH to send JSON bindings to a URL with an XML response.
   *
   * @param httpMethod The HTTP method to use: "GET", "POST", "PUT", or "PATCH"
   * @param jsonValue A JSON value to send (primitive/boxed type, String, List of JSON values, or Bindings of String/JSON value)
   *
   * @return The raw response body as XML
   */
  public Object sendXmlRequest( String httpMethod, Object jsonValue, Map<String, String> headers, int timeout )
  {
    try
    {
      return ManUrlExt.sendXmlRequest( new URL( _urlBase ), _proxy, httpMethod, jsonValue, headers, timeout );
    }
    catch( MalformedURLException e )
    {
      throw new RuntimeException( e );
    }
  }

  /**
   * Use HTTP GET, POST, PUT, or PATCH to send JSON bindings to a URL with a YAML response.
   *
   * @param httpMethod The HTTP method to use: "GET", "POST", "PUT", or "PATCH"
   * @param jsonValue A JSON value to send (primitive/boxed type, String, List of JSON values, or Bindings of String/JSON value)
   *
   * @return The raw response body as YAML
   */
  public Object sendYamlRequest( String httpMethod, Object jsonValue, Map<String, String> headers, int timeout )
  {
    try
    {
      return ManUrlExt.sendYamlRequest( new URL( _urlBase ), _proxy, httpMethod, jsonValue, headers, timeout );
    }
    catch( MalformedURLException e )
    {
      throw new RuntimeException( e );
    }
  }
}
