(ns follow-music.last-fm
  (:require [clj-http.client :as client])
  (:require [cheshire.core :as json])
  (:require [follow-music.utils :as utils]))

(def ^{:private true, :const true} config (utils/load-config "/usr/src/app/config/config.local.edn"))
(def ^{:private true, :const true} api-key (:key (:last-fm config)))
(def ^{:private true, :const true} api-base-url "https://ws.audioscrobbler.com/2.0")

(defn- constructed-url
  [method]
  (format "%s/?method=%s&api_key=%s&format=json" api-base-url method api-key))

(defn- similar-artists-url
  [artist limit]
  (format "%s&artist=%s&limit=%d" (constructed-url "artist.getsimilar") artist limit))

(defn- top-user-artists-url
  [user limit page]
  (format "%s&user=%s&limit=%d&page=%d" (constructed-url "user.gettopartists") user limit page))

(defn- fresh->similar-artists
  ([artist limit] (:body (client/get (similar-artists-url artist limit))))
  ([artist] (fresh->similar-artists artist 400)))

(defn- fresh->top-user-artists
  ([user limit page] (:body (client/get (top-user-artists-url user limit page))))
  ([user page] (fresh->top-user-artists user 1000 page)))

(defn- parsed-top-user-artists
  [payload]
  (:artist (:topartists (json/parse-string payload true))))

(defn- parsed-similar-artists
  [payload]
  (:artist (:similarartists (json/parse-string payload true))))

(defn- relevant?
  ([artist relevance] (>= (Float/parseFloat (:match artist)) relevance))
  ([artist] (relevant? artist 0.35)))

(defn similar-artists
  [artist]
  (->> (parsed-similar-artists (fresh->similar-artists artist))
   (take-while relevant?)
   (filter #(utils/not-nil? (:mbid %)))))

(defn user-artists
  ([user page all-artists]
   (let [artists (parsed-top-user-artists (fresh->top-user-artists user page))]
    (if (empty? artists)
     all-artists
     (recur user (inc page) (concat all-artists artists)))))
  ([user] (user-artists user 1 [])))
