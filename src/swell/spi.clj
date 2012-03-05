(ns swell.spi
  "Service provider interface for swell"
  (:require
   [swell.internal :as internal]))

(declare default-interactive-restart-fn)

(def
  ^{:dynamic true
    :doc "Interactive restart fn"}
  *interactive-restart-fn* #'default-interactive-restart-fn)

(defn handlers
  "Map of currently bound handlers"
  [] internal/*handlers*)

(defn restarts
  "Map of currently bound restarts"
  [] internal/*restarts*)

(defn scope-restarts
  "Set of currently bound restarts"
  [] internal/*scope-restarts*)

(defn find-restart
  "Finds restart in the current dynamic environment."
  [restart]
  (get internal/*restarts* restart (when (fn? restart) restart)))

(defn invoke-restart
  "Calls the function associated with restart, passing arguments to it. Restart
   must be valid in the current dynamic environment."
  [restart args]
  (if-let [f (find-restart restart)]
    (apply f args)
    (throw
     (RuntimeException.
      (format "Attempt to call unbound restart %s. Known restarts %s."
              restart (vec (keys internal/*restarts*)))))))

(defn default-interactive-restart-fn
  [restart]
  (if-let [f (or (find-restart restart) restart)]
    (do
      ;; TODO check and prompt for restart arguments
      (f))
    ))

(defn invoke-restart-interactively
  [restart]
  (*interactive-restart-fn* restart))

(defmacro with-scope-restarts
  [restarts & body]
  `(binding [internal/*scope-restarts* (set ~restarts)]
     ~@body))

(def ^{:dynamic true
       :doc "A hook that tooling can bind to provide interactive restart
             selection. Should return a vector with restart name and any
             arguments."}
  *unhandled-hook* nil)

(defn on-unhandled-hook
  "Call hook for unhandled exception"
  [e]
  (when (and *unhandled-hook* (scope-restarts))
    (*unhandled-hook* e (scope-restarts))))
