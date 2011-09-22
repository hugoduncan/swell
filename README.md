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
    (require '[swell.api :as api])

    (swell/handler-bind
      [keyword? :restart2]
      (swell/restart-case
          [restart1 (fn [_] :yes)
           :restart2 (fn [_] :no)]
        (slingshot/throw+ ::e)))
```

## Status

Not released, no jar pushed yet. Requires slingshot from
`issue-3-implement-throw-hook` branch of my slingshot clone.

## License

Copyright (C) 2011 Hugo Duncan

Distributed under the Eclipse Public License.
