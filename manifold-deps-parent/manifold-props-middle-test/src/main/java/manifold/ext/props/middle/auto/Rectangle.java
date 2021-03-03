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

public class Rectangle extends Shape
{
  public Rectangle( String name, double s1, double s2 )
  {
    super( name, s1, s2, s1, s2 );
  }

  public double getLength()
  {
    double[] sides = getSides();
    return Math.max( sides[0], sides[1] );
  }

  public double getWidth()
  {
    double[] sides = getSides();
    return Math.min( sides[0], sides[1] );
  }

  @Override
  public double getArea()
  {
    double[] sides = getSides();
    return sides[0] * sides[1];
  }
}
