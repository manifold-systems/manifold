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

package manifold.ext.props.middle.auto.gen;

import java.util.Collections;
import java.util.List;

public abstract class GenericBase<T extends CharSequence>
{
  private T tee;

  public GenericBase( T tee )
  {
    this.tee = tee;
  }

  public T getTee()
  {
    return tee;
  }
  public void setTee(T tee )
  {
    this.tee = tee;
  }

  public List<T> getList()
  {
    return Collections.singletonList( tee );
  }
  public void setList( List<T> list )
  {
    tee = list.get( 0 );
  }

  abstract protected GenericBase<T> getMee();
  abstract protected void setMee( GenericBase<T> mee );
}
