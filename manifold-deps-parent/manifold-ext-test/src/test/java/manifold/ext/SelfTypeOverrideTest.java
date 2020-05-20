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

package manifold.ext;

import junit.framework.TestCase;
import manifold.ext.rt.api.Self;

public class SelfTypeOverrideTest extends TestCase
{
  public void testOverride()
  {
    SinglyNode<String> sn = new SinglyNode<>();
    sn.setNext( sn );

    DoublyNode<String> dn = new DoublyNode<>();
    dn.setNext( new DoublyNode<>() );
  }

  public class SinglyNode<T> {
    private @Self SinglyNode<T> next;

    public @Self SinglyNode<T> getNext() {return next;}

    public void setNext(@Self SinglyNode<T> next) {
      this.next = next;
    }
  }

  public class DoublyNode<T> extends SinglyNode<T> {
    private @Self DoublyNode<T> prev;

    @Override
    public void setNext(@Self SinglyNode<T> next)
    {
      if(next instanceof DoublyNode) {
        super.setNext(next);
        ((DoublyNode)next).prev = this;
      }
      else {
        throw new IllegalArgumentException();
      }
    }
  }
}
