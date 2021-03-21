/*
 * Copyright (c) 2021 - Manifold Systems LLC
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

package manifold.ext.props.multi;

import manifold.util.ReflectUtil;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class MultiTest
{
  @Test
  public void testMulti()
  {
    String type = "hi";
    IBoth both = () -> type;

    assertEquals( String.class, ReflectUtil.method( both, "getType" ).getMethod().getReturnType() );
    assertEquals( "hi", both.getType() );

// should produce compile error: MSG_NASTY_INFERRED_PROPERTY_REF
//    IBoth both = new IBoth() {
//      @Override
//      public String getType()
//      {
//        return type;
//      }
//    };
  }
}
