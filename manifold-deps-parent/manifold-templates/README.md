# ManTL (Manifold Template Language)

ManTL is a lightweight & _type-safe_ template engine for the JVM using [Manifold](http://manifold.systems/).
It is modeled loosely on Java Server Pages (JSP), but is divorced from the Servlet API and thus can be
used in any application environment.

ManTL supports type-safe arguments to templates, type-safe inclusion of other templates,
shared layouts for templates and custom base classes for application-specific logic, among other features.

ManTL files have the suffix `mtl`, often optionally preceded by the language that the template is targeting 
(e.g. `index.html.mtl`).

See [here](http://manifold.systems/manifold-templates.html) for the documentation.