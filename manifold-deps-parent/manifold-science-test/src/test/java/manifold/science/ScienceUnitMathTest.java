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

import org.junit.Test;

import static manifold.science.MetricScaleUnit.r;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;
import static manifold.science.util.UnitConstants.*;

public class ScienceUnitMathTest {

  @Test
  public void testLengthMath() {
    for( LengthUnit unit : LengthUnit.values() ) {
      Length a = 1 unit;
      for( LengthUnit unit2 : LengthUnit.values() ) {
        Length b = 1 unit2;
        assertEquals( new Length( a.toBaseNumber() + b.toBaseNumber(), LengthUnit.BASE, unit ), a + b );
        assertEquals( new Length( a.toBaseNumber() - b.toBaseNumber(), LengthUnit.BASE, unit ), a - b );
        assertEquals( new Area( a.toBaseNumber() * b.toBaseNumber(), LengthUnit.BASE * LengthUnit.BASE, unit * unit2 ), a * b );
        assertEquals( a.toBaseNumber() / b.toBaseNumber(), a / b );
        assertEquals( a.toBaseNumber() % b.toBaseNumber(), a % b );
      }

      // Length / Time = Velocity
      Time time = 2s;
      time.toString();
      Velocity velocity = new Velocity( a.toBaseNumber() / time.toBaseNumber(), VelocityUnit.BASE, unit / time.getUnit() );
      assertEquals( velocity, a / time );

      // Length / Velocity = Time
      assertEquals( time, a / velocity );

      // Length * Area = Volume
      Area area = a * a;
      assertEquals( new Volume( a.toBaseNumber() * area.toBaseNumber(), VolumeUnit.BASE, unit * area.getUnit() ), a * area );

      // Length * Force = Energy
      Force force = 2 kg m/s/s;
      assertEquals( 2 N, force );
      assertEquals( new Energy( a.toBaseNumber() * force.toBaseNumber(), EnergyUnit.BASE, unit * force.getUnit() ), a * force );
    }
  }

  @Test
  public void testTimeMath() {
    for( TimeUnit unit : TimeUnit.values() ) {
      Time a = 1 unit;
      for( TimeUnit unit2 : TimeUnit.values() ) {
        Time b = 1 unit2;
        assertEquals( new Time( a.toBaseNumber() + b.toBaseNumber(), TimeUnit.BASE, unit ), a + b );
        assertEquals( new Time( a.toBaseNumber() - b.toBaseNumber(), TimeUnit.BASE, unit ), a - b );
        // multiplication undefined
        assertEquals( a.toBaseNumber() / b.toBaseNumber(), a / b );
        assertEquals( a.toBaseNumber() % b.toBaseNumber(), a % b );
      }

      // Time * Velocity = Length
      Length len = 1m;
      Velocity velocity = new Velocity( len.toBaseNumber() / a.toBaseNumber(), VelocityUnit.BASE, len.getUnit() / unit );
      assertEquals( len, a * velocity );

      // Time * Acceleration = Velocity
      Acceleration acceleration = new Acceleration( velocity.toBaseNumber() / a.toBaseNumber(), AccelerationUnit.BASE, velocity.getUnit() / unit );
      assertEquals( velocity, a * acceleration );

      // Time * Current = Charge
      Charge charge = 2 coulomb;
      Current current = new Current( charge.toBaseNumber() / a.toBaseNumber(), CurrentUnit.BASE, charge.getUnit() / unit );
      assertEquals( charge, a * current );

      // Time * Frequency = Angle
      Frequency frequency = 2 Hz;
      Angle angle = new Angle( a.toBaseNumber() * frequency.toBaseNumber(), AngleUnit.BASE, unit * frequency.getUnit() );
      assertEquals( angle, a * frequency );

      // Time * Power = Energy
      Power power = 2 watt;
      Energy energy = new Energy( a.toBaseNumber() * power.toBaseNumber(), EnergyUnit.BASE, unit * power.getUnit() );
      assertEquals( energy, a * power );

      // Time * Force = Momentum
      Force force = 2 N;
      Momentum momentum = new Momentum( a.toBaseNumber() * force.toBaseNumber(), MomentumUnit.BASE, unit * force.getUnit() );
      assertEquals( momentum, a * force );
    }
  }

