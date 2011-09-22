(ns swell.spi
  "Service provider interface for swell"
  (:require
   [swell.internal :as internal]))

(defn handlers
  "Map of currently bound handlers"
  [] internal/*handlers*)

(defn find-restart
  "Finds restart in the current dynamic environment."
  [restart]
  (get internal/*restarts* restart (when (fn? restart) restart)))

(defn invoke-restart
  "Calls the function associated with restart, passing arguments to it. Restart
   must be valid in the current dynamic environment."
  [restart & args]
  (if-let [f (find-restart restart)]
    (apply f args)
    (throw
     (RuntimeException.
      (format "Attempt to call unbound restart %s. Known restarts %s."
              restart (vec (keys internal/*restarts*)))))))
