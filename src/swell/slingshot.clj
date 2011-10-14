(ns swell.slingshot
  "Slingshot bindings for swell."
  (:require
   [slingshot.core :as slingshot]
   [slingshot.support :as support]
   [swell.spi :as spi]))

(defmacro try-with-restarts
  [restarts & body]
  `(slingshot/try+
    ~@body
    (catch #(isa? :swell/invoke-restart (type %)) e#
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
   (class? selector) (instance? selector (:object throw-context))
   (vector? selector) (let [[key val] selector]
                        (= (get (:object throw-context) key) val))
   :else
   (selector (:object throw-context))))

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
    [true
     (try-with-restarts
       (spi/scope-restarts)
       (handler context-map))]))

(defn unhandled-hook
  [context-map]
  (when-let [[restart & args] (spi/on-unhandled-hook
                               (-> context-map meta :throwable))]
    [true
     (try-with-restarts
       (spi/scope-restarts)
       (apply (spi/find-restart restart) args))]))

(defn on-catch
  [context-map]
  (if-let [[handled return-value] (or
                                   (handle context-map)
                                   (unhandled-hook context-map))]
    (vary-meta context-map assoc :catch-hook-return return-value)
    context-map))

(alter-var-root #'support/*catch-hook* (fn [a b] b) on-catch)

(defn unwind-to-invoke-restart
  [restart & args]
  (slingshot/throw+
   ^{:type :swell/invoke-restart}
   {:restart restart :args args}))
