# defn-spec

defn-spec lets you create Clojure Specs inline with your `defn`, rather than having to write a separate `fdef` and keep it synchronized with the function definition. The syntax (and implementation) has been borrowed from [Schema](https://github.com/plumatic/schema), so if you've used that before, this should be very familiar.

## Usage

The basic syntax is the same as `clojure.core/defn`, but you can optionally add 'spec hints' with `:-` to the arguments, and the return value.

```clj
(s/defn my-fn-name :- <spec>
  [arg1 :- <spec>
   arg2 :- <spec>
   arg3] ;; Not all args need to be specced
   ; ...
   )
```

The 'spec hints' are used to define a matching `fdef` for your function. For example:

```clj
(ns my.ns
  (:require '[co.deps.defn-spec :as ds]
             [clojure.spec.alpha]))

(s/def ::user-id string?)
(s/def ::password string?)

(ds/defn valid-password? :- boolean?
  [user-id :- ::user-id
   password :- ::password]
  (= password "sekrit"))

;; This compiles to:

(s/fdef valid-password?
  :args (s/cat :user-id ::user-id
               :password ::password)
  :ret boolean?)

(defn valid-password?
  [user-id
   password]
  (= password "sekrit"))
```

Metadata still works
Docstrings still work
Other metadata should be preserved.

Just defines spec fdefs, doesn't force validation, need to define it separately.

Currently doesn't do multiple arity functions

;; TODO: move to spec.alpha ns

## License

Copyright © 2017 Daniel Compton

defn-spec contains extensive copying from [plumatic/schema](https://github.com/plumatic/schema/), and couldn't have been completed without their great work.

Distributed under the Eclipse Public License either version 1.0 or (at
your option) any later version.
