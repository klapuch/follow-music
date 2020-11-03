(ns follow-music.commands.export
 (:require [neo4j-clj.core :as db])
 (:require [follow-music.export :as export])
 (:require [follow-music.utils :as utils])
 (:import (java.net URI)))

(def ^{:private true, :const true} user "RecklessFace")
(def ^{:private true, :const true} config (utils/load-config "/usr/src/app/config/config.local.edn"))

(def local-db
  (db/connect
   (URI.
    (format
     "bolt://%s:7687"
     (get-in config [:neo4j :host])))
    (get-in config [:neo4j :username])
    (get-in config [:neo4j :password])))

(defn -main
 [& args]
  (with-open [session (db/get-session local-db)]
   (export/last-fm->neo4j session user)
   (println "done")))
