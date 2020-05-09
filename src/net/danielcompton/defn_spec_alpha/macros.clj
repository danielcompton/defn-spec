(ns net.danielcompton.defn-spec-alpha.macros
  "Macros and macro helpers used in schema.core."
  (:require
    [clojure.string :as str]
    [schema.core]
    [net.danielcompton.defn-spec-alpha.utils :as utils]))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;; Helpers used in schema.core.

(defn cljs-env?
  "Take the &env from a macro, and tell whether we are expanding into cljs."
  [env]
  (boolean (:ns env)))

(defmacro if-cljs
  "Return then if we are generating cljs code and else for Clojure code.
   https://groups.google.com/d/msg/clojurescript/iBY5HaQda4A/w1lAQi9_AwsJ"
  [then else]
  (if (cljs-env? &env) then else))

(defmacro error!
  "Generate a cross-platform exception appropriate to the macroexpansion context"
  ([s]
   `(if-cljs
      (throw (js/Error. ~s))
      (throw (RuntimeException. ~(with-meta s `{:tag java.lang.String})))))
  ([s m]
   (let [m (merge {:type :schema.core/error} m)]
     `(if-cljs
        (throw (ex-info ~s ~m))
        (throw (clojure.lang.ExceptionInfo. ~(with-meta s `{:tag java.lang.String}) ~m))))))

(defmacro assert!
  "Like assert, but throws a RuntimeException (in Clojure) and takes args to format."
  [form & format-args]
  `(when-not ~form
     (error! (utils/format* ~@format-args))))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;; Helpers for processing and normalizing element/argument schemas in s/defrecord and s/(de)fn

(defn maybe-split-first [pred s]
  (if (pred (first s))
    [(first s) (next s)]
    [nil s]))

(def primitive-sym? '#{float double boolean byte char short int long
                       floats doubles booleans bytes chars shorts ints longs objects})

(defn valid-tag? [env tag]
  (and (symbol? tag) (or (primitive-sym? tag) (class? (resolve env tag)))))

(defn normalized-metadata
  "Take an object with optional metadata, which may include a :tag,
   plus an optional explicit schema, and normalize the
   object to have a valid Clojure :tag plus a :schema field."
  [env imeta explicit-spec]
  (let [{:keys [tag spec]} (meta imeta)]
    (assert! (< (count (remove nil? [spec explicit-spec])) 2)
             "Expected single schema, got meta %s, explicit %s" (meta imeta) explicit-spec)
    ;; It is useful to be able to differentiate between an any? symbol being attached as a spec
    ;; and it being defaulted to. Other parts of this parsing code rely on there always being
    ;; _a_ spec, so for now we carry around both bits of information. Perhaps as the code gets
    ;; further converted from it's schema roots this will change.
    (let [spec? (boolean (or explicit-spec spec tag))
          spec (or explicit-spec spec tag `any?)]
      (with-meta imeta
                 (-> (or (meta imeta) {})
                     (dissoc :tag)
                     (utils/assoc-when :spec spec
                                       :spec? spec?
                                       :tag (let [t (or tag spec)]
                                              (when (valid-tag? env t)
                                                t))))))))

(defn extract-schema-form
  "Pull out the schema stored on a thing.  Public only because of its use in a public macro."
  [symbol]
  (let [s (:spec (meta symbol))]
    (assert! s "%s is missing a schema" symbol)
    s))

(defn extract-arrow-schematized-element
  "Take a nonempty seq, which may start like [a ...] or [a :- schema ...], and return
   a list of [first-element-with-schema-attached rest-elements]"
  [env s]
  (assert (seq s))
  (let [[f & more] s]
    (if (= :- (first more))
      [(normalized-metadata env f (second more)) (drop 2 more)]
      [(normalized-metadata env f nil) more])))

