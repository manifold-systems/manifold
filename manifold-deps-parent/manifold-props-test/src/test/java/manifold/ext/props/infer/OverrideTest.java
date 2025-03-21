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

package manifold.ext.props.infer;

import org.junit.Test;

import static junit.framework.TestCase.assertEquals;

public class OverrideTest
{
  @Test
  public void testInferredOverrideAccessesFieldDirectly()
  {
    OverrideClasses.Child child = OverrideClasses.newChild();
    assertEquals( "Child", child.baz ); // baz field is not accessible here bc it's private, so the inferred property is used here, calls getBaz()
    assertEquals( "Child", child.getBaz() ); // getBaz() accesses baz field directly bc it is accessible

    assertEquals( "Parent", child.foo ); // foo field is directly accessible here bc it's protected
    assertEquals( "Child", child.getFoo() ); // getFoo() accesses foo field directly bc it is accessible

    assertEquals( "Parent", child.bar ); // bar field is directly accessible here bc it's package-private
    assertEquals( "Child", child.getBar() );

    assertEquals( "Outer", new OverrideClasses().outer );
//    assertEquals( "Enclosing", child.blad );
  }
}