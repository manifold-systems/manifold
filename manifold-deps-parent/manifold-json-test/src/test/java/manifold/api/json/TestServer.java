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

import spark.Spark;
import static spark.Spark.*;

import abc.ResponseError;

public class TestServer {
  public static void main(String[] args) {
    port(4567);

    // return the Query String of the GET request
    get("/testGet_QueryString", (req, res) -> req.raw().getQueryString() );

    // return the Query String of the POST request
    post("/testPost_QueryString", (req, res) -> req.raw().getQueryString() );

    // Error response for IllegalStateException
    exception(IllegalArgumentException.class, (e, req, res) -> {
      res.status(400);
      res.body(ResponseError.create(e.getMessage()).write().toJson()); // <~~~ The ResponseError.json file!
    });

    after((req, res) -> res.type("application/json"));
  }

  public static void stop() {
    Spark.stop();
  }
}