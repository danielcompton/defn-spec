(ns net.danielcompton.defn-spec.alpha
  (:refer-clojure :exclude [defn])
  (:require [clojure.spec.alpha :as s]
            [net.danielcompton.defn-spec.alpha.macros :as macros]))

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
  {:arglists '([name ret-spec? doc-string? attr-map? [params*] prepost-map? body])}
  [& defn-args]
  (let [[name & more-defn-args] (macros/normalized-defn-args &env defn-args)
        {:keys [doc tag] :as standard-meta} (meta name)
        {:keys [outer-bindings schema-form fn-body arglists raw-arglists
                processed-arities]} (macros/process-fn- &env name more-defn-args)]
    `(let ~outer-bindings
       (let [ret# (clojure.core/defn ~(with-meta name {})
                    ~(assoc (apply dissoc standard-meta (when (macros/primitive-sym? tag) [:tag]))
                       :doc (str
                              (str "Inputs: " (if (= 1 (count raw-arglists))
                                                (first raw-arglists)
                                                (apply list raw-arglists)))
                              (when-let [ret (when (= (second defn-args) :-) (nth defn-args 2))]
                                (str "\n  Returns: " ret))
                              (when doc (str "\n\n  " doc)))
                       :raw-arglists (list 'quote raw-arglists)
                       :arglists (list 'quote arglists)
                       ;; TODO: remove this
                       :spec schema-form)
                    ~@fn-body)]
         (s/fdef ~(with-meta name {})
                 :ret ~(:spec (meta name))
                 :args (s/cat ~@(mapcat
                                  #(list (keyword %) (:spec (meta %)))
                                  (first arglists))))
         ret#))))
