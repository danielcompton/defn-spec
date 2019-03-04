# defn-spec

[![CircleCI](https://circleci.com/gh/danielcompton/defn-spec.svg?style=svg)](https://circleci.com/gh/danielcompton/defn-spec)

defn-spec lets you create Clojure Specs inline with your `defn`, rather than having to write a separate `fdef` and keep it synchronized with the function definition. The syntax (and implementation) has been borrowed from [Schema](https://github.com/plumatic/schema), so if you've used that before, this should be very familiar.

Instead of writing:

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
(ds/defn-spec to-zoned-dt :- ::zoned-date-time
  [instant :- ::instant
   zone-id :- ::zone-id]
  (ZonedDateTime/ofInstant instant zone-id))
```

## Usage

The basic syntax is the same as `clojure.core/defn`, but you can optionally add 'spec hints' with `:-` to any of the arguments and the return value. Any valid spec can be used, e.g. functions, sets, registered specs.

```clj
(s/defn my-fn-name :- <:ret spec>
  [arg1 :- <spec for single arg>
   arg2 :- <spec for single arg>
   arg3] ;; Not all args need to be specced
   ; ...
   )
```

The 'spec hints' are used to define a matching `fdef` for your function.

## Implementation notes

defn-spec should preserve all metadata, docstrings, and type hints in the same way that Clojure's `defn` does. If you spot anything that isn't being retained, this is a bug, let me know!

## Limitations

* Multiple arity functions, are not currently supported, though this is on the roadmap
* Specs for functions are only defined, defn-spec doesn't control when/if the function specs are checked. I recommend using [orchestra](https://github.com/jeaye/orchestra) in development to instrument the `:args`, `:fn`, and `:ret` specs for your `fdef`'s. 
* `:fn` specs are not supported yet, as I'm not sure where to put the `:fn` spec yet.
* 

## Cursive integration

You can tell Cursive to resolve defn-spec's `defn` macro like the schema `defn` macro. See the [Cursive setup](doc/cursive.md) page for full documentation.

## License

Copyright © 2017-2019 Daniel Compton

defn-spec contains extensive copying from [plumatic/schema](https://github.com/plumatic/schema/), and couldn't have been completed without their great work.

Distributed under the Eclipse Public License either version 1.0 or (at
your option) any later version.
