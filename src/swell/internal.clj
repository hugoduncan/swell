(ns swell.internal
  "Internals")

(def
  ^{:dynamic true
    :doc "Handlers bound for current thread. Part of the dynamic environment."}
  *handlers* {})

(def
  ^{:dynamic true
    :doc "Restarts bound for current thread. Part of the dynamic environment."}
  *restarts* {})
