package manifold.science;

import manifold.science.api.AbstractQuotientUnit;
import manifold.science.api.UnitCache;
import manifold.science.util.Rational;


import static manifold.science.AngleUnit.Radian;
import static manifold.science.AngleUnit.Turn;
import static manifold.science.util.CoercionConstants.r;
import static manifold.science.TimeUnit.Minute;
import static manifold.science.TimeUnit.Second;

public final class FrequencyUnit extends AbstractQuotientUnit<AngleUnit, TimeUnit, Frequency, FrequencyUnit>
{
  private static final UnitCache<FrequencyUnit> CACHE = new UnitCache<>();

  public static final FrequencyUnit BASE = get( Radian, Second );
  public static final FrequencyUnit Hertz = get( Turn, Second, 1r, "Hertz", "Hz" );
  public static final FrequencyUnit RPM = get( Turn, Minute, 1r, "RPM", "rpm" );

  public static FrequencyUnit get( AngleUnit angleUnit, TimeUnit timeUnit ) {
    return get( angleUnit, timeUnit, null, null, null );
  }
  public static FrequencyUnit get( AngleUnit angleUnit, TimeUnit timeUnit, Rational factor, String name, String symbol ) {
    FrequencyUnit unit = new FrequencyUnit( angleUnit, timeUnit, factor, name, symbol );
    return CACHE.get( unit );
  }

  private FrequencyUnit( AngleUnit angleUnit, TimeUnit timeUnit, Rational factor, String name, String symbol ) {
    super( angleUnit, timeUnit, factor, name, symbol );
  }

  @Override
  public Frequency makeDimension( Number amount )
  {
    return new Frequency( Rational.get( amount ), this );
  }

  public AngleUnit getAngleUnit() {
    return getLeftUnit();
  }
  public TimeUnit getTimeUnit() {
    return getRightUnit();
  } 
}
