# defn-spec

[![CircleCI](https://circleci.com/gh/danielcompton/defn-spec.svg?style=svg)](https://circleci.com/gh/danielcompton/defn-spec)

defn-spec lets you create Clojure Specs inline with your `defn`. The syntax (and implementation) has been borrowed from [Schema](https://github.com/plumatic/schema), so if you've used that before, this should be very familiar.

You can define your argument and return specs inline with your `defn`:

```clj
(ns my.ns
  (:require [net.danielcompton.defn-spec.alpha :as ds]
            [clojure.spec.alpha :as s]
            ; ...
            ))

;; Predicate definitions elided for brevity
(s/def ::instant instant?)
(s/def ::zone-id zone-id?)
(s/def ::zoned-date-time zoned-date-time?)

(ds/defn to-zoned-dt :- ::zoned-date-time
  [instant :- ::instant
   zone-id :- ::zone-id]
  (ZonedDateTime/ofInstant instant zone-id))
```

Instead of writing your `fdef` separately from your `defn`:

```clj
(defn to-zoned-dt
  [instant zone-id]
  (ZonedDateTime/ofInstant instant zone-id))

(s/fdef to-zoned-dt
        :args (s/cat :instant ::instant :zone-id ::zone-id)
        :ret ::zoned-date-time)
```

## Usage

The basic syntax is the same as `clojure.core/defn`, but you can optionally add 'spec hints' with `:-` to any of the arguments and the return value. Any valid spec can be used, e.g. functions, sets, registered spec identifiers.

```clj
(ds/defn my-fn-name :- <:ret spec>
  [arg1 :- <spec for single arg>
   arg2 :- <spec for single arg>
   arg3] ;; Not all args need to be specced
   ; ...
   )
```

## Motivation

I've been using Clojure spec for a while, but I found that I often resisted writing specs for functions. This was mostly because I didn't want to have to duplicate a bunch of information into the `fdef`. It's not a huge deal, but in my experience it was enough to deter me from writing specs while I was heavily working on an area of code. I created defn-spec to increase the locality of the spec definitions, and to reduce the activation energy to start adding specs to your codebase.

This is similar to Orchestra's [defn-spec](https://github.com/jeaye/orchestra#defn-spec) macro, but allows for optionally only speccing part of the function, and matches the well-known Schema defn syntax. Try them both out though, and see which one works best for you.

## Benefits and tradeoffs

Like all things in life, defn-spec has benefits and tradeoffs:

**Benefits**

* Makes it easy to incrementally spec your functions. It lowers the activation energy needed to add a spec, perhaps just for a single arg or return value.
* Makes it easier to see more code on one screen.
* Makes it harder for your specs to get out of sync with the function definition, as they are linked together.
* Avoids repeating all argument names in the `s/cat` form.

**Tradeoffs**

* For some specs, particularly complex `:args` specs with many branches, it may be simpler to define the `fdef` separately. `defn-spec` is designed for the 80-90% of Clojure functions that have simple argument lists and return types.
* Making it easier to change specs means that it is easier to accidentally break your callers. If your `fdef` is defined separately, it forces you to think more about growing the spec.

## Implementation notes

* defn-spec uses your `:ret` spec as-is, but constructs an `:args` spec for you based on the function's argument names and the 'spec hints' that you provide.
* defn-spec should preserve all metadata, docstrings, and type hints in the same way that Clojure's `defn` does. If you spot anything that isn't being retained, this is a bug, let me know!
* If you use the `ds/defn` macro, but don't define any 'spec hints' for the arguments or return value then no `fdef` spec is defined.
* When expanding the `ds/defn` macro, the `fdef` is defined immediately before the `clojure.core/defn`. If you declare fdefs after this point, they will overwrite the defn-spec `fdef`. You cannot merge 'spec hints' defined on a function and other spec definitions in a standalone `fdef`.
* Unlike schema, defn-spec doesn't control when/if function specs are checked. I recommend using [orchestra](https://github.com/jeaye/orchestra) in development to instrument the `:args`, `:fn`, and `:ret` specs for your `fdef`'s.

## Limitations

* Multiple arity functions are not yet supported. [#2](https://github.com/danielcompton/defn-spec/issues/2)
* `& rest`, `& [a b]`, and `:keys` destructuring are not yet supported. [#3](https://github.com/danielcompton/defn-spec/issues/3), [#4](https://github.com/danielcompton/defn-spec/issues/3), [#10](https://github.com/danielcompton/defn-spec/issues/10)
* `:fn` specs are not supported yet, as I'm not sure where to put the `:fn` spec yet. [#6](https://github.com/danielcompton/defn-spec/issues/6)
* ClojureScript is not supported yet. [#7](https://github.com/danielcompton/defn-spec/issues/7)
* Using an attr-map after the function definition is not supported. I've never seen this used in the wild, and didn't even know this was a thing until investigating the `defn` macro.

## Stability

This library is currently in alpha preview and is soliciting feedback on functionality and syntax from interested parties before publishing an official release. In the meantime, you can use a SNAPSHOT build at `[TODO]`.

defn-spec follows clojure.spec.alpha. When `clojure.spec.alpha2` is released, the plan is to publish a new artifact ID and set of `alpha2` namespaces, so you can use both versions side-by-side.

Long-term I would like `defn-spec` to be so stable that it is safe to include as a library dependency. While I strongly want to keep source compatibility, I can't guarantee this in the short-term. Until this warning is removed I would recommend only using this in applications or libraries where you control the consumers. There have also been rumblings that eventually there may be something similar to this built into Clojure's core defn macro.

## Cursive integration

You can tell Cursive to [resolve](https://cursive-ide.com/userguide/macros.html) defn-spec's `defn` macro like the schema `defn` macro. See the [Cursive setup](doc/cursive.md) page for full details on how to do this.

## License

Copyright © 2017-2019 Daniel Compton

defn-spec contains extensive copying from [plumatic/schema](https://github.com/plumatic/schema/), and couldn't have been completed without their great work.

Distributed under the Eclipse Public License either version 1.0 or (at
your option) any later version.
