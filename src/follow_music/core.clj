(ns follow-music.core
 (:gen-class)
 (:require [ring.middleware.resource :refer [wrap-resource]])
 (:require [ring.middleware.params :refer [wrap-params]])
 (:require [ring.util.response :refer [resource-response redirect]])
 (:require [compojure.core :refer [defroutes GET POST]])
 (:require [compojure.route :refer [not-found]])
 (:require [ring.adapter.jetty :refer [run-jetty]])
 (:require [ring.middleware.reload :refer [wrap-reload]])
 (:require [follow-music.neo4j.queries :as queries])
 (:require [follow-music.pages.root :as root])
 (:require [neo4j-clj.core :as db])
 (:require [follow-music.utils :as utils])
 (:import (java.net URI)))

(def ^{:private true, :const true} config (utils/load-config "/usr/src/app/config/config.local.edn"))

(def local-db
  (db/connect
   (URI.
    (format
     "bolt://%s:7687"
     (get-in config [:neo4j :host])))
    (get-in config [:neo4j :username])
    (get-in config [:neo4j :password])))

(def ^{:private true, :const true} user "RecklessFace")

(defroutes approutes
 (GET "/" req (root/content (db/get-session local-db) user (:params req)))
 (POST "/not-interested-in" req (do
  (queries/mark-not-interested-in (db/get-session local-db) {:user {:user user} :artist {:id (get (:params req) "id")}})
  (redirect "/")))
 (not-found "<h1>Not Found</h1>"))

(def app
 (-> approutes
  (wrap-reload #'approutes)
  (wrap-params)
  (wrap-resource "public")))

(defn server
 []
 (run-jetty app {:join? false, :port 3000}))

(defn -main
 [& args]
 (server))
