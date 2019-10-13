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

package manifold.science.measures;

import manifold.science.api.AbstractMeasure;
import manifold.science.util.Rational;

public final class StorageCapacity extends AbstractMeasure<StorageCapacityUnit, StorageCapacity>
{
  public StorageCapacity( Rational value, StorageCapacityUnit unit, StorageCapacityUnit displayUnit )
  {
    super( value, unit, displayUnit );
  }

  public StorageCapacity( Rational value, StorageCapacityUnit unit )
  {
    this( value, unit, unit );
  }

  @Override
  public StorageCapacityUnit getBaseUnit()
  {
    return StorageCapacityUnit.BASE;
  }

  @Override
  public StorageCapacity make( Rational value, StorageCapacityUnit unit, StorageCapacityUnit displayUnit )
  {
    return new StorageCapacity( value, unit, displayUnit );
  }

  @Override
  public StorageCapacity make( Rational value, StorageCapacityUnit unit )
  {
    return new StorageCapacity( value, unit );
  }
}
