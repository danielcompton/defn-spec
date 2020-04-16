(ns net.danielcompton.defn-spec-alpha
  (:refer-clojure :exclude [defn])
  (:require [clojure.spec.alpha :as s]
            [net.danielcompton.defn-spec-alpha.spec :as spec]
            [net.danielcompton.defn-spec-alpha.macros :as macros]))

(defmacro defn
  "Like clojure.core/defn, except that spec typehints can be given on
   the argument symbols and on the function name (for the return value).

   (s/defn calculate-total :- number?
    [qty :- :unit/quantity
     price :- ::price]
    (* qty price))

   Gotchas and limitations:
    - The output spec always goes on the fn name, not the arg vector. This
      means that all arities must share the same :ret spec (as do all Clojure specs).
      defn-spec will automatically propagate primitive hints to the arg vector and class hints
      to the fn name, so that you get the behavior you expect from Clojure.
    - spec metadata is only processed on top-level arguments.  I.e., you can
      use destructuring, but you must put spec metadata on the top-level
      arguments, not the destructured variables.
      Bad:  (s/defn foo [{:keys [x :- ::x]}])
      Good: (s/defn foo [{:keys [x]} :- (s/keys :req [::x])])
    - Only a specific subset of rest-arg destructuring is supported:
      - & rest works as expected
      - & [a b] works, with specs for individual elements parsed out of the binding,
        or an overall spec on the vector
      - & {} is not supported.
    - Unlike clojure.core/defn, a final attr-map on multi-arity functions
      is not supported."
  [& args]
  (let [ast (s/conform ::spec/annotated-defn-args args)
        defn-form (->> ast
                       (spec/annotated-defn->defn)
                       (s/unform ::spec/defn-args))
        arg-spec (macros/combine-arg-specs ast)
        ret-spec (get-in ast [:ret-annotation :spec])
        fn-name (:fn-name ast)]
    `(do
       (clojure.core/defn ~@defn-form)
       ~(cond (and arg-spec ret-spec)
              `(clojure.spec.alpha/fdef ~fn-name
                 :args ~arg-spec
                 :ret ~ret-spec)
              arg-spec
              `(clojure.spec.alpha/fdef ~fn-name
                 :args ~arg-spec)
              ret-spec
              `(clojure.spec.alpha/fdef ~fn-name
                 :ret ~ret-spec)
              :else nil))))
