(ns swell.api
  "A CL style restart library for clojure."
  (:require
   [swell.internal :as internal]))

(defmacro defmacro-fwd
  [sym ns]
  (let [args (gensym "args")]
    `(defmacro ~sym [& ~args]
       `(~'~(symbol (name ns) (name sym)) ~@~args))))

(try
  (require 'swell.slingshot)
  (defmacro-fwd with-exception-scope swell.slingshot)
  (def unwind-to-invoke-restart
    (ns-resolve 'swell.slingshot 'unwind-to-invoke-restart))
  (def invoke-restart
    (ns-resolve 'swell.slingshot 'unwind-to-invoke-restart))
  (catch Exception e
    (throw (RuntimeException. "No exception handling lib available" e))))

(defn- restart-handlers-for
  [bindings]
  (mapcat
   (fn [[s r]] [s (if (and (sequential? r) (not (= `quote (first r))))
                    r
                    `(fn [e#] (invoke-restart ~r)))])
   (partition 2 bindings)))

(defmacro handler-bind
  "Executes body in a dynamic environment where the indicated handler bindings
are in effect. Each binding value is a funtions of one argument (the
exception), or a restart name (which will invoke the restart with no
arguments.

    (handler-bind
      [keyword? :restart2
       RuntimeException 'restart1]
      (f))"
  [bindings & body]
  `(binding [internal/*handlers* (merge
                                  internal/*handlers*
                                  (hash-map ~@(restart-handlers-for bindings)))]
     ~@body))

(defmacro restart-case
  "Evaluates body in a dynamic environment where control may be transferred to
the restarts specified.

Each binding is from something Named to a function with no arguments
that is passed the exeception.

    (restart-case
      [:return-3 (fn [] 3)
       'return-4 (fn [] 4)]
      (do-something))"
  [bindings & body]
  (let [restarts (into {}
                       (map
                        (fn [[s f]] [(list 'quote s) f])
                        (partition 2 bindings)))]
    `(binding [internal/*restarts* (merge
                                    internal/*restarts*
                                    ~restarts)]
       (with-exception-scope ~(keys restarts)
         ~@body))))

(def ^{:doc "Finds restart in the current dynamic environment."
       :arglists (:arglists (meta #'swell.spi/find-restart))}
  find-restart #'swell.spi/find-restart)
