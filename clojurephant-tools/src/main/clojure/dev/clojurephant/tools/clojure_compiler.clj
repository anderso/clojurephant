(ns dev.clojurephant.tools.clojure-compiler
  (:require [dev.clojurephant.tools.logger :refer [log]]
            [clojure.edn :as edn]))

(def ^:dynamic *namespaces*)

(defn -main [& args]
  (log :debug "Classpath: %s" (System/getProperty "java.class.path"))
  (let [[destination-dir namespaces options] (edn/read)]
    (binding [*namespaces* (seq namespaces)
              *compile-path* destination-dir
              *compiler-options* options]
      (doseq [namespace namespaces]
        (try
          (log :info "Compiling %s" namespace)
          (compile (symbol namespace))
          (catch Throwable e
            (loop [ex e]
              (when-let [msg (and ex (.getMessage ex))]
                (log :error "Failed to compile %s" namespace)
                (log :error msg))
              (when ex (recur (.getCause ex))))
            (throw e)))))))
