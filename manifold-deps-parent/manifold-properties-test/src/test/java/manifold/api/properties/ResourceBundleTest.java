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
    assertThat(Message.foo).hasToString("foo");
    assertThat(Message.App.Hello).isEqualTo("Hello");
    assertThat(Message.foo.bar.test).isEqualTo("bar");
    assertThat(Message.getValueByName("foo.bar.test")).isEqualTo("bar");
    assertThat(Message.foo.getValueByName("bar.test")).isEqualTo("bar");
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
    Message.setLocale(Locale.forLanguageTag(languageTag));

    assertThat(Message.foo).hasToString("foo_" + languageTag);
    assertThat(Message.App.Hello).isEqualTo("Hello_" + languageTag);
    assertThat(Message.foo.bar.test).isEqualTo("bar_" + languageTag);
    assertThat(Message.getValueByName("foo.bar.test")).isEqualTo("bar_" + languageTag);
    assertThat(Message.foo.getValueByName("bar.test")).isEqualTo("bar_" + languageTag);
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

    assertThat(Message.foo).hasToString("foo_nl-BE");
    assertThat(Message.App.Hello).isEqualTo("Hello_nl-BE");
    assertThat(Message.foo.bar.test).isEqualTo("bar_nl-BE");
    assertThat(Message.getValueByName("foo.bar.test")).isEqualTo("bar_nl-BE");
    assertThat(Message.foo.getValueByName("bar.test")).isEqualTo("bar_nl-BE");
  }

  /**
   * Verifies that when no match for the message bundle and the locale exist,
   * the default values are used instead.
   * */
  void testMessagesLocaleDoesNotExistLocale()
  {
    Message.setLocale(Locale.forLanguageTag("fr"));

    assertThat(Message.foo).hasToString("foo");
    assertThat(Message.App.Hello).isEqualTo("Hello");
    assertThat(Message.foo.bar.test).isEqualTo("bar");
    assertThat(Message.getValueByName("foo.bar.test")).isEqualTo("bar");
    assertThat(Message.foo.getValueByName("bar.test")).isEqualTo("bar");
  }

  /**
   * Tests the default values for another message bundle ('resourcebundle.Message2').
   * Verifies that the default values are as expected when using the second message bundle.
   */
  @Test
  void testMessagesDefaultSecondMessageBundle()
  {
    assertThat(Message2.m2.foo).hasToString("foo");
    assertThat(Message2.m2.App.Hello).isEqualTo("Hello");
    assertThat(Message2.m2.foo.bar.test).isEqualTo("bar");
  }

  /**
   * Tests the values for a message bundle that has only the default locale available ('resourcebundle.Message3').
   * This uses the normal properties logic.
   */
  @Test
  void testMessagesWithOnlyDefaultMessageBundleAvailable()
  {
    assertThat(Message3.m3.foo).hasToString("foo");
    assertThat(Message3.m3.App.Hello).isEqualTo("Hello");
    assertThat(Message3.m3.foo.bar.test).isEqualTo("bar");
    assertThat(Message3.m3.getValueByName("foo.bar.test")).isEqualTo("bar");
    assertThat(Message3.m3.foo.getValueByName("bar.test")).isEqualTo("bar");

    // verifies that 'setLocale' method does not exist
    assertThat(ReflectionUtils.findMethod(Message3.class, "setLocale", Locale.class)).isEmpty();
  }
}
