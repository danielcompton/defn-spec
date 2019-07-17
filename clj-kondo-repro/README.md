# clj-kondo-repro

Reproduction of clj-kondo lint-as bug. 

Linting `net.danielcompton.defn-spec-alpha/defn` as `schema.core/defn`

```console
$ clj-kondo --lint src
src/clj_kondo_repro/core.clj:4:10: info: unresolved symbol test-fn
src/clj_kondo_repro/core.clj:5:4: info: unresolved symbol a
src/clj_kondo_repro/core.clj:5:6: info: unresolved symbol b
src/clj_kondo_repro/core.clj:9:10: info: unresolved symbol test-fn2
src/clj_kondo_repro/core.clj:10:4: info: unresolved symbol x
src/clj_kondo_repro/core.clj:10:6: info: unresolved symbol y
linting took 5ms, errors: 0, warnings: 0
```

Linting `net.danielcompton.defn-spec-alpha/defn` as `clojure.core/defn`

```console
$ clj-kondo --lint src
src/clj_kondo_repro/core.clj:5:6: warning: unused binding b
src/clj_kondo_repro/core.clj:6:3: warning: misplaced docstring
src/clj_kondo_repro/core.clj:10:6: warning: unused binding y
src/clj_kondo_repro/core.clj:10:8: error: unsupported binding form :-
src/clj_kondo_repro/core.clj:10:11: warning: unused binding string?
src/clj_kondo_repro/core.clj:11:3: warning: misplaced docstring
linting took 6ms, errors: 1, warnings: 5
```
