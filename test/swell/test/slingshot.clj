(ns swell.test.slingshot
  (:require
   [swell.slingshot :as swell-slingshot]
   [swell.api :as swell]
   [slingshot.slingshot :as slingshot])
  (:use
   clojure.test
   swell.test.slingshot-test-helpers))

(deftest slingshot-test
  (is (=
       :yes
       (slingshot/try+
        (slingshot/throw+ ^{:type :abc} {})
        (catch #(isa? :abc (type %)) _ :yes)))
      "basic slingshot usage should still work"))

(defn fn-with-restart
  []
  (swell/restart-case
   [restart1 (fn [] :yes)
    :restart2 (fn [] :no)]
   (slingshot/throw+ ::e)))

(deftest unhandled-exception-test
  (testing "restart-case should not interfere with exceptions"
    (is-thrown-slingshot?
      (fn-with-restart))))

(deftest with-exception-scope-test
  (testing "with-exception-scope should compile"
    (is-thrown-slingshot?
      (swell/with-exception-scope []
        (slingshot/throw+ :anything)))))

(deftest invoke-exception-test
  (is-thrown-slingshot?
    (swell-slingshot/unwind-to-invoke-restart 'restart1))
  (is-thrown-slingshot?
    (swell/invoke-restart 'restart1)))

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
  (is (swell-slingshot/catches? {:object (Exception.)} Exception))
  (is (swell-slingshot/catches? {:object ::a} keyword?)))

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
  (let [f (fn []
            (swell/restart-case
             [restart1 (fn [] 1)]
             (inc
              (swell/restart-case
               [:restart2 (fn [] 3)]
               (slingshot/throw+ ::e)))))]
    (is (= 4 (swell/handler-bind [keyword? :restart2] (f))))
    (is (= 1 (swell/handler-bind [keyword? 'restart1] (f))))))

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
         (slingshot.slingshot/throw+ ::e))))))))
