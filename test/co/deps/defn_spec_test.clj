(ns co.deps.defn-spec-test
  (:refer-clojure :exclude [defn])
  (:require [clojure.test :refer :all]
            [co.deps.defn-spec :refer :all]))

(deftest a1
  (defn fn-args-2t [bxy :- String]
    5
    (+ 10 bxy)))