  @Test
  public void testMassMath() {
    for( MassUnit unit : MassUnit.values() ) {
      Mass a = 1 unit;
      for( MassUnit unit2 : MassUnit.values() ) {
        Mass b = 1 unit2;
        assertEquals( new Mass( a.toBaseNumber() + b.toBaseNumber(), MassUnit.BASE, unit ), a + b );
        assertEquals( new Mass( a.toBaseNumber() - b.toBaseNumber(), MassUnit.BASE, unit ), a - b );
        // multiplication undefined
        assertEquals( a.toBaseNumber() / b.toBaseNumber(), a / b );
        assertEquals( a.toBaseNumber() % b.toBaseNumber(), a % b );
      }

      // Mass * Acceleration = Force
      Acceleration acc = new Acceleration( 2r, AccelerationUnit.BASE );
      Force force = new Force( a.toBaseNumber() * acc.toBaseNumber(), ForceUnit.BASE, unit * acc.getUnit() );
      assertEquals( force, a * acc );
      assertEquals( 2 * force, force + force );

      // Mass * Velocity = Momentum
      Velocity v = new Velocity( 2r, VelocityUnit.BASE );
      Momentum momentum = new Momentum( a.toBaseNumber() *  v.toBaseNumber(), MomentumUnit.BASE, unit * v.getUnit() );
      assertEquals( momentum, a * v );

      // Mass * Area = Pressure
      Area area = new Area( 2r, AreaUnit.BASE );
      Pressure pressure = new Pressure( a.toBaseNumber() /  area.toBaseNumber(), PressureUnit.BASE, unit / area.getUnit() );
      assertEquals( pressure, a / area );

      // Mass * Volume = Density
      Volume volume = new Volume( 2r, VolumeUnit.BASE );
      Density density = new Density( a.toBaseNumber() /  volume.toBaseNumber(), DensityUnit.BASE, unit / volume.getUnit() );
      assertEquals( density, a / volume );
    }
  }

  @Test
  public void testTemperatureMath() {
    for( TemperatureUnit unit : TemperatureUnit.values() ) {
      Temperature a = 1 unit;
      for( TemperatureUnit unit2 : TemperatureUnit.values() ) {
        Temperature b = 1 unit;
        assertEquals( new Temperature( a.toBaseNumber() + b.toBaseNumber(), TemperatureUnit.BASE, unit ), a + b );
        assertEquals( new Temperature( a.toBaseNumber() - b.toBaseNumber(), TemperatureUnit.BASE, unit ), a - b );
        // multiplication undefined
        assertEquals( a.toBaseNumber() / b.toBaseNumber(), a / b );
        assertEquals( a.toBaseNumber() % b.toBaseNumber(), a % b );
      }
    }
  }

  @Test
  public void testChargeMath() {
    for( ChargeUnit unit : ChargeUnit.values() ) {
      Charge a = 1 unit;
      for( ChargeUnit unit2 : ChargeUnit.values() ) {
        Charge b = 1 unit;
        assertEquals( new Charge( a.toBaseNumber() + b.toBaseNumber(), ChargeUnit.BASE, unit ), a + b );
        assertEquals( new Charge( a.toBaseNumber() - b.toBaseNumber(), ChargeUnit.BASE, unit ), a - b );
        // multiplication undefined
        assertEquals( a.toBaseNumber() / b.toBaseNumber(), a / b );
        assertEquals( a.toBaseNumber() % b.toBaseNumber(), a % b );
      }
    }
  }

  @Test
  public void testAngleMath() {
    for( AngleUnit unit : AngleUnit.values() ) {
      Angle a = 1 unit;
      for( AngleUnit unit2 : AngleUnit.values() ) {
        Angle b = 1 unit2;
        assertEquals( new Angle( a.toBaseNumber() + b.toBaseNumber(), AngleUnit.BASE, unit ), a + b );
        assertEquals( new Angle( a.toBaseNumber() - b.toBaseNumber(), AngleUnit.BASE, unit ), a - b );
        // multiplication undefined
        assertEquals( a.toBaseNumber() / b.toBaseNumber(), a / b );
        assertEquals( a.toBaseNumber() % b.toBaseNumber(), a % b );
      }
    }
  }

  @Test
  public void testCombinations() {
    Force f = 9 kg m/s/s;
    Force ff = (9) (kg m/s/s);
    Force fff = (9r) kg m/s/s;
  }

  @Test
  public void testCaches() {
    assertSame( N m, kg m/s/s m );
    assertSame( N m, J );
    assertSame( J, kg m/s/s m );

    assertEquals( 5 N m, 5 kg m/s/s m );
    assertEquals( 5 N m, 5 J );
    assertEquals( 5 J, 5 kg m/s/s m );
  }
}