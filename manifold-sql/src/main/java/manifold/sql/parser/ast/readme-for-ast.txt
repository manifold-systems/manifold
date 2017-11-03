This is a readme for the AST package.

This package contains all the relevant types for the parser. In general, the AST for a file can be thought of as
starting with a SQL (which is either a DDL or a Statement), and then each class contains in itself other classes which
are in the AST. Many times, the classes contained in the AST are not actually used later; the fact that we have this
enables extensibility down the road.

All AST classes come with two toString methods, a standard toString which overrides the default toString, and a toString
with a parameter initial. The second version is protected, and is only called by other toString methods; this enables
us to visualize the syntax nicely, which may be beneficial for debugging.