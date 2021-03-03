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

public class RightTriangle extends Triangle
{
  public RightTriangle( double a, double b )
  {
    super( a, b, Math.sqrt( a*a + b*b ) );
  }

  public double getA()
  {
    return getSides()[0];
  }

  public double getB()
  {
    return getSides()[1];
  }

  public double getC()
  {
    return getSides()[2];
  }

  @Override
  public double getArea()
  {
    return (getA() * getB())/2;
  }
}
