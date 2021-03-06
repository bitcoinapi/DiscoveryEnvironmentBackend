(ns data-info.routes.domain.data
  (:use [compojure.api.sweet :only [describe]]
        [data-info.routes.domain.common])
  (:require [schema.core :as s]))

(s/defschema MetadataSaveRequest
  {:dest
   (describe NonBlankString "An IRODS path to a destination file where the metadata will be saved")

   :recursive
   (describe Boolean
     "When set to true and the given source is a folder, then all files and subfolders (plus all
      their files and subfolders) under the source folder will be included in the exported file,
      along with all of their metadata")})
