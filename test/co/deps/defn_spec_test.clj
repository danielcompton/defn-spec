(ns co.deps.defn-spec-test
  (:refer-clojure :exclude [defn])
  (:require [clojure.test :refer :all]
            [co.deps.defn-spec :as ds]
            [orchestra.spec.test :as st]
            [clojure.spec.alpha :as s])
  (:import (clojure.lang ExceptionInfo)))

(s/def ::int int?)

(deftest test-args-spec
  (ds/defn arg-1 [x :- int?]
    x)
  (ds/defn arg-1-spec [x :- ::int]
    x)
  (ds/defn arg-2 [x :- int? y]
    [x y])

  (st/instrument)

  (is (= 5 (arg-1 5)))
  (is (thrown? ExceptionInfo (arg-1 :x)))

  (is (= 5 (arg-1-spec 5)))
  (is (thrown? ExceptionInfo (arg-1-spec :x)))

  (is (= [1 2] (arg-2 1 2)))
  (is (= [1 :x] (arg-2 1 :x)))
  (is (thrown? ExceptionInfo (arg-2 :x :y))))

(deftest test-ret-spec
  (ds/defn ret :- int? [x] x)
  (ds/defn ret-spec :- ::int [x] x)

  (st/instrument)

  (is (= 5 (ret 5)))
  (is (thrown? ExceptionInfo (ret :x)))

  (is (= 5 (ret-spec 5)))
  (is (thrown? ExceptionInfo (ret-spec :x))))

(comment
  (s/describe (get @@#'clojure.spec.alpha/registry-ref `fn-args))

  )

