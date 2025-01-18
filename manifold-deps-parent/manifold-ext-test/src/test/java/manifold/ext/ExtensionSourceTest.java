package manifold.ext;


import static org.assertj.core.api.Assertions.*;

import org.junit.jupiter.api.Test;

class ExtensionSourceTest
{
  @Test
  void testExtensionSources()
  {
    String nullString = null;
    String text = "  abc  FoO bar ";

    // extensions from MyStringExtSource \\

    // overridden method should not throw an exception
    assertThat( nullString.trim() ).isNull();
    assertThat( text.trim() ).isEqualTo( "abc  FoO bar" );

    // overridden method should not throw an exception
    assertThat( nullString.startsWith( "  abc" ) ).isFalse();
    assertThat( text.startsWith( "  abc" ) ).isTrue();
    // Excluded method isn't overridden and should still throw a nullpointer exception
    assertThatNullPointerException().isThrownBy( () -> nullString.startsWith( "  abc", 2 ) );

    /// //////////////////////////////////

    // extensions from MyStringExtSource2 \\

    // only included method is overridden and should not throw an exception
    assertThat( nullString.substring( 6 ) ).isNull();
    assertThat( text.substring( 6 ) ).isEqualTo( " FoO bar " );

    // other methods aren't included and are still throwing a NPE
    assertThatNullPointerException().isThrownBy( () -> nullString.substring( 6, 10 ) );
    assertThat( text.substring( 6, 10 ) ).isEqualTo( " FoO" );

    /// //////////////////////////////////

    // extensions from MyStringExtSource3 \\

    // All methods are included, nothing is configured
    assertThat( nullString.toLowerCase() ).isNull();
    assertThat( text.toLowerCase() ).isEqualTo( "  abc  foo bar " );
  }
}