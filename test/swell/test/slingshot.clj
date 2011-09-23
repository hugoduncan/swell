(ns swell.test.slingshot
  (:require
   [swell.slingshot :as swell-slingshot]
   [swell.api :as swell]
   [slingshot.core :as slingshot])
  (:use
   [clojure.test]))

(deftest slingshot-test
  (is (=
       :yes
       (slingshot/try+
        (slingshot/throw+ ^{:type :abc} {})
        (catch {nil :abc} m :yes)))
      "basic slingshot usage should still work"))

(defn fn-with-restart
  []
  (swell/restart-case
   [restart1 (fn [] :yes)
    :restart2 (fn [] :no)]
   (slingshot/throw+ ::e)))

(deftest unhandled-exception-test
  (is (thrown? slingshot.Stone (fn-with-restart))
      "restart-case should not interfere with exceptions"))

(deftest with-exception-scope-test
  (is (thrown? slingshot.Stone
               (swell/with-exception-scope []
                 (slingshot/throw+ :anything)))
      "with-exception-scope should compile"))

(deftest invoke-exception-test
  (is (thrown? slingshot.Stone
               (swell-slingshot/unwind-to-invoke-restart 'restart1)))
  (is (thrown? slingshot.Stone
               (swell/invoke-restart 'restart1))))

(deftest invoke-restart-test
  (letfn [(f []
            (swell/restart-case
             [restart1 (fn [] :yes)]
             (swell-slingshot/unwind-to-invoke-restart 'restart1)))]
    (is (= :yes (f))
        "calling unwind-to-invoke-restart should return restart values"))
  (letfn [(f []
            (swell/restart-case
             [:restart1 (fn [] :yes)]
             (swell-slingshot/unwind-to-invoke-restart :restart1)))]
    (is (= :yes (f))
        "calling unwind-to-invoke-restart should return restart values")))

(deftest catches?-test
  (is (swell-slingshot/catches? {:obj (Exception.)} Exception))
  (is (swell-slingshot/catches? {:obj ::a} keyword?)))

(deftest restart-test
  (is (= :yes
         (swell/handler-bind
          [keyword? (fn invoke-restart1 [_] (swell/invoke-restart 'restart1))]
          (fn-with-restart)))
      "binding to arbitrary function")
  (is (= :yes
         (swell/handler-bind
          [keyword? 'restart1]
          (fn-with-restart)))
      "binding to restart name")
  (is (= :no
         (swell/handler-bind
          [keyword? :restart2]
          (fn-with-restart)))
      "binding to restart keyword"))

(deftest nested-handler-case-test
  (is (= 4
         (swell/handler-bind
          [keyword? :restart2]
          (swell/restart-case
           [restart1 (fn [] 1)]
           (inc
            (swell/restart-case
             [:restart2 (fn [] 3)]
             (slingshot/throw+ ::e)))))))
  (is (= 1
         (swell/handler-bind
          [keyword? 'restart1]
          (swell/restart-case
           [restart1 (fn [] 1)]
           (inc
            (swell/restart-case
             [:restart2 (fn [] 3)]
             (slingshot/throw+ ::e))))))))

(defn simple-unhandled-handler
  [e restarts]
  [(first restarts)])

(deftest unhandled-test
  (binding [swell.spi/*unhandled-hook* simple-unhandled-handler]
    (is
     (=
      4
      (swell.api/restart-case
       [restart1 (fn [] 1)]
       (inc
        (swell.api/restart-case
         [:restart2 (fn [] 3)]
         (slingshot.core/throw+ ::e))))))))
