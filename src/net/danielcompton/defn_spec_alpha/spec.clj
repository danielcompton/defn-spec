(ns net.danielcompton.defn-spec-alpha.spec
  "Here, two top-level specs are defined:
  ::defn-args, which is similar to the one defined in core.specs.alpha, but modified so that unform is the inverse
  of conform.

  ::annotated-defn-args, which defines the syntax of the defn-spec-alpha defn macro including spec annotations

  The original specs are specified here:
  https://github.com/clojure/core.specs.alpha/blob/master/src/main/clojure/clojure/core/specs/alpha.clj

  We've altered them according to https://blog.klipse.tech/clojure/2019/03/08/spec-custom-defn.html which
  in turn cites https://github.com/Engelberg/better-cond"
  (:require [clojure.spec.alpha :as s]
            [clojure.walk :as walk]))

(defn arg-list-unformer
  "The original param-list spec unforms function args as a seq and wraps & destructured args in another seq.
  Ensure args are unformed as a vector and & destructured forms are spliced into the args form."
  [arg]
  (vec 
   (if (and (coll? (last arg)) (= '& (first (last arg))))
     (concat (drop-last arg) (last arg))
     arg)))

;; ::defn-args supporting specs

(s/def ::seq-binding-form
  (s/and vector?
         (s/conformer identity vec)
         (s/cat :forms (s/* ::binding-form)
                :rest-forms (s/? (s/cat :ampersand #{'&} :form ::binding-form))
                :as-form (s/? (s/cat :as #{:as} :as-sym ::local-name)))))

(s/def ::map-special-binding
  (s/keys :opt-un [::as ::or ::keys ::syms ::strs]))

(s/def ::ns-keys
  (s/tuple
    (s/and qualified-keyword? #(-> % name #{"keys" "syms"}))
    (s/coll-of simple-symbol? :kind vector?)))

(s/def ::map-binding (s/tuple ::binding-form any?))

(s/def ::map-bindings
  (s/every (s/or :map-binding ::map-binding
                 :qualified-keys-or-syms ::ns-keys
                 :special-binding (s/tuple #{:as :or :keys :syms :strs} any?)) :kind map?))

(s/def ::map-binding-form (s/merge ::map-bindings ::map-special-binding))

(s/def ::binding-form
  (s/or :local-symbol ::local-name
        :seq-destructure ::seq-binding-form
        :map-destructure ::map-binding-form))

(s/def ::param-list
       (s/and
        vector?
        (s/conformer identity arg-list-unformer)
        (s/cat :args (s/* ::binding-form)
               :varargs (s/? (s/cat :amp #{'&} :form ::binding-form)))))

(s/def ::params+body
  (s/cat :params ::param-list
         :body (s/alt :prepost+body (s/cat :prepost map?
                                           :body (s/+ any?))
                      :body (s/* any?))))

(s/def ::defn-args
  (s/cat :fn-name simple-symbol?
         :docstring (s/? string?)
         :meta (s/? map?)
         :fn-tail (s/alt :arity-1 ::params+body
                         :arity-n (s/cat :bodies (s/+ (s/spec ::params+body))
                                         :attr-map (s/? map?)))))

;; ::annotated-defn-args suppporting specs

(s/def ::spec-annotation (s/cat :spec-literal #{:-} :spec any?))

(defn annotated-arg-list-unformer [a]
  (let [args-form (->> a
                       (arg-list-unformer)
                       (map (fn [x]
                              (cond (not (seq? x)) (list x)
                                    (map? x) (list x)
                                    :else x))))]
    (vec (apply concat args-form))))

(defn annotated-defn-unformer
  "Splice annotated forms into their parent expression rather than
  nesting them as an additional seq.

  Without an unformer (defn a :- int? [b c] (+ b c)), when conformed
  and unformed again, would produce (defn a (:- int?) ...) where we
  want (defn a :- int ...)"
  [a]
  (if (and (coll? a) (= :- (first (first (rest a)))))
    (concat (take 1 a) (first (rest a)) (rest (rest a)))
    a))

(s/def ::local-name (s/and simple-symbol? #(not= '& %)))

(s/def ::annotated-map-binding-form (s/merge ::map-bindings ::map-special-binding))

(s/def ::annotated-binding-form
  (s/alt :local-symbol (s/cat :local-name ::local-name
                              :annotation (s/? (s/cat :spec-literal #{:-} :spec any?)))
         :seq-destructure (s/cat :seq-binding-form ::seq-binding-form
                                 :annotation (s/? (s/cat :spec-literal #{:-} :spec any?)))
         :map-destructure (s/cat :map-binding-form ::map-binding-form
                                 :annotation (s/? (s/cat :spec-literal #{:-} :spec any?)))))

(s/def ::annotated-param-list
  (s/and
   vector?
   (s/conformer identity annotated-arg-list-unformer)
   (s/cat :args (s/* ::annotated-binding-form)
          :varargs (s/? (s/cat :amp #{'&} :form ::annotated-binding-form)))))

(s/def ::annotated-params+body
  (s/cat :params ::annotated-param-list
         :body (s/alt :prepost+body (s/cat :prepost map?
                                           :body (s/+ any?))
                      :body (s/* any?))))

(s/def ::annotated-defn-args
  (s/and
   (s/conformer identity annotated-defn-unformer)
   (s/cat :fn-name simple-symbol?
          :ret-annotation (s/? ::spec-annotation)
          :docstring (s/? string?)
          :meta (s/? map?)
          :fn-tail (s/alt :arity-1 ::annotated-params+body
                          :arity-n (s/cat :bodies (s/+ (s/spec ::annotated-params+body))
                                          :attr-map (s/? map?))))))


(defn- annotated-args->args
  "Strip annotation from an args AST node"
  [ast]
  (walk/postwalk (fn [x]
                   (cond (and (map-entry? x) (= :local-symbol (key x)) (not (symbol? (val x))))
                         [:local-symbol (:local-name (last x))]
                         (and (map-entry? x) (= :map-destructure (key x)))
                         [:map-destructure (:map-binding-form (last x))]
                         (and (map-entry? x) (= :seq-destructure (key x)))
                         [:seq-destructure (:seq-binding-form (last x))]
                         :else x)) ast))

(defn- annotated-varargs->args
  "Strip annotation from a varargs AST node"
  [ast]
  (walk/postwalk (fn [x]
                   (cond (and (coll? x) (:annotation x)) (dissoc x :annotation)
                         (and (map-entry? x) (= :local-symbol (key x)) (:local-name (val x)))
                         [:local-symbol (:local-name (val x))]
                         (and (map-entry? x) (= :seq-binding-form (key x))) (val x)
                         :else x)) ast))

(defn annotated-defn->defn
  "Given an AST obtained from conforming an annotated defn, transform it into an AST that can
  unform into a defn without annotation."
  [ast]
  (walk/prewalk (fn [x]
                  (cond (and (map-entry? x) (= :args (key x))) (annotated-args->args x)
                        (and (map-entry? x) (= :varargs (key x))) (annotated-varargs->args x)
                        :else x)) ast))


