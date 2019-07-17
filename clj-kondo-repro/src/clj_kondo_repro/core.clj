(ns clj-kondo-repro.core
  (:require [net.danielcompton.defn-spec-alpha :as ds]))

(ds/defn test-fn
  [a b]
  "Wrong place docstring"
  a)

(ds/defn test-fn2
  [x y :- string?]
  "Wrong place docstring"
  x)
