(ns net.danielcompton.defn-spec-alpha-test
  (:require [clojure.test :refer :all]
            [net.danielcompton.defn-spec-alpha :as ds]
            [orchestra.spec.test :as st]
            [clojure.spec.alpha :as s])
  (:import (clojure.lang ExceptionInfo)))

(use-fixtures :each
              (fn [f]
                (st/unstrument)
                (st/instrument)
                (f)))

(defn get-spec [p]
  (get @@#'clojure.spec.alpha/registry-ref p))

(defn maybe-describe [p]
  (some-> p get-spec s/describe))

(s/def ::int int?)

(ds/defn arg-1-fn [x :- int?]
  x)

(deftest arg-1-test
  (is (= 5 (arg-1-fn 5)))
  (is (thrown? ExceptionInfo (arg-1-fn :x))))

(ds/defn arg-1-spec [x :- ::int]
  x)

(deftest arg-1-spec-test
  (is (= 5 (arg-1-spec 5)))
  (is (thrown? ExceptionInfo (arg-1-spec :x))))

(ds/defn arg-2 [x :- int? y]
  [x y])

(deftest arg-2-test
  (is (= [1 2] (arg-2 1 2)))
  (is (= [1 :x] (arg-2 1 :x)))
  (is (thrown? ExceptionInfo (arg-2 :x :y))))

(ds/defn ret :- int? [x] x)

(deftest ret-test
  (is (= 5 (ret 5)))
  (is (thrown? ExceptionInfo (ret :x))))

(ds/defn ret-spec :- ::int [x] x)

(deftest ret-spec-test
  (is (= 5 (ret-spec 5)))
  (is (thrown? ExceptionInfo (ret-spec :x))))

(ds/defn args-ret :- int? [x :- ::int]
  x)

(ds/defn args-ret-broken :- int? [x :- ::int]
  :x)

(deftest args-ret-test
  (is (= 5 (args-ret 5)))
  (is (thrown? ExceptionInfo
               (args-ret-broken 5))))

(deftest spec-forms-test
  (is (= (maybe-describe `args-ret)
         '(fspec :args (cat :x ::int) :ret int? :fn nil)))

  ;; TODO: shouldn't define any :args spec if none is provided, #1
  #_(is (= (maybe-describe `ret-spec)
         '(fspec :args nil :ret ::int :fn nil)))

  (is (= (maybe-describe `arg-2)
         '(fspec :args (cat :x int? :y any?) :ret any? :fn nil))))

(ds/defn multi-arity
  ([]
    0)
  ([x]
    1)
  ([x y]
    2)
  ([x y z]
    3))

(ds/defn multi-arity-spec :- int?
  ([] 0)
  ([x :- ::int] 1))

(deftest multi-arity-test
  (testing "works with no specs"
    (is (= 0 (multi-arity)))
    ;; TODO: not working yet, see #2
    #_(is (= 1 (multi-arity nil)))
    #_(is (= 2 (multi-arity nil nil)))
    #_(is (= 3 (multi-arity nil nil nil))))

  (testing "works with specs"
    (is (= (multi-arity-spec) 0))
    #_(is (= (multi-arity-spec 5) 1))))

; TODO: Broken, see #3
;(ds/defn rest-destructuring-1
;  [& rest]
;  rest)
;
;(ds/defn rest-destructuring-2 [head & tail]
;  [head tail])
;
;(deftest rest-destructuring-test)

; TODO: Broken, see #4
;(ds/defn destructuring-list :- ::int
;  [a :- ::int b :- int? & [c d]]
;  [a b c d])


; TODO: Broken, see #10
;(s/def ::foo string?)
;(s/def ::keys-test (s/keys :req-un [::int ::foo]))
;
;; Speccing keys
;(ds/defn keys-destructuring-unnamed
;  [{:keys [int foo]} :- ::keys-test]
;  [int foo])
;
;(ds/defn keys-destructuring-named
;  [{:keys [int foo] :as test-map} :- ::keys-test]
;  [int foo])
;
;
;(deftest keys-destructuring-test
;  (testing "destructuring maps without an :as"
;    (is (= (keys-destructuring-unnamed {:int 1 :foo "hi"}) [1 "hi"]))
;    (is (thrown? ExceptionInfo
;                 (keys-destructuring-unnamed {:int 5 :foo 5}))))
;
;  (testing "destructuring maps with an :as"
;    (is (= (keys-destructuring-named {:int 1 :foo "hi"}) [1 "hi"]))
;    (is (thrown? ExceptionInfo
;                 (keys-destructuring-named {:int 5 :foo 5}))))
;  )


(clojure.core/defn standard-defn [x]
  nil)

(ds/defn no-spec-ds-defn [x]
  nil)

(ds/defn only-arg [x :- ::int]
  x)

(ds/defn several-args-1-specced [y x :- ::int]
  x)

(ds/defn only-ret :- ::int [x]
  x)

(deftest partial-spec
  (testing "standard defn's don't register specs"
    (is (nil? (get-spec `standard-defn))))

  (testing "if no spec hints are provided, no function spec is defined"
    (is (nil? (get-spec `no-spec-ds-defn))))

  (testing "if some spec hints are provided, a function spec is defined"
    (is (some? (get-spec `only-arg)))
    (is (some? (get-spec `several-args-1-specced)))
    (is (some? (get-spec `only-ret)))))

(comment
  (s/describe (get @@#'clojure.spec.alpha/registry-ref `arg-1-spec))


  )

