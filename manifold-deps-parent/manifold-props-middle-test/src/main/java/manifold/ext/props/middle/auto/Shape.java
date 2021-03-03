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

package manifold.ext.props.middle.auto;

public abstract class Shape
{
  private final String name;
  private final double[] sides;
  private String color = "White";

  public Shape( String name, double... sides )
  {
    this.name = name;
    this.sides = sides;
  }

  public String getName()
  {
    return name;
  }

  public double[] getSides()
  {
    return sides;
  }

  public abstract double getArea();

  public boolean isEquilateral()
  {
    double test = 0;
    for( double s : sides )
    {
      if( test == 0 )
      {
        test = s;
      }
      else if( s != test )
      {
        return false;
      }
    }
    return true;
  }

  public String getColor()
  {
    return color;
  }

  public void setColor( String color )
  {
    this.color = color;
  }
}
