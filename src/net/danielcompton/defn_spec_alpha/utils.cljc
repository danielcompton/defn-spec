(ns net.danielcompton.defn-spec-alpha.utils
  "Private utilities used in schema implementation."
  (:refer-clojure :exclude [record?])
  #?(:clj
     (:require [clojure.string :as string])
     (:require
       goog.string.format
       [goog.string :as gstring]
       [clojure.string :as string]))
  #?(:cljs (:require-macros [schema.utils :refer [char-map]])))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;; Miscellaneous helpers

(defn assoc-when
  "Like assoc but only assocs when value is truthy.  Copied from plumbing.core so that
   schema need not depend on plumbing."
  [m & kvs]
  (assert (even? (count kvs)))
  (into (or m {})
        (for [[k v] (partition 2 kvs)
              :when v]
          [k v])))

;; TODO: move away from format, has DCE issues
(defn format* [fmt & args]
  (apply #?(:clj format :cljs gstring/format) fmt args))
