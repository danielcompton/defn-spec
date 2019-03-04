# defn-spec

[![CircleCI](https://circleci.com/gh/danielcompton/defn-spec.svg?style=svg)](https://circleci.com/gh/danielcompton/defn-spec)

defn-spec lets you create Clojure Specs inline with your `defn`, rather than having to write a separate `fdef` and keep it synchronized with the function definition. The syntax (and implementation) has been borrowed from [Schema](https://github.com/plumatic/schema), so if you've used that before, this should be very familiar.

Instead of writing your spec separately:

```clj
;; Predicate definitions elided for brevity
(s/def ::instant instant?)
(s/def ::zone-id zone-id?)
(s/def ::zoned-date-time zoned-date-time?)

(defn to-zoned-dt
  [instant zone-id]
  (ZonedDateTime/ofInstant instant zone-id))

(s/fdef to-zoned-dt
        :args (s/cat :instant ::instant :zone-id ::zone-id)
        :ret ::zoned-date-time)
```

You can define your argument and return specs inline:

```clj
(ns my.ns
  (:require [net.danielcompton.defn-spec.alpha :as ds]
            ; ...
            ))

(ds/defn-spec to-zoned-dt :- ::zoned-date-time
  [instant :- ::instant
   zone-id :- ::zone-id]
  (ZonedDateTime/ofInstant instant zone-id))
```

## Usage

The basic syntax is the same as `clojure.core/defn`, but you can optionally add 'spec hints' with `:-` to any of the arguments and the return value. Any valid spec can be used, e.g. functions, sets, registered specs.

```clj
(ds/defn my-fn-name :- <:ret spec>
  [arg1 :- <spec for single arg>
   arg2 :- <spec for single arg>
   arg3] ;; Not all args need to be specced
   ; ...
   )
```

The 'spec hints' are used to define a matching `fdef` for your function.

## Benefits and tradeoffs

Like all things in life, defn-spec has benefits and tradeoffs:

**Benefits**

* Makes it easy to incrementally spec your functions. It lowers the activation energy needed to add a spec, perhaps just for a single arg or return value.
* Makes it harder for your specs to get out of sync with the function definition, as they are linked together.
* Avoids repeating all argument names in the `s/cat` form.

**Tradeoffs**

* For some specs, particularly with complex macros, it may be simpler to define the `fdef` separately for full control of the `s/cat` form.
* Making it easier to change specs means that it is easier to accidentally break your callers. If your `fdef` is defined separately, it forces you to think more about growing the spec.

## Implementation notes

* defn-spec should preserve all metadata, docstrings, and type hints in the same way that Clojure's `defn` does. If you spot anything that isn't being retained, this is a bug, let me know!
* If you use the `ds/defn` macro, but don't define any 'spec hints' then no `fdef` spec is defined.
* When expanding the `ds/defn` macro, the `fdef` is defined immediately after the `clojure.core/defn`. If you declare fdefs after this point, they will overwrite the defn-spec `fdef`. You cannot merge 'spec hints' defined on a function and other spec definitions in a standalone `fdef`.
* Unlike schema, defn-spec doesn't control when/if function specs are checked. I recommend using [orchestra](https://github.com/jeaye/orchestra) in development to instrument the `:args`, `:fn`, and `:ret` specs for your `fdef`'s.

## Limitations

* Multiple arity functions are not yet supported. [#2](https://github.com/danielcompton/defn-spec/issues/2)
* `& rest` and `& [a b]` destructuring are not yet supported. [#3](https://github.com/danielcompton/defn-spec/issues/3), [#4](https://github.com/danielcompton/defn-spec/issues/3)
* `:fn` specs are not supported yet, as I'm not sure where to put the `:fn` spec yet. [#6](https://github.com/danielcompton/defn-spec/issues/6)
* ClojureScript is not supported yet. [#7](https://github.com/danielcompton/defn-spec/issues/7)

## Stability

This library is currently in alpha preview and is soliciting feedback from interested parties before publishing an official release. In the meantime, you can use a SNAPSHOT build at `[TODO]`.

defn-spec follows clojure.spec.alpha. When `clojure.spec.alpha2` is released, the plan is to publish a new artifact ID and set of `alpha2` namespaces, so you can use both versions side-by-side.

Long-term I would like `defn-spec` to be so stable that it is safe to include `defn-spec` as a library dependency. While I strongly want to keep source compatibility, I can't guarantee this in the short-term, so I would recommend only using this in applications or libraries where you control the consumers. 

## Cursive integration

You can tell Cursive to [resolve](https://cursive-ide.com/userguide/macros.html) defn-spec's `defn` macro like the schema `defn` macro. See the [Cursive setup](doc/cursive.md) page for full details on how to do this.

## License

Copyright © 2017-2019 Daniel Compton

defn-spec contains extensive copying from [plumatic/schema](https://github.com/plumatic/schema/), and couldn't have been completed without their great work.

Distributed under the Eclipse Public License either version 1.0 or (at
your option) any later version.
