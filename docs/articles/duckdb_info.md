# Type-safe analytical SQL with DuckDB and Manifold

With [manifold-sql](https://github.com/manifold-systems/manifold/blob/master/manifold-deps-parent/manifold-sql/readme.md),
DuckDB's productive SQL extensions are directly and type-safely integrated with Java and IntelliJ IDEA.

---
Query types are instantly available as you type native SQL of any complexity in your Java code.

><p><img src="../images/duckdb_1.png" alt="echo method" width="60%" height="60%"/></p>

---
DuckDB's SQL extensions are type-safe and fully integrated with code completion, usage searching, etc.

><p><img src="../images/duckdb_2.png" alt="echo method" width="60%" height="60%"/></p>

---
Parameterized queries are type-safe & sql-injection safe

><p><img src="../images/duckdb_3.png" alt="echo method" width="60%" height="60%"/></p>

---
Execute any type of SQL statement type-safely, either directly or in a batch. 

><p><img src="../images/duckdb_7.png" alt="echo method" width="80%" height="80%"/></p>

---
manifold-sql streamlines DuckDB's additional APIs for a simpler, type-safe dev experience. For instance,
the Appender API is made straightforward and foolproof.

><p><img src="../images/duckdb_4.png" alt="echo method" width="30%" height="30%"/></p>

---
Entity types are automatically derived from your database, providing type-safe CRUD, decoupled
TX, and a lot more.  

```java
Language english = Language.create("English");

Film casablanca = Film.builder("Casablanca", english)
  .withReleaseYear(1942)
  .build();

MyDatabase.commit();

// all generated columns, foreign keys, etc. are auto-assigned after commit
casablanca.getLanguageId();
```               

---
Code completion supplies type-safe access to column properties for both entities and queries.

><p><img src="../images/duckdb_6.png" alt="echo method" width="90%" height="90%"/></p>

---
Quick, type-safe access to foreign key relations.

><p><img src="../images/duckdb_5.png" alt="echo method" width="90%" height="90%"/></p>

---
Learn more about manifold-sql [here](https://github.com/manifold-systems/manifold/blob/master/manifold-deps-parent/manifold-sql/readme.md).

