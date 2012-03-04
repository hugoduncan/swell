# swell

A CL style restart library for clojure.

Given that the JVM has exceptions, which can be thrown by non-clojure code,
restarts in clojure will be limited to catch points defined within clojure.
Swell initially targets catch points defined using
[slingshot](https://github.com/scgilardi/slingshot).

For an introduction to restarts in CL, see [PCL](http://www.gigamonkeys.com/book/beyond-exception-handling-conditions-and-restarts.html).

## Usage

Provides `handler-bind`, `restart-case` and `invoke-restart` which are clojure
versions of their CL brethren.

```clojure
    (require '[swell.api :as swell])

    (let [f (fn []
            (swell/restart-case
             [restart1 (fn [] 1)]
             (inc
              (swell/restart-case
               [:restart2 (fn [] 3)]
               (slingshot/throw+ ::e)))))]
      (is (= 4 (swell/handler-bind [keyword? :restart2] (f))))
      (is (= 1 (swell/handler-bind [keyword? 'restart1] (f)))))
```

To use in your project add the following to your project.clj `:dependencies`:

```clojure
    [swell "0.1.0-SNAPSHOT"]
```

## project.clj

Swell is released to clojars. To use in your project, add the following to your
`dependencies`:

```clj
[swell "0.1.0"]
```

## pom.xml
To use in your pom, add the following to your `<dependencies>`:

```xml
<dependency>
  <groupId>swell</groupId>
  <artifactId>swell</artifactId>
  <version>0.1.0</version>
</dependency>
```

and the following to your `<repositories>`:

``` xml
<repository>
  <id>clojars</id>
  <url>http://clojars.org/repo</url>
</repository>
```

## License

Copyright (C) 2011, 2012 Hugo Duncan

Distributed under the Eclipse Public License.