(defn process-arrow-schematized-args
  "Take an arg vector, in which each argument is followed by an optional :- schema,
   and transform into an ordinary arg vector where the schemas are metadata on the args."
  [env args]
  (loop [in args out []]
    (if (empty? in)
      out
      (let [[arg more] (extract-arrow-schematized-element env in)]
        (recur more (conj out arg))))))


;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;; Helpers for schematized fn/defn

(defn nil->any? [spec]
  (if (nil? spec) 'any?
      spec))

(defn nils->any? [specs]
  (map nil->any? specs))


(defn gen-argument-keys [args]
  "Given a list of arguments obtained by conforming the ::annotated-defn-args spec,
   generate a list of keywords to label the spec in s/cat. Because map destructures are
   sometimes anonymous and seq destructures are always anonymous, we generate unique keys
   to annote them and aid in legibility when functions are instrumented."
  (let [keys-accumulator
        (reduce (fn [acc [type params]]
                  (case type
                    :local-symbol (update acc :arg-keys conj (keyword (:local-name params)))
                    :map-destructure (-> acc
                                         (update :arg-keys
                                                 conj
                                                 (keyword
                                                  (keyword (str "map-destructure-" (:map-destructure-count acc)))))
                                         (update :map-destructure-count inc))
                    :seq-destructure (-> acc
                                         (update :arg-keys
                                                 conj
                                                 (keyword (str "seq-destructure-" (:seq-destructure-count acc))))
                                         (update :seq-destructure-count inc))))
                {:map-destructure-count 1
                 :seq-destructure-count 1
                 :arg-keys []}
                args)]
    (:arg-keys keys-accumulator)))

(defn arity-labels []
  (map (fn [x] (keyword (str "arity-" x))) (iterate inc 1)))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;; Public: helpers for schematized functions

(defn combine-arg-specs [{:keys [fn-tail]}]
  ;; In the event of arity-1, check if anything is specified at all. If not, return nil
  ;; If so, run through each form, generating either the annotation with label or the
  ;; label with any?
  (case (first fn-tail)
    :arity-1
    (let [{:keys [args varargs]} (:params (last fn-tail))
          arg-specs (into [] (map #(get-in (last %) [:annotation :spec])) args)
          arg-names (gen-argument-keys args)
          vararg-spec (-> varargs
                          :form
                          last
                          :annotation
                          :spec)]
      ;; If no arguments are specced, return nil
      (when (some identity (conj arg-specs vararg-spec))
        (let [specced-args (vec (interleave arg-names (nils->any? arg-specs)))]
          (if varargs
            `(s/cat ~@specced-args :vararg (s/* ~(nil->any? vararg-spec)))
            `(s/cat ~@specced-args)))))
    :arity-n
    (let [{:keys [bodies]} (last fn-tail)
          params (map :params bodies)
          arg-lists (map :args params)
          vararg-lists (map :varargs params)
          arg-specs (map (fn [arglist]
                           (map (fn [arg]
                                  (get-in (last arg) [:annotation :spec]))
                                arglist)) arg-lists)
          vararg-specs (map (fn [vararg-list]
                              (map (fn [vararg]
                                     (-> vararg :form last :annotation :spec)) vararg-list)) vararg-lists)]
      ;; If no arguments are specced, return nil
      (when (some identity (conj (flatten arg-specs) (flatten vararg-specs)))
        `(s/or
          ~@(interleave (arity-labels)
                        (map (fn [arg-list vararg]
                               (let [arg-specs (into [] (comp (map #(get-in (last %) [:annotation :spec]))
                                                              (map nil->any?)) arg-list)
                                     vararg-spec (-> vararg
                                                     :form
                                                     last
                                                     :annotation
                                                     :spec)
                                     arg-names (gen-argument-keys arg-list)]
                                 (let [specced-args (vec (interleave arg-names arg-specs))]
                                   (if vararg
                                     `(s/cat ~@specced-args :varargs (s/* ~(nil->any? vararg-spec)))
                                     `(s/cat ~@specced-args)))))
                             arg-lists vararg-lists)))))))
