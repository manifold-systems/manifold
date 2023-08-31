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

package manifold.sql.util;

import manifold.rt.api.util.ManIdentifierUtil;
import org.junit.Test;

import static manifold.rt.api.util.ManIdentifierUtil.makePascalCaseIdentifier;
import static org.junit.Assert.*;

public class UtilTest
{
  @Test
  public void testPascalCase()
  {
    String ident = makePascalCaseIdentifier( "city_id", false );
    assertEquals( "cityId", ident );
    ident = makePascalCaseIdentifier( "city_id", true );
    assertEquals( "CityId", ident );
    ident = makePascalCaseIdentifier( "_city_id", false );
    assertEquals( "_cityId", ident );
    ident = makePascalCaseIdentifier( "_city_id", true );
    assertEquals( "_CityId", ident );
    ident = makePascalCaseIdentifier( "__city_id", false );
    assertEquals( "__cityId", ident );
    ident = makePascalCaseIdentifier( "__city_id", true );
    assertEquals( "__CityId", ident );
    ident = makePascalCaseIdentifier( "city__id", false );
    assertEquals( "city_Id", ident );
    ident = makePascalCaseIdentifier( "city__id", true );
    assertEquals( "City_Id", ident );
    ident = makePascalCaseIdentifier( "city#id", false );
    assertEquals( "cityId", ident );
    ident = makePascalCaseIdentifier( "city#id", true );
    assertEquals( "CityId", ident );
  }
}
