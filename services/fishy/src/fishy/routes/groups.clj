(ns fishy.routes.groups
  (:use [compojure.api.sweet]
        [fishy.routes.domain.group]
        [fishy.routes.domain.params])
  (:require [fishy.service.groups :as groups]
            [fishy.util.service :as service]))

(defroutes* groups
  (GET* "/" [:as {:keys [uri]}]
        :query       [params GroupSearchParams]
        :return      GroupList
        :summary     "Group Search"
        :description "This endpoint allows callers to search for groups by name. Only groups that
        are visible to the given user will be listed. The folder name, if provided, contains the
        name of the folder to search. Any folder name provided must exactly match the name of a
        folder in the system."
        (service/trap uri groups/group-search params)))