(ns co.deps.defn-spec
  (:require [clojure.spec.alpha :as s]
            [co.deps.defn-spec.macros :as macros]
            [co.deps.defn-spec.utils :as utils]))

;; Defn
;; Look at what prismatic schema did, and gfredricks
;; Can provide specs in several places

;; As metadata on the function

'[user :- :app/user]
;; ret value :-

;; As part of the pre/post map

{:pre  (fn [x])
 :post (fn [x])
 :args (s/cat :user :app/user)
 :ret  :app/user-friends}

;; As an fspec on the pre/post map

{:pre  (fn [x])
 :post (fn [x])
 :fspec (s/fspec :args (s/cat :user :app/user)
                 :ret :app/user-friends)}


;; Provide fn and defn
;; Allows speccing anonymous functions


(defmacro defn
  "Like clojure.core/defn, except that schema-style typehints can be given on
   the argument symbols and on the function name (for the return value).
   You can call s/fn-schema on the defined function to get its schema back, or
   use with-fn-validation to enable runtime checking of function inputs and
   outputs.
   (s/defn foo :- s/Num
    [x :- s/Int
     y :- s/Num]
    (* x y))
   (s/fn-schema foo)
   ==> (=> java.lang.Number Int java.lang.Number)
   (s/with-fn-validation (foo 1 2))
   ==> 2
   (s/with-fn-validation (foo 1.5 2))
   ==> Input to foo does not match schema: [(named (not (integer? 1.5)) x) nil]
   See (doc schema.core) for details of the :- syntax for arguments and return
   schemas.
   The overhead for checking if run-time validation should be used is very
   small -- about 5% of a very small fn call.  On top of that, actual
   validation costs what it costs.
   You can also turn on validation unconditionally for this fn only by
   putting ^:always-validate metadata on the fn name.
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
        {:keys [outer-bindings schema-form fn-body arglists raw-arglists]} (macros/process-fn- &env name more-defn-args)]
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
                       :schema schema-form)
                    ~@fn-body)]
         ;; TODO: create an fdef that goes here.
         (utils/declare-class-schema! (utils/fn-schema-bearer ~name) ~schema-form)
         ret#))))
