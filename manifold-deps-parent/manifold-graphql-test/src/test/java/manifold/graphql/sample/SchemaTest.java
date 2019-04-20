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

package manifold.graphql.sample;

import org.junit.Test;

import manifold.ext.DataBindings;
import static manifold.graphql.sample.Sample.*;
import static org.junit.Assert.assertEquals;

public class SchemaTest
{
  @Test
  public void testSchema()
  {
    Actor actor = (Actor) new DataBindings();
    ActorInput actorInput = (ActorInput) new DataBindings();
    Animal animal = (Animal) new DataBindings();
    Sample.Character character = (Sample.Character) new DataBindings();
    Movie movie = (Movie) new DataBindings();
    MovieInput movieInput = (MovieInput) new DataBindings();
    Person person = (Person) new DataBindings();
    Review review = (Review) new DataBindings();
    ReviewInput reviewInput = (ReviewInput) new DataBindings();

    Genre genre = Genre.Action;
    assertEquals( 8, Genre.values().length );

    Type type = Type.Main;
    assertEquals( 5, Type.values().length );
  }
}
