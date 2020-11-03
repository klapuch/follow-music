(ns follow-music.pages.root
 (:require [hiccup.page :refer [html5]])
 (:require [ring.util.codec :refer [form-encode]])
 (:require [follow-music.neo4j.queries :as queries]))

(defn- h-not-interested-in-forms
 [artists]
 (for [artist artists]
  [:form {:method "POST" :action "/not-interested-in" :id (:id (:recommended artist))}
   [:input {:type "hidden" :name "id" :value (:id (:recommended artist))}]]))

(defn- first-from-each-group
 [artists]
 (->> artists
  (group-by #(:id (:known %)))
  (map last)
  (map first)
  (map (fn [artist] [(:id (:known artist)) (:id (:recommended artist))]))))

(defn- first-group?
 [firsts artist]
 (some
  (fn [current] (= current [(:id (:known artist)) (:id (:recommended artist))]))
  firsts))

(defn- is-very-similar?
 ([artist relevance] (> (:relevance (:recommended artist)) relevance))
 ([artist] (is-very-similar? artist 0.6)))

(defn- h-recommended-artists
 [artists]
 (let [firsts (first-from-each-group artists)]
  (for [artist artists]
   (let [known (:known artist) recommended (:recommended artist)]
    [:tr {:class (when (first-group? firsts artist) "has-background-light")}
     [:td
      [:a {:href (:url known)} (:name known)]
     [:td
      [(if (is-very-similar? artist) :strong :span)
       [:a {:href (:url recommended) :title (:relevance recommended)} (:name recommended)]]]
     [:td
      [:button.button.is-black.is-small {:form (:id recommended) :type "submit"} "Not interested"]]]]))))

(def ^{:private true, :const true} page-name "page")

(defn- with-page
 [query-parameters page]
 (form-encode (assoc query-parameters page-name page)))

(defn- prev-page-link
 [query-parameters page]
 (if (> page 2)
  (str "?" (with-page query-parameters (dec page)))
  (let [without-page (dissoc query-parameters page-name)]
   (if (empty? without-page)
    "/"
    (str "?" (form-encode without-page))))))

(defn- next-page-link
 [query-parameters page]
 (str "?" (with-page query-parameters (inc page))))

(defn- h-pagination
 [page query-parameters has-prev-page has-next-page]
 [:nav.pagination {:role "navigation" :aria-label "pagination"}
  (when has-prev-page [:a.pagination-previous.is-pulled-left {:href (prev-page-link query-parameters page)} "Previous"])
  (when has-next-page [:a.pagination-next.is-pulled-right {:href (next-page-link query-parameters page)} "Next"])])


(defn content
 ([session user query-parameters per-page]
 (let [
       page (max (Integer/parseInt (get query-parameters page-name "1")) 1)
       db-recommended-artists (queries/recommended-artists session {:user {:user user}
                                                                    :limit (inc per-page)
                                                                    :skip (* per-page (dec page))})
       recommended-artists (drop-last db-recommended-artists)
       has-next-page (> (count db-recommended-artists) per-page)
       has-prev-page (> page 1)
      ]
  (html5
   [:head
    [:meta {:http-equiv "content-type" :content "text/html; charset=UTF-8"}]
    [:title "Follow Music"]
    [:link {:rel "stylesheet" :type "text/css" :href "css/bulma.min.css"}]]
   [:body
    (h-not-interested-in-forms recommended-artists)
    [:section.section
     [:div.container
      [:div.columns.is-centered
       [:div.column.is-half
        (h-pagination page query-parameters has-prev-page has-next-page)
        [:table.table.is-bordered.is-hoverable.is-fullwidth
         [:thead
          [:tr
           [:th "Artist"]
           [:th "Recommendation"]
           [:th "Actions"]]]
         [:tbody (h-recommended-artists recommended-artists)]]
         (h-pagination page query-parameters has-prev-page has-next-page)]]]]])))
  ([session user query-parameters] (content session user query-parameters 50)))