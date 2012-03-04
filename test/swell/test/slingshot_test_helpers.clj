(ns swell.test.slingshot-test-helpers
  (:use clojure.test))

(defn slingshot-exception-class
  "Return the best guess at a slingshot exception class."
  []
  (try
    (Class/forName "slingshot.Stone")
    (catch Exception _
      (let [ei (Class/forName "slingshot.ExceptionInfo")]
        (if (and (resolve 'clojure.core/ex-info)
                 (resolve 'clojure.core/ex-data))
          (Class/forName "clojure.lang.ExceptionInfo")
          ei)))))

(defmacro is-thrown-slingshot?
  "clojure.test clause for testing that a slingshot exception is thrown."
  [& body]
  `(is (~'thrown? ~(slingshot-exception-class) ~@body)))

(defmacro is-thrown-with-msg-slingshot?
  "clojure.test clause for testing that a slingshot exception is thrown."
  [& body]
  `(is (~'thrown-with-msg? ~(slingshot-exception-class) ~@body)))

(def ex-data
  (or
   (try
     (require 'slingshot.support)
     (ns-resolve 'slingshot.support 'ex-data)
     (catch Exception _))
   (fn ex-data [e] (.data e))))

(defn slingshot-object
  "Return the object thrown using slingshot."
  [e]
  (let [data (ex-data e)]
    (or (:object data)
        (:obj data))))
