# Javadoc agent

If your project generates javadoc and uses any of the following Manifold features, read on.

* Properties (via `manifold-props`)
* Operator overloading (via `manifold-ext`)
* Preprocessor (via `manifold-preprocessor`)

Although the JDK's javadoc tool uses the Java compiler to parse Java source code, curiously it does not support the `-Xplugin`
argument for compiler plugins. This is not a problem for most of Manifold's features, however there are a few that
require intervention, namely the ones listed above.

If your project generates javadoc and uses any of these language extensions, you'll need to add Manifold's javadoc agent
to your javadoc config:
```xml
<dependencies>
  <!-- For Javadoc agent -->
  <dependency>
    <groupId>systems.manifold</groupId>
    <artifactId>manifold-javadoc-agent</artifactId>
    <scope>provided</scope>
    <version>${manifold.version}</version>
  </dependency>
</dependencies>

<profiles>
  <profile>
    <id>release</id>
    <build>
      <plugins>
        <!-- For javadoc agent referenced in maven-javadoc-plugin -->
        <plugin>
          <groupId>org.apache.maven.plugins</groupId>
          <artifactId>maven-dependency-plugin</artifactId>
          <version>2.8</version>
          <executions>
            <execution>
              <goals>
                <goal>properties</goal>
              </goals>
            </execution>
          </executions>
        </plugin>
        
        <!-- add Manifold's javadoc agent -->
        <plugin>
          <groupId>org.apache.maven.plugins</groupId>
          <artifactId>maven-javadoc-plugin</artifactId>
          <version>3.2.0</version>
          <configuration>
            <encoding>UTF-8</encoding>
            <additionalOptions>
              <additionalOption>-Xdoclint:none</additionalOption>
            </additionalOptions>
            
            <!-- java agent adds the manifold compiler plugin to javadoc's compiler -->
            <additionalJOptions>
              <additionalJOption>-J-javaagent:${systems.manifold:manifold-javadoc-agent:jar}</additionalJOption>
            </additionalJOptions>
    
            <!-- Add project's manifold dependencies to javadoc classpath. 
                 Note, Maven does _not_ make these transitive, thus manifold-all is a convenience here, ymmv. 
                 Also note, if your project defines module-info.java files, you _cannot_ use manifold-all, instead you
                 must provide the explicit dependencies that would otherwise be implied if additionalDependencies
                 were transitive. -->
            <additionalDependencies>
              <additionalDependency>
                <groupId>systems.manifold</groupId>
                <artifactId>manifold-all</artifactId>
                <version>${manifold.version}</version>
              </additionalDependency>
            </additionalDependencies>
              
          </configuration>
          <executions>
            <execution>
              <id>attach-javadocs</id>
              <goals>
                <goal>jar</goal>
              </goals>
            </execution>
          </executions>
        </plugin>
      </plugins>
    </build>
  </profile>
</profiles>
```
