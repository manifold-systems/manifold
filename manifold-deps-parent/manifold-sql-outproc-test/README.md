This module tests out-of-process, server-based databases, as opposed to in-process, or embedded, databases, which are
located in manifold-sql-inproc-test.

Note, the server-based nature of these tests prevent this module from working with continuous integration, at least the
way things are set up now. As a consequence, this module must never be checked into git as a child module of its parent.
It should always be commented out and then commented back in when performing local server testing during dev.