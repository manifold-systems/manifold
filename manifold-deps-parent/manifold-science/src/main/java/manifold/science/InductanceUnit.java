package manifold.science;

import manifold.science.api.AbstractProductUnit;
import manifold.science.api.UnitCache;
import manifold.science.util.Rational;


import static manifold.science.util.CoercionConstants.r;

public final class InductanceUnit extends AbstractProductUnit<ResistanceUnit, TimeUnit, Inductance, InductanceUnit>
{
  private static final UnitCache<InductanceUnit> CACHE = new UnitCache<>();

  public static final InductanceUnit H = get( ResistanceUnit.BASE, TimeUnit.BASE, 1r, "Henry", "H" );

  public static final InductanceUnit BASE = H;

  public static InductanceUnit get( ResistanceUnit resistanceUnit, TimeUnit timeUnit ) {
    return get( resistanceUnit, timeUnit, null, null, null );
  }
  public static InductanceUnit get( ResistanceUnit resistanceUnit, TimeUnit timeUnit, Rational factor, String name, String symbol ) {
    InductanceUnit unit = new InductanceUnit( resistanceUnit, timeUnit, factor, name, symbol );
    return CACHE.get( unit );
  }

  private InductanceUnit( ResistanceUnit resistanceUnit, TimeUnit timeUnit, Rational factor, String name, String symbol ) {
    super( resistanceUnit, timeUnit, factor, name, symbol );
  }

  @Override
  public Inductance makeDimension( Number amount )
  {
    return  new Inductance( Rational.get( amount ), this );
  }

  public ResistanceUnit getResistanceUnit() {
    return getLeftUnit();
  }
  public TimeUnit getTimeUnit() {
    return getRightUnit();
  }
  
  public ResistanceUnit div( TimeUnit w ) {
    return getResistanceUnit();
  }
}
