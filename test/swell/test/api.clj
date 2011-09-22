(ns swell.test.api
  (:require
   [swell.api :as swell]
   [swell.internal :as internal])
  (:use [clojure.test]))

(deftest restart-case-test
  (is (= {} internal/*restarts*) "should initially be empty")
  (let [abc-fn (fn [] ::abc)]
    (swell/restart-case
     [abc abc-fn]
     (is (= abc-fn (get internal/*restarts* 'abc)) "bound by restart-case"))))
