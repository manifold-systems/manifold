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

package manifold.ext.delegation.parts.superinterfacecall;

import junit.framework.TestCase;

public class SuperInterfaceCallTest extends TestCase
{
  public void testSuperInterfaceCall_Delegation()
  {
    MyA_APart myA = new MyA_APart();
    assertEquals( "MyAhi 1 2", myA.stuff( "1", "2" ) );
  }
  public void testSuperInterfaceCall_Forwarding()
  {
    MyA_ANotPart myA = new MyA_ANotPart();
    assertEquals( "Ahi 1 2", myA.stuff( "1", "2" ) );
  }
  public void testSuperInterfaceCall_Delegation_Void()
  {
    MyA_APart myA = new MyA_APart();
    StringBuilder sb = new StringBuilder();
    myA.stuffVoid( "1", "2", sb );
    assertEquals( "MyAhi 1 2", sb.toString() );
  }
  public void testSuperInterfaceCall_Forwarding_Void()
  {
    MyA_ANotPart myA = new MyA_ANotPart();
    StringBuilder sb = new StringBuilder();
    myA.stuffVoid( "1", "2", sb );
    assertEquals( "Ahi 1 2", sb.toString() );
  }
}
