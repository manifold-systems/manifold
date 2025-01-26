package manifold.ext.params;

import junit.framework.TestCase;

public class EnumTest extends TestCase {

    public void testMe()
    {
        assertEquals( "-user", Arg.user.getName() );
        assertEquals( "Github user/org name", Arg.user.getDescription() );
        assertNull( Arg.user.getDefaultValue() );
        assertTrue( Arg.user.isRequired() );
        assertFalse( Arg.user.isFlag() );
        Arg.user.validate("foo");

        assertEquals( "-days", Arg.days.getName() );
        assertEquals( "Number of days to display. Values may range from 1..14. Default is 14.", Arg.days.getDescription() );
        assertEquals( "14", Arg.days.getDefaultValue() );
        assertFalse( Arg.days.isRequired() );
        assertFalse( Arg.days.isFlag() );
        Arg.days.validate("3");
    }

    enum Arg {
        /**
         * Github user name
         */
        user("-user", "Github user/org name") {
            @Override
            public void validate(String value) {
                if (value == null || value.isEmpty()) {
                    throw new RuntimeException(
                            "Argument: '${getName()}' requires a valid github user name, but was: $value");
                }
            }
        },
        /**
         * Github repository name
         */
        repo("-repo", "Github repository name") {
            @Override
            public void validate(String value) {
                if (value == null || value.isEmpty()) {
                    throw new RuntimeException(
                            "Argument: '${getName()}' requires a valid github repository name, but was: $value");
                }
            }
        },
        /**
         * Github authorization token
         */
        token("-token", "Github authorization token") {
            @Override
            public void validate(String value) {
            }
        },
        /**
         * (Optional) Number of days to display, default is 14
         */
        days(name:"-days", description:"Number of days to display. Values may range from 1..14. Default is 14.", required:false, defaultValue:"14") {
            @Override
            public void validate(String value) {
                int days = Integer.parseInt(value);
                if (days < 1 || days > 14) {
                    throw new RuntimeException(
                            "Argument: '${getName()}' must be >=1 and <= 14, but was $value");
                }
            }
        };

        private final String _name;
        private final boolean _required;
        private final boolean _isFlag;
        private final String _description;
        private final String _defaultValue;

        Arg(String name, String description, boolean required = true, boolean isFlag = false, String defaultValue = null) {
            _name = name;
            _required = required;
            _isFlag = isFlag;
            _description = description;
            _defaultValue = defaultValue;
        }

        public String getName() {
            return _name;
        }

        public String getDescription() {
            return _description;
        }

        public String getDefaultValue() {
            return _defaultValue;
        }

        public boolean isRequired() {
            return _required;
        }

        public boolean isFlag() {
            return _isFlag;
        }

        public abstract void validate(String arg);
    }
}
