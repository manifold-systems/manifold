# Manifold : Science

>Warning: **Experimental Feature**

Use the `manifold-science` framework to type-safely incorporate units & measures into your application. `Dimension`
type-safely models quantities. The framework provides comprehensive support for physical quantities such as `Length`,
`Mass`, and `Temperature`, as well as abstract quantities such as `StorageCapacity` and `Money`. Together with
[unit expressions](https://github.com/manifold-systems/manifold/tree/master/manifold-deps-parent/manifold-ext#unit-expressions)
these classes provide an expressive units & measures foundation for a variety of applications.

Conveniently and type-safely express physical quantities of any type and unit of measure using *unit expressions*:

```java
import static manifold.science.util.UnitConstants.*; // kg, m, s, ft, etc.
...
Force f = 5 kg * 9.807 m/s/s; // result: 49.035 Newtons

Area space = (20ft + 2in) * (37ft + 7.5in); // result: 758 37/48 ft²
```
>Note dimensions work directly in arithmetic expressions by integrating with [operator overloading](https://github.com/manifold-systems/manifold/tree/master/manifold-deps-parent/manifold-ext#operator-overloading)
and [unit expressions](https://github.com/manifold-systems/manifold/tree/master/manifold-deps-parent/manifold-ext#unit-expressions),
both of these features are provided by the [`manifold-ext`](https://github.com/manifold-systems/manifold/tree/master/manifold-deps-parent/manifold-ext#unit-expressions)
dependency.

 
## Table of Contents
* [Dimensions & Units API](#dimensions--units-api)
* [Library](#library)
* [Rational Numbers](#rational-numbers)
* [IDE Support](#ide-support)
* [Building](#building)
* [License](#license)
* [Versioning](#versioning)
* [Author](#author)


# Dimensions & Units API

Physical quantities have a fundamental *dimension* that is independent of units of measurement. The primary physical
dimensions are: length, mass, time, electrical charge, temperature and luminous intensity. Derived physical dimensions
include area, volume, velocity, force, energy, power and so on. The `manifold-science` framework provides an API to
model both primary and derived dimensions. Using this API the framework supplies a class library consisting of all of
the primary dimensions and many of the derived mechanical dimensions used with classical physics computations.

## API

The foundational API of the science framework consists of a small set of base classes and interfaces defined in the
`manifold.science.api` package.  

## Measures

`Dimension` interface is the root of the API. It models a dimension as having a unitless measure represented as a
`Rational` value as well as common [operator methods](https://github.com/manifold-systems/manifold/tree/master/manifold-deps-parent/manifold-ext#operator-overloading)
for arithmetic operations. Since common dimensions define a unit of measure you will rarely implement `Dimension`
directly, instead you should extend `AbstractMeasure`.  This class adds a unit of measure to `Dimension` so you can
extend it to reuse unit functionality common to all physical quantities.

Instances of this class store the value (or magnitude) of the measure in terms of *base units*. Thus all arithmetic on
measures is performed using base units, which permits measures of differing input units to work in calculations. A
measure instance also maintains a *display unit*, which is used for display purposes and for working with other systems
requiring specific units.

For example, the `Length` dimension is defined like this:

```java
public final class Length extends AbstractMeasure<LengthUnit, Length> {
  public Length(Rational value, LengthUnit unit, LengthUnit displayUnit) {
    super(value, unit, displayUnit);
  }

  public Length(Rational value, LengthUnit unit) {
    this( value, unit, unit );
  }

  @Override
  public LengthUnit getBaseUnit() {
    return LengthUnit.BASE;
  }
...
}
```   

## Units

The `Unit` interface provides a base abstraction for unit types. Primary unit types `Length`, `Mass`, `Time`, `Charge`,
and `Temperature` implement `Unit` indirectly via `AbstractPrimaryUnit`. For instance, `LengthUnit`:

```java
public final class LengthUnit extends AbstractPrimaryUnit<Length, LengthUnit> {
  ...
  // SI Units
  public static final LengthUnit Femto = get(FEMTO, "Femtometer", "fm");
  public static final LengthUnit Pico = get(PICO, "Picometer", "pm");
  public static final LengthUnit Angstrom = get(1e-10r, "Angstrom", "Å");
  public static final LengthUnit Nano = get(NANO, "Nanometer", "nm");
  public static final LengthUnit Micro = get(MICRO, "Micrometre", "µm");
  public static final LengthUnit Milli = get(MILLI, "Millimeter", "mm");
  public static final LengthUnit Centi = get(CENTI, "Centimeter", "cm");
  public static final LengthUnit Deci = get(DECI, "Decimeter", "dm");
  public static final LengthUnit Meter = get(1r, "Meter", "m");
  public static final LengthUnit Kilometer = get(KILO, "Kilometer", "km");
  public static final LengthUnit Megameter = get(KILO.pow(2), "Megameter", "Mm");
  public static final LengthUnit Gigameter = get(KILO.pow(3), "Gigameter", "Gm");
  public static final LengthUnit Terameter = get(KILO.pow(4), "Terameter", "Tm");

  // US Standard
  public static final LengthUnit Caliber = get(0.000254r, "Caliber", "cal.");
  public static final LengthUnit Inch = get(0.0254r, "Inch", "in");
  public static final LengthUnit Foot = get(12 * 0.0254r, "Foot", "ft");
  public static final LengthUnit Yard = get(3 * 12 * 0.0254r, "Yard", "yd");
  public static final LengthUnit Rod = get(5.0292r, "Rod", "rd");
  public static final LengthUnit Chain = get(20.1168r, "Chain", "ch");
  public static final LengthUnit Furlong = get(201.168r, "Furlong", "fur");
  public static final LengthUnit Mile = get(1609.344r, "Mile", "mi");

  // Navigation
  public static final LengthUnit NauticalMile = get(1852r, "NauticalMile", "n.m.");

  // Very large
  public static final LengthUnit IAU = get(1.49597870e11r, "IAU-length", "au");
  public static final LengthUnit LightYear = get(9.460730473e+15r, "LightYear", "ly");

  // Very small
  public static final LengthUnit Planck = get(1.61605e-35r, "Planck-length", "ℓP");

  // Ancient
  public static final LengthUnit Cubit = get(0.4572r, "Cubit", "cbt");
  ...
}
```

Derived unit types consist of products or quotients of other unit types, so they extend `AbstractProductUnit` and
`AbstractQuotientUnit`.  For instance, `VelocityUnit` is the quotient of `LengthUnit` and `TimeUnit`, therefore it
derives from `AbstractQuotientUnit`:

```java
final public class VelocityUnit extends AbstractQuotientUnit<LengthUnit, TimeUnit, Velocity, VelocityUnit> {
  
  public static final VelocityUnit BASE = get( Meter, Second );

  ...
}
```  

## Operator Methods

You can use *all* Dimensions and Units directly in arithmetic, relational, and [unit (or "binding") expressions](https://github.com/manifold-systems/manifold/tree/master/manifold-deps-parent/manifold-ext#unit-expressions).

The `Length` class, for example, implements method operators for all arithmetic, relational, and unit operators. This is
what allows you to write expressions like this:
```java
 *   // commonly used unit abbreviations e.g., m, ft, hr, mph, etc.
 *   import static manifold.science.util.UnitConstants.*;
 *   ...
 *   Length l = 5m; // 5 meters
 *   Length height = 5 ft + 9.5 in;
 *   Area room = 20 ft * 15.5 ft;
 *   Length distance = 80 mph * 2.3 hr;
``` 

See the documentation for [operator overloading](https://github.com/manifold-systems/manifold/tree/master/manifold-deps-parent/manifold-ext#operator-overloading)
and [unit expressions](https://github.com/manifold-systems/manifold/tree/master/manifold-deps-parent/manifold-ext#unit-expressions)
for detail about working with these features.
 
# Library

All of the primary dimensions and many of the derived dimensions are directly provided in the `manifold.science`
package. These include:

* `Acceleration` & `AccelerationUnit`
* `Angle` & `AngleUnit`
* `Area` & `AreaUnit`
* `Capacitance` & `CapacitanceUnit`
* `Charge` & `ChargeUnit`
* `Conductance` & `ConductanceUnit`
* `Current` & `CurrentUnit`
* `Density` & `DensityUnit`
* `Energy` & `EnergyUnit`
* `Force` & `ForceUnit`
* `Frequency` & `FrequencyUnit`
* `HeatCapacity` & `HeatCapacityUnit`
* `Inductance` & `InductanceUnit`
* `Length` & `LengthUnit`
* `MagneticFlux` & `MagneticFluxUnit`
* `MagneticFluxDensity` & `MagneticFluxDensityUnit`
* `Mass` & `MassUnit`
* `MetricScaleUnit`
* `Momentum` & `MomentumUnit`
* `Potential` & `PotentialUnit`
* `Power` & `PowerUnit`
* `Pressure` & `PressureUnit`
* `Resistance` & `ResistanceUnit`
* `SolidAngle` & `SolidAngleUnit`
* `StorageCapacity` & `StorageCapacityUnit`
* `Temperature` & `TemperatureUnit`
* `Time` & `TimeUnit`
* `Velocity` & `VelocityUnit`
* `Volume` & `VolumeUnit`

A small library of experimental vector classes are available in the `manifold.science.vector` package. These classes
support vector math and can be used directly within arithmetic expressions: 

* `Vector`
* `LengthVector`
* `TimeVector`
* `VelocityVector`
 
Utility classes providing useful constants are available in the `manifold.science.util` package, these include:

* `AngleConstants`
* `CoercionConstants`
* `DimensionlessConstants`
* `MetricFactorConstants`
* `Rational`
* `UnitConstants`

# Rational Numbers

The `Rational` class in the `manifold.science.util` package is similar to `BigDecimal` in that it can model rational
numbers with arbitrary precision. However `Rational` differs from `BigDecimal` in that it models a rational number as 
the quotient of two `BigIntenger` numbers.  This has the advantage of maintaining what is otherwise a repeating decimal
for values such as `1/3`. For instance, dividing a number by 3 then later multiplying that number by 3 should result
in the original number without rounding errors. While you can handle rounding with `BigDecimal`, using `Rational` can
be less error prone in some cases especially when working with equations. For this reason, all the Dimensions and Units
defined in the `manifold.science` package use `Rational`. [Feedback](https://github.com/manifold-systems/manifold/issues)
on this subject is welcome!

>Note as a performance measure this class does *not* maintain its value in reduced form. You must call `reduce()` to get
a separate instance for the reduced form. Call `isReduced()` to determine if an instance is in reduced form.

Use the `CoercionConstants` and `MetricScaleUnit` classes to conveniently use literal values as `Rational` numbers:
```java
  import static manifold.science.measures.MetricScaleUnit.M;
  import static manifold.science.util.CoercionConstants.r;
  ...
  Rational pi = 3.14159r;
  Rational yocto = "1/1000000000000000000000000"r;
  Rational fiveMillion = 5M;
```
`Rational` implements arithmetic, negation, and relational operators via [operator overloading](https://github.com/manifold-systems/manifold/tree/master/manifold-deps-parent/manifold-ext#operator-overloading)
provided by the manifold-ext dependency. Operator overloading lets you use `Rataional` numbers directly in arithmetic,
negation, and relational expressions:
```java
Rational oneThird = 1r/3;
Rational circumference = 3.14159r * 5.27r;

if (oneThird > 1r/4) {...}
if (3.14159r == pi) {...}
``` 

Note `Dimension` implements the `==` and `!=` operators with `compareTo()`.  Read more about implementing relational
operators with [operator overloading](https://github.com/manifold-systems/manifold/tree/master/manifold-deps-parent/manifold-ext#operator-overloading).

# IDE Support 

Manifold is best experienced using [IntelliJ IDEA](https://www.jetbrains.com/idea/download).

## Install

Get the [Manifold plugin](https://plugins.jetbrains.com/plugin/10057-manifold) for IntelliJ IDEA directly from IntelliJ via:

<kbd>Settings</kbd> ➜ <kbd>Plugins</kbd> ➜ <kbd>Marketplace</kbd> ➜ search: `Manifold`

<p><img src="http://manifold.systems/images/ManifoldPlugin.png" alt="echo method" width="60%" height="60%"/></p>

## Sample Project

Experiment with the [Manifold Sample Project](https://github.com/manifold-systems/manifold-sample-project) via:

<kbd>File</kbd> ➜ <kbd>New</kbd> ➜ <kbd>Project from Version Control</kbd> ➜ <kbd>Git</kbd>

<p><img src="http://manifold.systems/images/OpenSampleProjectMenu.png" alt="echo method" width="60%" height="60%"/></p>

Enter: <kbd>https://github.com/manifold-systems/manifold-sample-project.git</kbd>

<p><img src="http://manifold.systems/images/OpenSampleProject.png" alt="echo method" width="60%" height="60%"/></p>

Use the [plugin](https://plugins.jetbrains.com/plugin/10057-manifold) to really boost your productivity. Use code
completion to conveniently access extension methods. Create extension methods using a convenient user interface. Make
changes to your extensions and use the changes immediately, no compilation! Use extensions provided by extension library
dependencies. Find usages of any extension. Use the `Range` API and unit expressions with complete type-safety.

# Building

## Building this project

The `manifold-science` project is defined with Maven.  To build it install Maven and run the following command.

```
mvn compile
```

## Using this project

The `manifold-science` dependency works with all build tooling, including Maven and Gradle. It also works with Java versions
8 - 12.

Here are some sample build configurations references.

>Note you can replace the `manifold-science` dependency with [`manifold-all`](https://github.com/manifold-systems/manifold/tree/master/manifold-all) as a quick way to gain access to all of
Manifold's features.

## Gradle

Here is a sample `build.gradle` script. Change `targetCompatibility` and `sourceCompatibility` to your desired Java
version (8 - 12), the script takes care of the rest.  
```groovy
plugins {
    id 'java'
}

group 'systems.manifold'
version '1.0-SNAPSHOT'

targetCompatibility = 11
sourceCompatibility = 11

repositories {
    jcenter()
    maven { url 'https://oss.sonatype.org/content/repositories/snapshots/' }
}

dependencies {
    compile group: 'systems.manifold', name: 'manifold-science', version: '2019.1.23'
    testCompile group: 'junit', name: 'junit', version: '4.12'
                       
    if (JavaVersion.current() == JavaVersion.VERSION_1_8) {
        // tools.jar dependency (for Java 8 only), primarily to support structural typing without static proxies.
        // Thus if you are *not* using structural typing, you **don't** need tools.jar
        compile files( "${System.properties['java.home']}/../lib/tools.jar" )
    }
    // Add manifold to -processorpath for javac
    annotationProcessor group: 'systems.manifold', name: 'manifold-science', version: '2019.1.23'
}

if (JavaVersion.current() != JavaVersion.VERSION_1_8 &&
    sourceSets.main.allJava.files.any {it.name == "module-info.java"}) {
    tasks.withType(JavaCompile) {
        // if you DO define a module-info.java file:
        options.compilerArgs += ['-Xplugin:Manifold', '--module-path', it.classpath.asPath]
    }
} else {
    tasks.withType(JavaCompile) {
        // If you DO NOT define a module-info.java file:
        options.compilerArgs += ['-Xplugin:Manifold']
    }
}

tasks.compileJava {
    classpath += files(sourceSets.main.output.resourcesDir) //adds build/resources/main to javac's classpath
    dependsOn processResources
}
tasks.compileTestJava {
    classpath += files(sourceSets.test.output.resourcesDir) //adds build/resources/test to test javac's classpath
    dependsOn processTestResources
}
```
Use with accompanying `settings.gradle` file:
```groovy
rootProject.name = 'MyExtProject'
```

## Maven

### Java 8

```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/maven-v4_0_0.xsd">
    <modelVersion>4.0.0</modelVersion>

    <groupId>com.example</groupId>
    <artifactId>my-ext-app</artifactId>
    <version>0.1-SNAPSHOT</version>

    <name>My Java App</name>

    <properties>
        <!-- set latest manifold version here --> 
        <manifold.version>2019.1.23</manifold.version>
    </properties>
    
    <dependencies>
        <dependency>
            <groupId>systems.manifold</groupId>
            <artifactId>manifold-science</artifactId>
            <version>${manifold.version}</version>
        </dependency>
    </dependencies>

    <!--Add the -Xplugin:Manifold argument for the javac compiler-->
    <build>
        <plugins>
            <plugin>
                <groupId>org.apache.maven.plugins</groupId>
                <artifactId>maven-compiler-plugin</artifactId>
                <version>3.8.0</version>
                <configuration>
                    <source>8</source>
                    <target>8</target>
                    <encoding>UTF-8</encoding>
                    <compilerArgs>
                        <!-- Configure manifold plugin-->
                        <arg>-Xplugin:Manifold</arg>
                    </compilerArgs>
                </configuration>
            </plugin>
        </plugins>
    </build>

    <profiles>
        <!-- tools.jar dependency (for Java 8 only), primarily to support structural typing without static proxies.
             Thus if you are not using structural typing, you **don't** need tools.jar -->
        <profile>
            <id>internal.tools-jar</id>
            <activation>
                <file>
                    <exists>* ${java.home}/../lib/tools.jar</exists>
                </file>
            </activation>
            <dependencies>
                <dependency>
                    <groupId>com.sun</groupId>
                    <artifactId>tools</artifactId>
                    <version>1.8.0</version>
                    <scope>system</scope>
                    <systemPath>* ${java.home}/../lib/tools.jar</systemPath>
                </dependency>
              </dependencies>
        </profile>
    </profiles>
</project>
```

### Java 9 or later
```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/maven-v4_0_0.xsd">
    <modelVersion>4.0.0</modelVersion>

    <groupId>com.example</groupId>
    <artifactId>my-ext-app</artifactId>
    <version>0.1-SNAPSHOT</version>

    <name>My Java App</name>

    <properties>
        <!-- set latest manifold version here --> 
        <manifold.version>2019.1.23</manifold.version>
    </properties>
    
    <dependencies>
        <dependency>
            <groupId>systems.manifold</groupId>
            <artifactId>manifold-science</artifactId>
            <version>${manifold.version}</version>
        </dependency>
    </dependencies>

    <!--Add the -Xplugin:Manifold argument for the javac compiler-->
    <build>
        <plugins>
            <plugin>
                <groupId>org.apache.maven.plugins</groupId>
                <artifactId>maven-compiler-plugin</artifactId>
                <version>3.8.0</version>
                <configuration>
                    <source>11</source>
                    <target>11</target>
                    <encoding>UTF-8</encoding>
                    <compilerArgs>
                        <!-- Configure manifold plugin-->
                        <arg>-Xplugin:Manifold</arg>
                    </compilerArgs>
                    <!-- Add the processor path for the plugin (required for Java 9+) -->
                    <annotationProcessorPaths>
                        <path>
                            <groupId>systems.manifold</groupId>
                            <artifactId>manifold-science</artifactId>
                            <version>${manifold.version}</version>
                        </path>
                    </annotationProcessorPaths>
                </configuration>
            </plugin>
        </plugins>
    </build>
</project>
```

# License

## Open Source
Open source Manifold is free and licensed under the [Apache 2.0](http://www.apache.org/licenses/LICENSE-2.0) license.  

## Commercial
Commercial licenses for this work are available. These replace the above ASL 2.0 and offer 
limited warranties, support, maintenance, and commercial server integrations.

For more information, please visit: http://manifold.systems//licenses

Contact: admin@manifold.systems

# Versioning

For the versions available, see the [tags on this repository](https://github.com/manifold-systems/manifold/tags).

# Author

* [Scott McKinney](mailto:scott@manifold.systems)
