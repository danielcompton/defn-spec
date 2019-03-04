(defproject net.danielcompton/defn-spec.alpha "0.1.0-SNAPSHOT"
  :description "Create specs for your functions inline with your defn's"
  :url "https://github.com/danielcompton/defn-spec"
  :license {:name "Eclipse Public License"
            :url  "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.10.0"]
                 [prismatic/schema "1.1.10"]
                 [org.clojure/clojurescript "1.9.562"]]
  :profiles {:dev {:dependencies [[orchestra "2019.02.06-1"]]}})
