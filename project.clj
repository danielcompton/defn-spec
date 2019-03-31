(defproject net.danielcompton/defn-spec-alpha "0.1.1-SNAPSHOT"
  :description "Create specs for your functions inline with your defn's"
  :url "https://github.com/danielcompton/defn-spec"
  :license {:name "Eclipse Public License"
            :url  "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.10.0" :scope "provided"]
                 [org.clojure/clojurescript "1.9.562" :scope "provided"]
                 [prismatic/schema "1.1.10"]]
  :deploy-repositories [["releases" :clojars]
                       ["snapshots" :clojars]]
  :release-tasks [["vcs" "assert-committed"]
                  ["change" "version" "leiningen.release/bump-version" "release"]
                  ["vcs" "commit"]
                  ["vcs" "tag" "v"]
                  ["deploy"]
                  ["change" "version" "leiningen.release/bump-version"]
                  ["vcs" "commit"]
                  ["vcs" "push"]]
  :profiles {:dev {:dependencies [[orchestra "2019.02.06-1"]]}})
