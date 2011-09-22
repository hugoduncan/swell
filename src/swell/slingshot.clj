(ns swell.slingshot
  "Slingshot bindings for swell."
  (:require
   [slingshot.core :as slingshot]
   [swell.spi :as spi]))

(defmacro try-with-restarts
  [restarts & body]
  `(slingshot/try+
    ~@body
    (catch {nil :swell/invoke-restart} e#
      (if (~restarts (:restart e#))
        (apply spi/invoke-restart (:restart e#) (:args e#))
        (throw (-> ~'&throw-context meta :throwable))))))

(defmacro with-exception-scope [restarts & body]
  `(spi/with-scope-restarts [~@restarts]
    (try-with-restarts (spi/scope-restarts)
      ~@body)))

(defn catches?
  "Provides run-time version of slingshot.core/catch->cond"
  [throw-context selector]
  (cond
   (class? selector) (instance? selector (:obj throw-context))

   (#'slingshot/typespec? selector)
   (let [[hierarchy parent] (first selector)]
     (if (nil? hierarchy)
       (isa? (type (:obj throw-context)) parent)
       (isa? hierarchy (type (:obj throw-context)) parent)))

   :else
   (selector (:obj throw-context))))

(defn matching-handler
  "Provides run-time version of slingshot.core/catch->cond"
  [throw-context [selector handler]]
  (when (catches? throw-context selector)
    handler))

(defn handle
  "Try and handle an exception by checking for swell handlers"
  [context-map]
  (when-let [handler (some
                      (partial matching-handler context-map)
                      (spi/handlers))]
    [nil
     (try-with-restarts
       (spi/scope-restarts)
       (handler context-map))]))

(defn on-catch
  [context-map]
  (or
   (handle context-map)
   [context-map nil]))

(alter-var-root #'slingshot/*catch-hook* (fn [a b] b) on-catch)

(defn unwind-to-invoke-restart
  [restart & args]
  (slingshot/throw+
   ^{:type :swell/invoke-restart}
   {:restart restart :args args}))
