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

package manifold.science;

import java.math.BigDecimal;
import java.math.BigInteger;
import manifold.science.util.Rational;


import static manifold.science.util.CommonConstants.*;

/**
 * The range of the metric scale from Yocto to Yatta delcared in standard SI abbreviated form for use as units.
 * Applicable to all Number types and String such as:
 * <pre><code>
 * 1.25M // 1.25 Million
 * "1.25"M // 1.25 Million
 * 1.25m // 1.25 milli or 0.00125
 * </code></pre>
 */
public enum MetricScaleUnit {
  y( YOCTO, "yocto", "y" ),
  z( ZEPTO, "zepto", "z" ),
  a( ATTO, "atto", "a" ),
  fe( FEMTO, "femto", "f" ), // 'fe', not 'f' because conflicts with number literal float suffix
  p( PICO, "pico", "p" ),
  n( NANO, "nano", "n" ),
  u( MICRO, "micro", "u" ),
  m( MILLI, "milli", "m" ),
  c( CENTI, "centi", "c" ),
  de( DECI, "deci", "d" ), // 'de', not 'd' because conflicts with number literal float suffix
  r( Rational.ONE, "", "" ), // a nice way to make Rational numbers from literals eg., 5r
  da( DECA, "Deca", "da" ),
  h( HECTO, "Hecto", "h" ),
  k( KILO, "Kilo", "k" ),
  Ki( KIBI, "Kibi", "Ki" ),
  M( KILO.pow( 2 ), "Mega", "M" ),
  Mi( KIBI.pow( 2 ), "Mebi", "Mi" ),
  G( KILO.pow( 3 ), "Giga", "G" ),
  Gi( KIBI.pow( 3 ), "Gibi", "Gi" ),
  T( KILO.pow( 4 ), "Tera", "T" ),
  Ti( KIBI.pow( 4 ), "Tebi", "Ti" ),
  P( KILO.pow( 5 ), "Peta", "P" ),
  Pi( KIBI.pow( 5 ), "Pebi", "Pi" ),
  E( KILO.pow( 6 ), "Exa", "E" ),
  Ei( KIBI.pow( 6 ), "Exbi", "Ei" ),
  Z( KILO.pow( 7 ), "Zetta", "Z" ),
  Zi( KIBI.pow( 7 ), "Zebi", "Zi" ),
  Y( KILO.pow( 8 ), "Yotta", "Y" ),
  Yi( KIBI.pow( 8 ), "Yobi", "Yi" );
  
  private Rational _amount;
  private String _name;
  private String _symbol;

  MetricScaleUnit( Rational amount, String name, String symbol ) {
    _amount = amount;
    _name = name;
    _symbol = symbol;
  }

  public Rational getAmount() {
    return _amount;
  }

  public String getUnitName() {
    return _name;
  }

  public String getUnitSymbol() {
    return _symbol;
  }

  public Rational postfixBind( String value ) {
    return _amount * Rational.get( value );
  }
  public Rational postfixBind( Integer value ) {
    return _amount * value;
  }
  public Rational postfixBind( Long value ) {
    return _amount * value;
  }
  public Rational postfixBind( Float value ) {
    return _amount * value;
  }
  public Rational postfixBind( Double value ) {
    return _amount * value;
  }
  public Rational postfixBind( BigInteger value ) {
    return _amount * value;
  }
  public Rational postfixBind( BigDecimal value ) {
    return _amount * value;
  }
  public Rational postfixBind( Rational value ) {
    return _amount * value;
  }
}
