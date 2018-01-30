The .class files in this directory are compiled with Java 9.  The corresponding source files end with *.java9 in
the src directory.  To compile them:

- use maven
- set java_home=<your java9 path>
- temporarily rename *.java9 to *.java
- mvn clean compile (only manifold core needs to compile, there will be errors in other modules)
- copy the corresponding class files from the target dir and place them in the this resource directory
- revert the renamed files back to *.java9