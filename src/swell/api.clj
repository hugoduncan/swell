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
  (catch Exception e
    (throw (RuntimeException. "No exception handling lib available" e))))

(declare default-interactive-restart-fn)

(def
  ^{:dynamic true
    :doc "Interactive restart fn"}
  *interactive-restart-fn* #'default-interactive-restart-fn)

(defn- restart-handlers-for
  [bindings]
  (mapcat
   (fn [[s r]] [s (if (and (sequential? r) (not (= `quote (first r))))
                    r
                    `(fn [e#] (invoke-restart ~r e#)))])
   (partition 2 bindings)))

(defmacro handler-bind
  "Executes body in a dynamic environment where the indicated handler bindings
   are in effect."
  [bindings & body]
  `(binding [internal/*handlers* (merge
                                  internal/*handlers*
                                  (hash-map ~@(restart-handlers-for bindings)))]
     ~@body))

(defmacro restart-case
  "Evaluates body in a dynamic environment where control may be transferred to
   the restarts specified"
  [bindings & body]
  `(binding [internal/*restarts* (merge
                                  internal/*restarts*
                                  ~(into {}
                                         (map
                                          (fn [[s f]] [(list 'quote s) f])
                                          (partition 2 bindings))))]
     (with-exception-scope
       ~@body)))

(def find-restart #'swell.spi/find-restart)
(def invoke-restart #'swell.spi/invoke-restart)

(defn default-interactive-restart-fn
  [restart]
  (if-let [f (or (find-restart restart) restart)]
    (do
      ;; TODO check and prompt for restart arguments
      (f))
    ))

(defn on-catch [])
(defn on-throw [])
