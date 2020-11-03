(ns follow-music.export
 (:gen-class)
 (:require [follow-music.last-fm :as last_fm])
 (:require [follow-music.neo4j.queries :as queries]))

(defn- add-artists
 [session user artists]
 (if-let [artist (first artists)]
  (do
   (queries/add-user-artist session {:user {:user user}
                                     :artist {:mbid (:mbid artist)
                                              :name (:name artist)
                                              :url (:url artist)}
                                     :play_count (:playcount artist)})
   (recur session user (rest artists)))))

(defn- add-similar-artists
 [session artists]
 (if-let [name (:name (first artists))]
  (do
   (let [similar-artists (last_fm/similar-artists name)]
    (dorun
     (map
      #(queries/add-similar-artist session {:name name
                                            :similar {:mbid (:mbid %)
                                                      :name (:name %)
                                                      :url (:url %)}
                                            :relevance (:match %)})
      similar-artists)))
   (recur session (rest artists)))))

(defn last-fm->neo4j
  [session user]
  (do
    (queries/add-user session {:user {:user user}})
    (add-artists session user (last_fm/user-artists user))
    (add-similar-artists session (queries/artists-without-similar session {:user {:user user}}))
    (queries/mark-unknown-artists session {:user {:user user}})))
