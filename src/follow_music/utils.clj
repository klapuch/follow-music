(ns follow-music.utils
  (:require [clojure.edn :as edn]))

(defn load-config
  [filename]
  (edn/read-string (slurp filename)))

(def not-nil? (complement nil?))
