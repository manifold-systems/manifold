# Interface delegation with links & parts
  
> **⚠ Experimental Feature**
 
The `manifold-delegation` project is a compiler plugin providing language support for call forwarding and true delegation.
These features are an experimental effort toward interface composition as a practical alternative to class inheritance.

Use `@link` to automatically transfer calls on unimplemented interface methods to fields in the same class.

* Choose between call forwarding and true delegation with `@part`
* Override linked interface methods (solves [the Self problem](https://web.media.mit.edu/~lieber/Lieberary/OOP/Delegation/Delegation.html))
* Share super interface implementations (solves [the Diamond problem](https://en.wikipedia.org/wiki/Multiple_inheritance#The_diamond_problem))
* Configure class implementation dynamically

# Basic usage
```java
class MyClass implements MyInterface {
  @link MyInterface myInterface; // transfers calls on MyInterface to myInterface

  public MyClass(MyInterface myInterface) {
    this.myInterface = myInterface; // dynamically configure behavior
  }
}
```

# Forwarding

Generally, the difference between forwarding and true delegation is that forwarding does not fully support virtual methods,
while true delegation does. This difference is at the heart of _the Self problem_ (aka _broken delegation_).

In terms of this project, delegation works exclusively with `@part` classes. If a `@part` class is assigned to a `@link`
field, the link uses delegation and fully supports polymorphic calls. Otherwise, the link uses forwarding.

```java
class MyStudent implements Student {
  @link Person person;
  private final String major;
  
  public MyStudent(Person person, String major) {
    this.person = person;
    this.major = major;
  }

  public String getTitle() {return "Student";}
  public String getMajor() {return major;}
}

interface Person {
  String getName();
  String getTitle();
  String getTitledName();
}
interface Student extends Person {
  String getMajor();
}
```
With `@link` on the `person` field MyStudent automatically transfers calls to unimplemented Person methods to the field.
```java
class MyPerson implements Person {
  private final String name;

  public PersonPart(String name) {this.name = name;}
  
  public String getName() {return name;}
  public String getTitle() {return "Person";}
  public String getTitledName() {return getTitle() + " " + getName();}
}

MyPerson person = new MyPerson("Milton");
MyStudent student = new MyStudent(person, "Metallurgy");
out.println(student.getTitledName());
```
Since MyPerson is _not_ annotated with `@part` forwarding is used to transfer interface method calls.

But with forwarding, since the calls are one-way tickets, the call to `student.getTitledName()` results in:
```text
    Person Milton
```
With forwarding the call to `getTitle()` from MyPerson is not polymorphic with respect to the link established
in MyStudent.

Generally, this behavior can be viewed as positive or negative, depending on the desired call transfer model.

# Delegation

If the field's value is a `@part` class, the Person methods are called using _delegation_. Unlike forwarding, delegation
enables polymorphic calls; MyStudent can override Person methods so that the implementation of Person defers to MyStudent.
Essentially, `@part` solves _the Self problem_.
```java
@part class PersonPart implements Person {
  private final String name;

  public PersonPart(String name) {this.name = name;}
  
  public String getName() {return name;}
  public String getTitle() {return "Person";}
  public String getTitledName() {return getTitle() + " " + getName();}
}

PersonPart person = new PersonPart("Milton");
MyStudent student = new MyStudent(person, "Metallurgy");
out.println(student.getTitledName());
```
The call to `student.getTitledName()` results in:
```text
    Student Milton
```
This is because PersonPart is a `part` class, which enables polymorphic calls from linked parts. This means inside PersonPart
`this` refers to MyStudent in terms of the Person interface. Thus, the call to `getTitle()` dispatches to MyStudent.

If PersonPart were _not_ annotated with `@part`, the result would have been:
```text
    Person Milton
```
Because, without `@part` the call is forwarded, not delegated.

## Default methods

Consider `getTitledName()` as a default method in Person instead of an implementation in PersonPart.
```java
interface Person {
  String getName();
  String getTitle();
  default String getTitledName() {return getTitle() + " " + getName();}
}
```  
Calls must behave identically regardless of where the method is implemented; polymorphism must be preserved when using `part`
classes. As such the call to `student.getTitledName()` results just as before:
```text
    Student Milton
```    
Inside Person `this` refers to MyStudent even when called from PersonPart.

# Diamonds

When super interfaces overlap, a "diamond" relationship results. This is known as _the diamond problem_.
```text
         Person
           ▲▲
      ┌────┘└────┐
   Student    Teacher
      ▲          ▲
      └────┐┌────┘
           TA
```
Should TA use Student's Person or Teacher's? Use `@link(share=true)` to resolve the ambiguity.
```java
interface Teacher extends Person {
  String getDepartment();
}

interface TA extends Student, Teacher {
}

@part class TeacherPart implements Teacher {
  @link Person person;
  private final String department;

  public TeacherPart(Person person, String department) {
    this.person = person;
    this.department = department;
  }
  public String getTitle() {return "Teacher";}
}

@part class TaPart implements TA {
  @link(share=true) Student student; // use student as the person
  @link Teacher teacher;

  public TaPart(Student student) {
    this.student = student;
    this.teacher = new Teacher(student, "Math"); // the student is the teacher
  }
}
```
The student is the teacher, so TaPart shares the link to student with `@link(share=true)` and student is passed along to
the Teacher constructor.

>Note, `part` classes are not required with `@link(share=true)`; it works with both forwarding and delegation.

# Structural interfaces

Sometimes the class you want to link to doesn't implement the interface you want to expose. If you don't control the implementation
of the class, you can define a [structural interface](https://github.com/manifold-systems/manifold/tree/master/manifold-deps-parent/manifold-ext#structural-interfaces-via-structural)
to map specific methods you want to expose.
```java
@Structural
interface LimitedList<E> {
  boolean add(E e);
  E get( int index );
  boolean contains(Object e);
}

class MyLimitedList<E> implements LimitedList<E> {
  @link LimitedList _list;

  public MyLimitedList(LimitedList  list) {
    _list = list;
  }
}

// ArrayList structurally satisfies LimitedList
LimitedList<String> limitedList = new MyLimitedList<>((LimitedList<String>)new ArrayList<>());
limitedList.add("hi");
assertTrue(limitedList.contains("hi"));
assertEquals("hi", limitedList.get(0));
```

# Inheritance

Delegation involves a compound object consisting of a root linking object and its graph of linked `part` classes. Inside
this compound object linked interface calls are always applied to the root object and never to the linked parts; `this`
must always refer to the root in terms of the interfaces defined by the links.

If any of the linked parts are allowed to directly refer to another linked part, delegation is broken. Polymorphic calling
is compromised because a direct reference to another link bypasses the root, which must always dispatch all interface calls.

Therefore, `part` classes may only subclass other `part` classes to maintain delegation integrity.