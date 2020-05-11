(ns net.danielcompton.defn-spec-alpha-test
  (:require [clojure.spec.alpha :as s]
            [clojure.test :refer :all]
            [net.danielcompton.defn-spec-alpha :as ds]
            [net.danielcompton.defn-spec-alpha.spec :as spec]
            [orchestra.spec.test :as st])
  (:import clojure.lang.ExceptionInfo))

(use-fixtures :each
              (fn [f]
                (st/unstrument)
                (st/instrument)
                (f)))

(defn maybe-describe [p]
  (some-> p s/get-spec s/describe))

(s/def ::int int?)

;; spec tests

(defn conform-unform [sexpr]
  (->> sexpr
       (s/conform ::spec/defn-args)
       (s/unform ::spec/defn-args)))

(defn annotated-conform-unform [sexpr]
  (->> sexpr
       (s/conform ::spec/annotated-defn-args)
       (s/unform ::spec/annotated-defn-args)))

(deftest conform-is-the-inverse-of-unform
  (testing "defn spec"
    (let [single-arity '(a [x] x)
          keys-destructuring '(b [{:keys [a b c]}] (+ b c))
          seq-destructuring '(c [[a b] c] (+ a c))
          and-destructuring '(d [a & xs] xs)
          multiple-arities '(e ([a] a) ([a {:keys [b c]}] (+ b c)) ([a & xs] xs) ([[c d]] (+ c d)))]
      (are [original unformed] (= original unformed)
        single-arity (conform-unform single-arity)
        keys-destructuring (conform-unform keys-destructuring)
        seq-destructuring (conform-unform seq-destructuring)
        and-destructuring (conform-unform and-destructuring)
        multiple-arities (conform-unform multiple-arities))))
  (testing "annotated defn spec"
    (let [only-ret '(a :- int? [b] b)
          no-ret '(a :- int? [b :- int?] b)
          map-destructuring '(a :- int? [{:keys [b c]} :- map?]
                                (+ b c))
          seq-destructuring '(a :- int? [& xs :- any?]
                                (apply + xs))]
      (are [original] (= original (annotated-conform-unform original))
        only-ret
        no-ret
        map-destructuring
        seq-destructuring))))

;; function definition tests

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
  (is (thrown? ExceptionInfo (args-ret-broken 5))))

(deftest spec-forms-test
  (is (= (maybe-describe `args-ret)
         '(fspec :args (cat :x ::int) :ret int? :fn nil)))

  (is (= (maybe-describe `ret-spec)
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
    (is (= 1 (multi-arity nil)))
    (is (= 2 (multi-arity nil nil)))
    (is (= 3 (multi-arity nil nil nil))))

  (testing "works with specs"
    (is (= (multi-arity-spec) 0))
    (is (= (multi-arity-spec 5) 1))
    (is (thrown? ExceptionInfo (multi-arity-spec :x)))
    ;; TODO: arity names should start match number of arguments
    (is (= '(fspec :args (or :arity-1 (cat)
                             :arity-2 (cat :x ::int)
                             :arity-3 (cat :x ::int :y ::int))
                   :ret int?
                   :fn nil)
           (maybe-describe `multi-arity-spec)))))

(ds/defn rest-destructuring-1
  [& rest]
  rest)

(ds/defn rest-destructuring-2 [head & tail]
  [head tail])

(ds/defn multi-arity-varargs
  ([x]
   x)
  ([x y z & rest]
   [x y z rest]))

(ds/defn destructuring-list :- ::int
  [a :- ::int b :- int? & [c d]]
  [a b c d])

(ds/defn annotated-vararg-destructuring :- string?
  [a :- string? & rest]
  (apply str a rest))


;; Should we support annotation on the RHS of an &?
#_(ds/defn annotated-seq-destructuring :- string?
  [a :- string? & rest :- (s/coll-of string?)]
    (apply str a rest))

(ds/defn annotated-seq-destructuring :- ::int
  [[a b] :- (s/coll-of ::int)]
  (+ a b))

(deftest varargs-tests
  (is (= '(1 2 3) (rest-destructuring-1 1 2 3)))
  (is (= [1 '(2 3)] (rest-destructuring-2  1 2 3)))
  (is (= 3 (annotated-seq-destructuring [1 2])))
  (testing "multi-arity varargs"
    (is (= 1 (multi-arity-varargs 1)))
    (is (thrown? ExceptionInfo (multi-arity-varargs 1 2)))
    (is (= [1 2 3 nil] (multi-arity-varargs 1 2 3)))
    (is (= [1 2 3 [4]] (multi-arity-varargs 1 2 3 4)))
    (is (= [1 2 3 [4 5]] (multi-arity-varargs 1 2 3 4 5))))
  (is (thrown? ExceptionInfo (annotated-seq-destructuring [1 :x]))))

(s/def ::foo string?)
(s/def ::keys-test (s/keys :req-un [::int ::foo]))


(ds/defn keys-destructuring-unnamed :- (s/coll-of any?)
  [{:keys [int foo]} :- ::keys-test]
  [int foo])

(ds/defn keys-destructuring-named
  [{:keys [int foo] :as test-map} :- ::keys-test]
  [int foo])


(deftest keys-destructuring-test
  (testing "destructuring maps without an :as"
    (is (= (keys-destructuring-unnamed {:int 1 :foo "hi"}) [1 "hi"]))
    (is (thrown? ExceptionInfo
                 (keys-destructuring-unnamed {:int 5 :foo 5}))))
;
  (testing "destructuring maps with an :as"
    (is (= (keys-destructuring-named {:int 1 :foo "hi"}) [1 "hi"]))
    (is (thrown? ExceptionInfo
                 (keys-destructuring-named {:int 5 :foo 5})))))


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
    (is (nil? (s/get-spec `standard-defn))))

  (testing "if no spec hints are provided, no function spec is defined"
    (is (nil? (s/get-spec `no-spec-ds-defn))))

  (testing "if some spec hints are provided, a function spec is defined"
    (is (some? (maybe-describe `only-arg)))
    (is (some? (maybe-describe `several-args-1-specced)))
    (is (some? (maybe-describe `only-ret)))))

(comment
  (maybe-describe `arg-1-spec))

