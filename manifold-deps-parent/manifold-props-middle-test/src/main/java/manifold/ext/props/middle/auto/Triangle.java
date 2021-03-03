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

public class Triangle extends Shape
{
  double area; // handle field having same name as inferred property (and property still working)

  public Triangle( double s1, double s2, double s3 )
  {
    super( "Triangle", s1, s2, s3 );
  }

  public double area()
  {
    area = 5; // must be direct field assign
    return area; // must be direct field access
  }

  @Override
  public double getArea()
  {
    double[] sides = getSides();
    double s = (sides[0] + sides[1] + sides[2]) / 2.0;
    return Math.sqrt( s * (s - sides[0]) * (s - sides[1]) * (s - sides[2]) );
  }
}
