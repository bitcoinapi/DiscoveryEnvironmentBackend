(ns donkey.services.filesystem.uuids
  (:use [clj-jargon.metadata]
        [clj-jargon.permissions]
        [clojure-commons.validators]
        [slingshot.slingshot :only [throw+]]
        [donkey.services.filesystem.validators])
  (:require [clojure.tools.logging :as log]
            [clj-icat-direct.icat :as icat]
            [donkey.services.filesystem.stat :as stat]
            [cheshire.core :as json]
            [clj-jargon.init :as init]
            [clojure-commons.error-codes :as error]
            [donkey.util.config :as cfg]
            [donkey.services.filesystem.icat :as jargon])
  (:import [java.util UUID]
           [clojure.lang IPersistentMap ISeq]))


(def uuid-attr "ipc_UUID")


(defn ^IPersistentMap path-for-uuid
  "Resolves a stat info for the entity with a given UUID.

   Params:
     user - the user requesting the info
     uuid - the UUID

   Returns:
     It returns a path-stat map containing an additional UUID field."
  ([^IPersistentMap cm ^String user ^UUID uuid]
   (let [results (list-everything-with-attr-value cm uuid-attr uuid)]
     (when (empty? results)
       (throw+ {:error_code error/ERR_DOES_NOT_EXIST :uuid uuid}))
     (when (> (count results) 1)
       (log/warn "Too many results for" uuid ":" (count results))
       (log/debug "Results for" uuid ":" results)
       (throw+ {:error_code error/ERR_TOO_MANY_RESULTS
                :count      (count results)
                :uuid       uuid}))
     (if (pos? (count results))
       (merge {:uuid uuid} (stat/path-stat cm user (first results))))))
  ([^String user ^UUID uuid]
   (init/with-jargon (jargon/jargon-cfg) [cm]
     (path-for-uuid cm user uuid))))

(defn ^IPersistentMap uuid-exists?
  "Checks if an entry exists with a given UUID.

   Params:
     uuid - the UUID

   Returns:
     True if any entries were found with the given UUID, false otherwise."
  ([^IPersistentMap cm ^UUID uuid]
    (let [results (list-everything-with-attr-value cm uuid-attr uuid)]
      (pos? (count results))))
  ([^UUID uuid]
    (init/with-jargon (jargon/jargon-cfg) [cm]
      (uuid-exists? cm uuid))))


(defn paths-for-uuids
  [user uuids]
  (letfn [(id-type [type entity] (merge entity {:id (:path entity) :type type}))]
    (init/with-jargon (jargon/jargon-cfg) [cm]
      (user-exists cm user)
      (->> (concat (map (partial id-type :dir) (icat/select-folders-with-uuids uuids))
                   (map (partial id-type :file) (icat/select-files-with-uuids uuids)))
        (mapv (partial stat/decorate-stat cm user))
        (remove #(nil? (:permission %)))))))

(defn- fmt-stat
  [cm user entry]
  (let [path (:full_path entry)]
    (->> {:date-created  (* 1000 (Long/valueOf (:create_ts entry)))
          :date-modified (* 1000 (Long/valueOf (:modify_ts entry)))
          :file-size     (:data_size entry)
          :id            (:uuid entry)
          :path          path
          :type          (case (:type entry)
                           "collection" :dir
                           "dataobject" :file)}
      (stat/decorate-stat cm user))))

(defn paths-for-uuids-paged
  "Resolves the stat info for the entities with the given UUIDs. The results are paged.

   Params:
     user       - the user requesting the info
     sort-field - the stat field to sort on
     sort-order - the direction of the sort (asc|desc)
     limit      - the maximum number of results to return
     offset     - the number of results to skip before returning some
     uuids      - the UUIDS of interest
     info-types - This is info types to of the files to return. It may be nil, meaning return all
                  info types, a string containing a single info type, or a sequence containing a set
                  of info types.

   Returns:
     It returns a page of stat info maps."
  [^String  user
   ^String  sort-col
   ^String  sort-order
   ^Integer limit
   ^Integer offset
   ^ISeq    uuids
   ^ISeq    info-types]
  (let [zone (cfg/irods-zone)]
    (init/with-jargon (jargon/jargon-cfg) [cm]
      (user-exists cm user)
      (map (partial fmt-stat cm user)
           (icat/paged-uuid-listing user zone sort-col sort-order limit offset uuids info-types)))))


(defn do-paths-for-uuids
  [params body]
  (validate-map params {:user string?})
  (validate-map body {:uuids sequential?})
  (json/encode {:paths (paths-for-uuids (:user params) (:uuids body))}))

(defn uuid-for-path
  [cm user path]
  (let [attrs (get-attribute cm path uuid-attr)]
    (when-not (pos? (count attrs))
      (log/warn "Missing UUID for" path)
      (throw+ {:error_code error/ERR_NOT_FOUND :path path}))
    (if (pos? (count attrs))
      (merge {:uuid (:value (first attrs))}
             (stat/path-stat cm user path)))))

(defn uuids-for-paths
  [user paths]
  (init/with-jargon (jargon/jargon-cfg) [cm]
    (user-exists cm user)
    (all-paths-exist cm paths)
    (all-paths-readable cm user paths)
    (filter #(not (nil? %)) (mapv (partial uuid-for-path cm user) paths))))

(defn do-uuids-for-paths
  [params body]
  (log/warn body)
  (validate-map params {:user string?})
  (validate-map body {:paths sequential?})
  (json/encode {:paths (uuids-for-paths (:user params) (:paths body))}))


(defn ^Boolean uuid-accessible?
  "Indicates if a filesystem entry is readble by a given user.

   Parameters:
     user     - the authenticated name of the user
     entry-id - the UUID of the filesystem entry

   Returns:
     It returns true if the user can access the entry, otherwise false"
  [^String user ^UUID entry-id]
  (init/with-jargon (jargon/jargon-cfg) [cm]
    (let [entry-path (:path (path-for-uuid cm user (str entry-id)))]
      (and entry-path (is-readable? cm user entry-path)))))
