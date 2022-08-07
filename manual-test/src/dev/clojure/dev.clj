(ns dev
  (:require [com.stuartsierra.component.repl :refer [reset set-init start stop system]]
            [sample.core :as core]))

(set-init core/make-system)
