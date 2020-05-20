---
layout: default
---

# Roadmap
 
![roadmap](http://manifold.systems/images/roadmap.jpg)
 
## On the workbench

Focus is _**Manifold on Android**_. This work includes full compiler, runtime, and Android Studio support.

Related issues:
* [#181](https://github.com/manifold-systems/manifold/issues/181)
* [#77](https://github.com/manifold-systems/manifold/issues/77)
* [#10](https://github.com/manifold-systems/manifold/issues/10)

More generally, these changes contribute toward the option of using Manifold in a _pure static_ mode, where the runtime
is ultra-slim, and none of Manifold's dynamic behavior is available.
 
>**Warning:** This set of changes is going to break some existing APIs. For instance, some API classes of the form
>`manifold.xxx.api.XxxFoo` will become `manifold.xxx.rt.api.XxxFoo`.
>If you are concerned, please open a discussion in Manifold [Slack Group](https://join.slack.com/t/manifold-group/shared_invite/zt-e0bq8xtu-93ASQa~a8qe0KDhOoD6Bgg).
 
## On the pile (in no particular order)
 
#### Operator overloading enhancements
* [#126](https://github.com/manifold-systems/manifold/issues/126)

#### New schema type manifolds 
* [#111](https://github.com/manifold-systems/manifold/issues/111)

#### Manifold "inliner" (aka De-Manifold) tool
* [#95](https://github.com/manifold-systems/manifold/issues/95)

#### Default parameter values
* [#93](https://github.com/manifold-systems/manifold/issues/93)

#### Manifold plugin for VS Code (which is Eclipse)
* [#142](https://github.com/manifold-systems/manifold/issues/142)

#### Manifold plugin for Eclipse
* [#18](https://github.com/manifold-systems/manifold/issues/18)

