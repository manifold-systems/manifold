package manifold.science;

import manifold.science.api.AbstractMeasure;
import manifold.science.util.Rational;

public final class StorageCapacity extends AbstractMeasure<StorageCapacityUnit, StorageCapacity>
{
  public StorageCapacity( Rational value, StorageCapacityUnit unit, StorageCapacityUnit displayUnit ) {
    super( value, unit, displayUnit, StorageCapacityUnit.BASE );
  }
  public StorageCapacity( Rational value, StorageCapacityUnit unit ) {
    this( value, unit, unit );
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
