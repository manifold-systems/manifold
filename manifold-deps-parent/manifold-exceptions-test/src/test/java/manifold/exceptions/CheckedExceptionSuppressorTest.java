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

package manifold.exceptions;

import java.net.URL;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import junit.framework.TestCase;

public class CheckedExceptionSuppressorTest extends TestCase
{
  public void testSimple()
  {
    new URL( "http://example.com/test" );

    List<String> strings = Arrays.asList( "http://example.com", "https://google.com" );
    List<URL> urls = strings.stream()
      .map(URL::new) // Mmm, life is good
      .collect( Collectors.toList());
    urls.forEach(System.out::println);

  }
}
