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

package manifold.ext.stuff;

import manifold.ext.api.Self;

import java.awt.*;

public class CarBuilder extends AbstractBuilder {
  private Color _color;

  public @Self CarBuilder withColor(int red, int green, int blue) {
    _color = new Color(red, green, blue);
    return this;
  }

  protected Color getColor() {
    return _color;
  }

  public Car build() {
    return new Car(getName(), getColor());
  }
}
