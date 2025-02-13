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

package manifold.api.properties;

import static org.assertj.core.api.Assertions.*;
import java.util.Locale;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.junit.platform.commons.util.ReflectionUtils;
import resourcebundle.Message;
import resourcebundle.Message2;
import resourcebundle.Message3;

class ResourceBundleTest
{

  /**
   * Tests the default values for the message bundle 'resourcebundle.Messages' when the default locale is used.
   */
  @Test
  void testMessagesDefault()
  {
    assertThat(Message.getLocale()).isEqualTo(Locale.ROOT);
    assertThat(Message.foo).hasToString("foo");
    assertThat(Message.App.Hello).hasToString("Hello");
    assertThat(Message.foo.bar.test).hasToString("bar");
    assertThat(Message.getValueByName("foo.bar.test")).hasToString("bar");
    assertThat(Message.foo.getValueByName("bar.test")).hasToString("bar");
  }

  /**
   * Tests the localized values for the message bundle 'resourcebundle.Messages'.
   * Verifies that the values are correctly localized based on the provided language tag.
   *
   * @param languageTag The language tag to test.
   */
  @ParameterizedTest
  @ValueSource(strings = {"nl", "nl-BE", "nl-NL", "fr-FR", "fr-FR-POSIX"})
  void testMessagesLocalized(String languageTag)
  {
    Locale locale = Locale.forLanguageTag(languageTag);
    Message.setLocale(locale);

    assertThat(Message.getLocale()).isEqualTo(locale);
    assertThat(Message.foo).hasToString("foo_" + languageTag);
    assertThat(Message.App.Hello).hasToString("Hello_" + languageTag);
    assertThat(Message.foo.bar.test).hasToString("bar_" + languageTag);
    assertThat(Message.getValueByName("foo.bar.test")).hasToString("bar_" + languageTag);
    assertThat(Message.foo.getValueByName("bar.test")).hasToString("bar_" + languageTag);
  }

  /**
   * Verifies that when an exact match for the message bundle and the locale does not exist,
   * the most specific values are used instead.
   *
   */
  @Test
  void testMessagesLocaleDoesNotExistVariant()
  {
    Message.setLocale(Locale.forLanguageTag("nl-BE-POSIX"));

    assertThat(Message.getLocale()).isEqualTo(Locale.forLanguageTag("nl-BE"));
    assertThat(Message.foo).hasToString("foo_nl-BE");
    assertThat(Message.App.Hello).hasToString("Hello_nl-BE");
    assertThat(Message.foo.bar.test).hasToString("bar_nl-BE");
    assertThat(Message.getValueByName("foo.bar.test")).hasToString("bar_nl-BE");
    assertThat(Message.foo.getValueByName("bar.test")).hasToString("bar_nl-BE");
  }

  /**
   * Verifies that when no match for the message bundle and the locale exist,
   * the default values are used instead.
   * */
  @Test
  void testMessagesLocaleDoesNotExistLocale()
  {
    Message.setLocale(Locale.forLanguageTag("fr"));

    assertThat(Message.getLocale()).isEqualTo(Locale.ROOT);
    assertThat(Message.foo).hasToString("foo");
    assertThat(Message.App.Hello).hasToString("Hello");
    assertThat(Message.foo.bar.test).hasToString("bar");
    assertThat(Message.getValueByName("foo.bar.test")).hasToString("bar");
    assertThat(Message.foo.getValueByName("bar.test")).hasToString("bar");
  }

  /**
   * Tests the default values for another message bundle ('resourcebundle.Message2').
   * Verifies that the default values are as expected when using the second message bundle.
   */
  @Test
  void testMessagesDefaultSecondMessageBundle()
  {
    assertThat(Message2.m2.foo).hasToString("foo");
    assertThat(Message2.m2.App.Hello).hasToString("Hello");
    assertThat(Message2.m2.foo.bar.test).hasToString("bar");
  }

  /**
   * Tests the values for a message bundle that has only the default locale available ('resourcebundle.Message3').
   * This uses the normal properties logic.
   */
  @Test
  void testMessagesWithOnlyDefaultMessageBundleAvailable()
  {
    assertThat(Message3.m3.foo).hasToString("foo");
    assertThat(Message3.m3.App.Hello).hasToString("Hello");
    assertThat(Message3.m3.foo.bar.test).hasToString("bar");
    assertThat(Message3.m3.getValueByName("foo.bar.test")).hasToString("bar");
    assertThat(Message3.m3.foo.getValueByName("bar.test")).hasToString("bar");

    // verifies that 'setLocale' method does not exist
    assertThat(ReflectionUtils.findMethod(Message3.class, "setLocale", Locale.class)).isEmpty();
    // verifies that 'getLocale' method does not exist
    assertThat(ReflectionUtils.findMethod(Message3.class, "getLocale")).isEmpty();
  }
}
