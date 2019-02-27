(ns co.deps.defn-spec
  (:refer-clojure :exclude [defn])
  (:require [clojure.spec.alpha :as s]
            [schema.core]
            [co.deps.defn-spec.macros :as macros]))

(defmacro defn
  "Like clojure.core/defn, except that spec typehints can be given on
   the argument symbols and on the function name (for the return value).

   (s/defn foo :- number?
    [x :- integer?
     y :- number?]
    (* x y))

   Gotchas and limitations:
    - The output schema always goes on the fn name, not the arg vector. This
      means that all arities must share the same output schema. Schema will
      automatically propagate primitive hints to the arg vector and class hints
      to the fn name, so that you get the behavior you expect from Clojure.
    - All primitive schemas will be passed through as type hints to Clojure,
      despite their legality in a particular position.  E.g.,
        (s/defn foo [x :- int])
      will fail because Clojure does not allow primitive ints as fn arguments;
      in such cases, use the boxed Classes instead (e.g., Integer).
    - Schema metadata is only processed on top-level arguments.  I.e., you can
      use destructuring, but you must put schema metadata on the top-level
      arguments, not the destructured variables.
      Bad:  (s/defn foo [{:keys [x :- s/Int]}])
      Good: (s/defn foo [{:keys [x]} :- {:x s/Int}])
    - Only a specific subset of rest-arg destructuring is supported:
      - & rest works as expected
      - & [a b] works, with schemas for individual elements parsed out of the binding,
        or an overall schema on the vector
      - & {} is not supported.
    - Unlike clojure.core/defn, a final attr-map on multi-arity functions
      is not supported."
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
