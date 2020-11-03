(ns follow-music.neo4j.queries
 (:require [neo4j-clj.core :as db]))

(db/defquery add-user "
MERGE (user :User { user: $user.user })
   ON CREATE SET user.user = $user.user,
                 user.id = apoc.create.uuid()
")

(db/defquery add-user-artist "
MATCH (user :User { user: $user.user })
MERGE (artist :Artist { name: $artist.name })
   ON CREATE SET artist.mbid = $artist.mbid,
                 artist.name = $artist.name,
                 artist.url = $artist.url,
                 artist.id = apoc.create.uuid()
MERGE (user)-[r :LISTEN_TO]->(artist)
   ON CREATE SET r.play_count = $play_count
   ON MATCH SET r.play_count = $play_count
")

(db/defquery artists-without-similar "
MATCH (user :User { user: $user.user })-[:LISTEN_TO]->(artist :Artist)
WHERE NOT (artist)-[:SIMILAR_TO]->(:Artist) AND COALESCE(artist.unknown, FALSE) <> TRUE
RETURN artist.name AS name
")

(db/defquery add-similar-artist "
MATCH (artist :Artist { name: $name })
MERGE (similar :Artist { name: $similar.name })
   ON CREATE SET similar.mbid = $similar.mbid,
                 similar.name = $similar.name,
                 similar.url = $similar.url,
                 similar.id = apoc.create.uuid()
MERGE (artist)-[r :SIMILAR_TO]->(similar)
   ON CREATE SET r.relevance = $relevance
   ON MATCH SET r.relevance = $relevance
")


(db/defquery recommended-artists "
MATCH (user :User { user: $user.user })-[listen_to :LISTEN_TO]->(known_artist :Artist)-[similarity :SIMILAR_TO]->(unknown_artist :Artist)
WHERE NOT (user)-[:LISTEN_TO]->(unknown_artist)
  AND COALESCE(unknown_artist.unknown, FALSE) <> TRUE
  AND NOT (user)-[:NOT_INTERESTED_IN]-(unknown_artist)
  AND toInteger(listen_to.play_count) > 50
RETURN
 {id: known_artist.id, name: known_artist.name, url: known_artist.url} AS known,
 {id: unknown_artist.id, name: unknown_artist.name, url: unknown_artist.url, relevance: apoc.math.round(toFloat(similarity.relevance), 3)} AS recommended
ORDER BY
 toInteger(listen_to.play_count) DESC,
 apoc.math.round(toFloat(similarity.relevance), 3) DESC,
 unknown_artist.name
SKIP $skip
LIMIT $limit
")

(db/defquery mark-unknown-artists "
MATCH (user :User { user: $user.user })-[:LISTEN_TO]->(artist :Artist)
WHERE NOT (artist)-[:SIMILAR_TO]->(:Artist) AND COALESCE(artist.unknown, FALSE) <> TRUE
SET artist.unknown = TRUE
")

(db/defquery mark-not-interested-in "
MATCH (user :User { user: $user.user })
MATCH (artist :Artist { id: $artist.id })
MERGE (user)-[r :NOT_INTERESTED_IN]->(artist)
   ON CREATE SET r.since = localdatetime()
   ON MATCH SET r.since = localdatetime()
")
