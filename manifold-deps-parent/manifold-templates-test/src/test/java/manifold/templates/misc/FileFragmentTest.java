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

package manifold.templates.misc;

import org.junit.Test;


import static java.lang.System.out;

public class FileFragmentTest
{
  @Test
  public void testFileFragment()
  {
    /*[MyFragmentTest.html.mtl/]
    <%@ params(String name) %>
    <!DOCTYPE html>
    <html lang="en">
    <head>
      <meta charset="UTF-8">
      <title>Use Template Manifold (ManTL)</title>
    </head>
    <body>
      The letters in <i>${name}</i>:
      <%
        for(int i = 0; i < name.length(); i++) {
      %>
      Letter: <b>${name.charAt(i)}</b>
      <%
        }
      %>
    </body>
    </html>
    */
    out.println(!MyFragmentTest.render("hi").isEmpty());
  }
}