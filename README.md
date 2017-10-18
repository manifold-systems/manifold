<center>
  <img src="https://i.imgur.com/No1RPUf.png" width=80% height=80%/>
</center>

# Introduction

Manifold is a unique framework that allows developers to dynamically and seamlessly 
extend Java's type system. Building on this core framework Manifold provides features 
to make Java development more appealing and productive.

What does Manifold do for Java programmers?
* **Meta-programming**: Type-safe, direct access to your data. Eliminate code generators. Minimize build time.
* **Extensions**: Add methods to existing Java classes such as `String`, `List`, and `File`. Eliminate boilerplate code involving "Util" and "Manager" libraries.
* **Structural Typing**: Unify disparate APIs. Bridge software components you do not control.

## Getting Started

Using Manifold in your Java project is simple:

* Add the Manifold jar[s] to your classpath
* Add '-Xplugin:Manifold' as an argument to Javac

That's all.

Manifold currently works with Java 8.  Support for Java 9 is coming soon.

For the convenience of non-maven users you can directly download Manifold binaries:
* [manifold](http://repo1.maven.org/maven2/systems/manifold/manifold/0.1-alpha/manifold-0.1-alpha.jar):
Core Manifold support, also includes properties and image manifolds
* [manifold-ext](http://repo1.maven.org/maven2/systems/manifold/manifold-ext/0.1-alpha/manifold-ext-0.1-alpha.jar):
Support for structural typing and extensions
* [manifold-json](http://repo1.maven.org/maven2/systems/manifold/manifold-json/0.1-alpha/manifold-json-0.1-alpha.jar):
JSON and JSchema support
* [manifold-js](http://repo1.maven.org/maven2/systems/manifold/manifold-js/0.1-alpha/manifold-js-0.1-alpha.jar):
JavaScript support
* [manifold-collections](http://repo1.maven.org/maven2/systems/manifold/manifold-collections/0.1-alpha/manifold-collections-0.1-alpha.jar):
Collections extensions
* [manifold-io](http://repo1.maven.org/maven2/systems/manifold/manifold-io/0.1-alpha/manifold-io-0.1-alpha.jar):
I/O extensions
* [manifold-text](http://repo1.maven.org/maven2/systems/manifold/manifold-text/0.1-alpha/manifold-text-0.1-alpha.jar):
Text extensions
* [manifold-templates](http://repo1.maven.org/maven2/systems/manifold/manifold-templates/0.1-alpha/manifold-templates-0.1-alpha.jar):
Integrated template support

### Maven

Add manifold artifacts that suit your project's needs.  The minimum requirements are to 
include the core `manifold` artifact and `tools.jar` and add the `-Xplugin:Manifold`
argument as a Java compiler argument:

```xml
  <dependencies>
    <!--Core Manifold support, includes properties and image manifolds-->
    <dependency>
      <groupId>systems.manifold</groupId>
      <artifactId>manifold</artifactId>
      <!--<version>\${project.version}</version>-->
    </dependency>
    
    <!--Support for structural typing and extensions-->
    <dependency>
      <groupId>systems.manifold</groupId>
      <artifactId>manifold-ext</artifactId>
      <!--<version>\${project.version}</version>-->
    </dependency>
    
    <!--JSON and JSchema support-->
    <dependency>
      <groupId>systems.manifold</groupId>
      <artifactId>manifold-json</artifactId>
      <!--<version>\${project.version}</version>-->
    </dependency>
    
    <!--JavaScript support-->
    <dependency>
      <groupId>systems.manifold</groupId>
      <artifactId>manifold-js</artifactId>
      <!--<version>\${project.version}</version>-->
    </dependency>
    
    <!--Template support-->
    <dependency>
      <groupId>systems.manifold</groupId>
      <artifactId>manifold-templates</artifactId>
      <!--<version>\${project.version}</version>-->
    </dependency>
    
    <!--Collections extensions-->
    <dependency>
      <groupId>systems.manifold</groupId>
      <artifactId>manifold-collections</artifactId>
      <!--<version>\${project.version}</version>-->
    </dependency>
    
    <!--I/O extensions-->
    <dependency>
      <groupId>systems.manifold</groupId>
      <artifactId>manifold-io</artifactId>
      <!--<version>\${project.version}</version>-->
    </dependency>
    
    <!--I/O extensions-->
    <dependency>
      <groupId>systems.manifold</groupId>
      <artifactId>manifold-io</artifactId>
      <!--<version>\${project.version}</version>-->
    </dependency>
    
    <!--Text extensions-->
    <dependency>
      <groupId>systems.manifold</groupId>
      <artifactId>manifold-text</artifactId>
      <!--<version>\${project.version}</version>-->
    </dependency>    
  </dependencies>

  <build>
    <plugins>
      <plugin>
        <groupId>org.apache.maven.plugins</groupId>
        <artifactId>maven-compiler-plugin</artifactId>
        <configuration>
          <compilerArgs>
            <arg>-Xplugin:Manifold</arg>
          </compilerArgs>
        </configuration>
      </plugin>
    </plugins>
  </build>

  <profiles>
    <profile>
      <id>internal.tools-jar</id>
      <activation>
        <file>
          <exists>\${java.home}/../lib/tools.jar</exists>
        </file>
      </activation>
      <dependencies>
        <dependency>
          <groupId>com.sun</groupId>
          <artifactId>tools</artifactId>
          <version>1.8.0</version>
          <scope>system</scope>
          <systemPath>\${java.home}/../lib/tools.jar</systemPath>
        </dependency>
      </dependencies>
    </profile>
  </profiles>
```

### Gradle

Add manifold artifacts that suite your project's needs.  The minimum requirements are to 
include the core `manifold` artifact and `tools.jar` and add the `-Xplugin:Manifold`
argument as a Java compiler argument:

```groovy
dependencies {
  // Core Manifold support, includes properties and image manifolds
  compile group: 'systems.manifold', name: 'manifold', version: '0.1-SNAPSHOT'
  
  // Support for structural typing and extensions
  compile group: 'systems.manifold', name: 'manifold-ext', version: '0.1-SNAPSHOT'
    
  // JSON and JSchema support  
  compile group: 'systems.manifold', name: 'manifold-json', version: '0.1-SNAPSHOT'
  
  // JavaScript support
  compile group: 'systems.manifold', name: 'manifold-js', version: '0.1-SNAPSHOT'
  
  // Template support
  compile group: 'systems.manifold', name: 'manifold-templates', version: '0.1-SNAPSHOT'
  
  // Collection extensions
  compile group: 'systems.manifold', name: 'manifold-collections', version: '0.1-SNAPSHOT'
  
  // I/O extensions
  compile group: 'systems.manifold', name: 'manifold-io', version: '0.1-SNAPSHOT'
  
  // Text extensions
  compile group: 'systems.manifold', name: 'manifold-text', version: '0.1-SNAPSHOT'
  
  // tools.jar
  compile files("\${System.getProperty('java.home')}/../lib/tools.jar")
}

compileJava {
  options.compilerArgs += ['-Xplugin:Manifold']
}
```

### IntelliJ

Get the [Manifold plugin](https://plugins.jetbrains.com/plugin/10057-manifold) for IntelliJ IDEA.

Install the plugin directly from IntelliJ via: `Settings | Plugins | Browse Repositories | Manifold`


## Contributing

To contribute a change to Manifold:

* Fork the main manifold repository
* Create a new feature branch based on the `development` branch with a reasonably descriptive name (e.g. `fix_json_specific_thing`)
* Implement your fix
* Add a test to `/test/unit_tests.html`.  (It's pretty easy!)
* Create a pull request for that branch against `development` in the main repository

## Versioning

For the versions available, see the [tags on this repository](https://github.com/manifold-systems/manifold/tags). 

## Authors

* **Scott McKinney** - *Manifold creator, principal engineer, and founder of [Manifold Systems, LLC](http://manifold.systems)*
</p>

* **Carson Gross** - *Contributor, Manifold Templates author*
* **Kyle Moore** - *Contributor, build system hero*
* **Natalie McKinney** - *Contributor, CSV and FASTA parsers*
* **Luca Boasso** - *Contributor*

See also the list of [contributors](https://github.com/manifold-systems/manifold/graphs/contributors) who participated in this project.

## License

The open source portion of this project is licensed under the [Apache 2.0](https://www.apache.org/licenses/LICENSE-2.0) - see [our website](http://manifold.systems) for details

## Acknowledgments

* Much of the inspiration for Manifold came from the [Gosu language](https://gosu-lang.github.io/), namely its _Open Type System_
* Many thanks to Carson Gross for getting the Manifold Systems website off the ground
* Shout out to [Lazerhawk](https://lazerhawk.bandcamp.com/album/redline) for world class coding music

## Website

Visit the [Manifold](http://manifold.systems) website to learn more about manifold.

[![Join the chat at https://gitter.im/manifold-io/Lobby](https://badges.gitter.im/intercooler-js/Lobby.svg)](https://gitter.im/intercooler-js/Lobby?utm_source=badge&utm_medium=badge&utm_campaign=pr-badge&utm_content=badge)