>**⚠ _Experimental Feature_**

# Manifold : SQL

![latest](https://img.shields.io/badge/latest-v2023.1.30-darkgreen.svg)
[![slack](https://img.shields.io/badge/slack-manifold-blue.svg?logo=slack)](https://join.slack.com/t/manifold-group/shared_invite/zt-e0bq8xtu-93ASQa~a8qe0KDhOoD6Bgg)
![GitHub Repo stars](https://img.shields.io/github/stars/manifold-systems/manifold?logo=github&color=red)


Manifold provides a simple alternative to conventional JDBC abstractions that makes it possible to use native SQL
directly and type-safely from Java code.

- Entity types are automatically derived from JDBC metadata at compile-time, fully relational/FK aware, CRUD, etc.
- Query types are instantly available as you type native SQL of any complexity in your IDE
- Query results are type-safe and type-rich and simple to use (see examples below)
- No ORM, No DSLs, No annotations, and No code generation build steps

Use Manifold simply by adding the Manifold compiler [plugin](https://github.com/manifold-systems/manifold) and dependencies
to your gradle or maven build.

---

## Features:
- Use actual SQL directly and type-safely in your Java code<br>
- _Inline_ SQL directly in Java source, or use .sql files<br>
- Type-safe DDL &bull; Type-safe queries &bull; Type-safe results<br>
- Full CRUD support with DDL type projections (entities)<br>
- No code gen build steps &bull; No ORM shenanigans &bull; No DSL mumbo-jumbo<br>
- Managed transaction scoping, commit/revert entity changes as needed<br>
- Pluggable architecture with simple dependency injection<br>
- Tested with popular JDBC database drivers and SQL dialects<br>
- Comprehensive IDE support (IntelliJ IDEA, Android Studio)
- Supports Java 8 - 21 (LTS releases)

### Examples

<img width="800" height="120" align="top" src="../../docs/images/img_3.png">
<br>
<br>
<img width="550" height="400" align="top" src="../../docs/images/img.png">
<br>

## Coming _**real**_ soon. . .
                                   
> This project is nearing a preview release. A healthy round of tire kicking and general feedback is needed. If you
> would like to participate, please shoot an email to [info@manifold.systems](mailto:info@manifold.systems) or send a
> direct message to Scott McKinney on our [slack](https://join.slack.com/t/manifold-group/shared_invite/zt-e0bq8xtu-93ASQa~a8qe0KDhOoD6Bgg). 


