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

import manifold.json.rt.api.DataBindings;
import manifold.graphql.sample.movies.Role;
import org.junit.Test;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;

import static manifold.graphql.sample.movies.*;
import static manifold.graphql.sample.movies.Genre.*;
import static manifold.graphql.sample.movies.Type.Main;
import static manifold.graphql.sample.movies.Type.Supporting;
import static org.junit.Assert.*;

public class SchemaTest {
  private static int _ID = 0;

  @Test
  public void testCastBindings() {
    Actor actor = (Actor) new DataBindings();
    ActorInput actorInput = (ActorInput) new DataBindings();
    Animal animal = (Animal) new DataBindings();
    Role role = (Role) new DataBindings();
    Movie movie = (Movie) new DataBindings();
    MovieInput movieInput = (MovieInput) new DataBindings();
    Person person = (Person) new DataBindings();
    Review review = (Review) new DataBindings();
    ReviewInput reviewInput = (ReviewInput) new DataBindings();
  }

  @Test
  public void testBuilders() {
    Person STEVE_MCQUEEN = Person.builder(id(), "Steve McQueen", date(1930, 3, 24))
      .withHeight(1.77)
      .withNationality("American")
      .build();
    Person SLIM_PICKENS = Person.builder(id(), "Slim Pickens", date(1919, 6, 29))
      .withHeight(1.91)
      .withNationality("American")
      .build();
    Person JAMES_GARNER = Person.builder(id(), "James Garner", date(1928, 4, 7))
      .withHeight(1.87)
      .withNationality("American")
      .build();

    Animal TRIGGER = Animal.builder(id(), "Trigger")
      .withKind("Horse")
      .withNationality("American")
      .build();

    Role MICHAEL_DELANEY = Role.builder(id(), STEVE_MCQUEEN, "Michael Delaney", Main)
      .build();
    Role HILTS = Role.builder(id(), STEVE_MCQUEEN, "Hilts 'The Cooler King'", Main)
      .build();
    Role DOC_MCCOY = Role.builder(id(), STEVE_MCQUEEN, "Doc McCoy", Main)
      .build();
    Role COWBOY = Role.builder(id(), SLIM_PICKENS, "Cowboy", Supporting)
      .build();
    Role HENDLY = Role.builder(id(), JAMES_GARNER, "Hendly 'The Scrounger'", Supporting)
      .build();
    Role COMANCHE = Role.builder(id(), TRIGGER, "Comanche", Main)
      .build();
    Role ACE = Role.builder(id(), SLIM_PICKENS, "Ace", Type.Flat)
      .build();

    Movie LE_MANS = Movie.builder(id(), "Le Mans", list(Action), date(1971, 6, 3), list(MICHAEL_DELANEY))
      .build();
    Movie THE_GREAT_ESCAPE = Movie.builder(id(), "The Great Escape", list(Action, Drama), date(1963, 7, 4),
      list(HILTS, HENDLY))
      .build();
    Movie THE_GETAWAY = Movie.builder(id(), "The Getaway", list(Action, Drama, Romance), date(1972, 12, 6),
      list(DOC_MCCOY, COWBOY))
      .build();
    Movie TONKA = Movie.builder(id(), "Tonka", list(Drama, Western), date(1958, 12, 25),
      list(COMANCHE, ACE))
      .build();
  }

  @Test
  public void testCreates() {
    Person STEVE_MCQUEEN = Person.create(id(), "Steve McQueen", date(1930, 3, 24));
    Person SLIM_PICKENS = Person.create(id(), "Slim Pickens", date(1919, 6, 29));
    Person JAMES_GARNER = Person.create(id(), "James Garner", date(1928, 4, 7));

    Animal TRIGGER = Animal.create(id(), "Trigger");

    Role MICHAEL_DELANEY = Role.create(id(), STEVE_MCQUEEN, "Michael Delaney", Main);
    Role HILTS = Role.create(id(), STEVE_MCQUEEN, "Hilts 'The Cooler King'", Main);
    Role DOC_MCCOY = Role.create(id(), STEVE_MCQUEEN, "Doc McCoy", Main);
    Role COWBOY = Role.create(id(), SLIM_PICKENS, "Cowboy", Supporting);
    Role HENDLY = Role.create(id(), JAMES_GARNER, "Hendly 'The Scrounger'", Supporting);
    Role COMANCHE = Role.create(id(), TRIGGER, "Comanche", Main);
    Role ACE = Role.create(id(), SLIM_PICKENS, "Ace", Type.Flat);

    Movie LE_MANS = Movie.create(id(), "Le Mans", list(Action), date(1971, 6, 3), list(MICHAEL_DELANEY));
    Movie THE_GREAT_ESCAPE = Movie.create(id(), "The Great Escape", list(Action, Drama), date(1963, 7, 4),
      list(HILTS, HENDLY));
    Movie THE_GETAWAY = Movie.create(id(), "The Getaway", list(Action, Drama, Romance), date(1972, 12, 6),
      list(DOC_MCCOY, COWBOY));
    Movie TONKA = Movie.create(id(), "Tonka", list(Drama, Western), date(1958, 12, 25), list(COMANCHE, ACE));
  }

  @Test
  public void testInput() {
    MovieInput movieInput = MovieInput.builder("Le Mans", Action).withReleaseDate(date(1971, 6, 3)).build();
    assertEquals("Le Mans", movieInput.getTitle());
    assertEquals(Action, movieInput.getGenre());
    assertEquals(date(1971, 6, 3), movieInput.getReleaseDate());
  }

  @Test
  public void testEnums() {
    Genre genre = Action;
    assertEquals(8, Genre.values().length);

    Type type = Main;
    assertEquals(5, Type.values().length);
  }

  @Test
  public void testUnionLubInterface() {
    assertTrue(Actor.class.isAssignableFrom(CastMember.class));
  }

  @Test
  public void testUnionNoCast() {
    Person person = Person.create(id(), "Steve McQueen", date(1930, 3, 24));
    CastMember cm = person;  // no cast
    Animal animal = Animal.create(id(), "Fred");
    cm = animal;  // no cast
  }

  @Test
  public void testUnionIntersectionMethods() {
    String id = id();
    Person person = Person.create(id, "Steve McQueen", date(1930, 3, 24));
    CastMember cm = person;
    assertSame(id, cm.getId());
    assertSame(person.getName(), cm.getName());
    assertSame(person.getNationality(), cm.getNationality());

    id = id();
    Animal animal = Animal.create(id, "Fred");
    cm = animal;
    assertSame(id, cm.getId());
    assertSame(animal.getName(), cm.getName());
    assertSame(animal.getNationality(), cm.getNationality());
  }

  private static String id() {
    return String.valueOf(++_ID);
  }

  private static LocalDate date(int year, int month, int day) {
    return LocalDate.of(year, month, day);
  }

  @SafeVarargs
  private static <E> List<E> list(E... e) {
    return Arrays.asList(e);
  }
}
