(ns donkey.services.metadata.favorites
  (:require [clojure.set :as set]
            [cheshire.core :as json]
            [donkey.auth.user-attributes :as user]
            [donkey.clients.data-info :as data]
            [donkey.clients.metadata.raw :as metadata]
            [donkey.util.service :as svc]
            [donkey.util.validators :as valid])
  (:import [java.util UUID]))


(defn- format-favorites
  [favs]
  (letfn [(mk-fav [entry] (assoc entry :isFavorite true))]
    (assoc favs
      :files   (map mk-fav (:files favs))
      :folders (map mk-fav (:folders favs)))))


(defn- user-col->api-col
  [col]
  (if col
    (case (.toUpperCase col)
      "NAME"         :base-name
      "ID"           :full-path
      "LASTMODIFIED" :modify-ts
      "DATECREATED"  :create-ts
      "SIZE"         :data-size
                     :base-name)
    :base-name))


(defn- user-order->api-order
  [order]
  (if order
    (case (.toUpperCase order)
      "ASC"  :asc
      "DESC" :desc
             :asc)
    :asc))


(defn add-favorite
  "This function marks a given filesystem entry as a favorite of the authenticated user.

   Parameters:
     entry-id - This is the `entry-id` from the request.  It should be the UUID of the entry being
                marked."
  [entry-id]
  (let [user     (:shortUsername user/current-user)
        entry-id (UUID/fromString entry-id)]
    (data/validate-uuid-accessible user entry-id)
    (metadata/add-favorite entry-id (data/resolve-data-type entry-id))))


(defn remove-favorite
  "This function unmarks a given filesystem entry as a favortie of the authenticated user.

   Parameters:
     entry-id - This is the `entry-id` from the request.  It should be the UUID of the entry being
                unmarked."
  [entry-id]
  (metadata/remove-favorite entry-id))


(defn- ids-txt->uuids-set
  [ids-txt]
  (->> ids-txt (map #(UUID/fromString %)) set))

(defn- parse-filesystem-ids
  [json-txt]
  (-> json-txt (json/parse-string true) :filesystem))

(defn- extract-favorite-uuids-set
  [response]
  (-> response :body slurp parse-filesystem-ids ids-txt->uuids-set))


(defn list-favorite-data-with-stat
  "Returns a listing of a user's favorite data, including stat information about it. This endpoint
   is intended to help with paginating.

   Parameters:
     sort-col    - This is the value of the `sort-col` query parameter. It should be a case-
                   insensitive string containing one of the following:
                   DATECREATED|ID|LASTMODIFIED|NAME|SIZE
     sort-order  - This is the value of the `sort-order` query parameter. It should be a case-
                   insensitive string containing one of the following: ASC|DESC
     limit       - This is the value of the `limit` query parameter. It should contain a positive
                   number.
     offset      - This is the value of the `offset` query parameter. It should contain a non-
                   negative number.
     entity-type - This is the value of the `entity-type` query parameter. It should be a case-
                   insensitive string containing one of the following: ANY|FILE|FOLDER. If it is
                   nil, ANY will be used.
     info-types  - This is the value(s) of the `info-type` query parameter(s). It may be nil,
                   meaning return all info types, a string containing a single info type, or a
                   sequence containing a set of info types."
  [sort-col sort-order limit offset entity-type info-types]
  (let [user        (:shortUsername user/current-user)
        col         (user-col->api-col sort-col)
        ord         (user-order->api-order sort-order)
        limit       (Long/valueOf limit)
        offset      (Long/valueOf offset)
        entity-type (valid/resolve-entity-type entity-type)
        uuids       (extract-favorite-uuids-set (metadata/list-favorites (name entity-type)))]
    (->> (data/stats-by-uuids-paged user col ord limit offset uuids info-types)
      format-favorites
      (hash-map :filesystem)
      svc/success-response)))


(defn filter-favorites
  "Forwards a list of UUIDs for filesystem entries to the metadata service favorites filter
   endpoint, parsing its response and returning a set of the returned UUIDs.

   Parameters:
     entries - A list of UUIDs to filter."
  [entries]
  (extract-favorite-uuids-set (metadata/filter-favorites entries)))

(defn filter-accessible-favorites
  "Given a list of UUIDs for filesystem entries, it filters the list, returning only the UUIDS that
   are accessible and marked as favorite by the authenticated user.

   Parameters:
     body - This is the request body. It should contain a JSON document containing a field
            `filesystem` containing an array of UUIDs."
  [body]
  (let [user    (:shortUsername user/current-user)
        entries (->> body slurp parse-filesystem-ids ids-txt->uuids-set)]
    (->> (filter-favorites entries)
      (filter (partial data/uuid-accessible? user))
      (hash-map :filesystem)
      svc/success-response)))
