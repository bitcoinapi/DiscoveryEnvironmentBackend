(ns metadactyl.service.apps.de.categorization
  (:use [korma.core]
        [korma.db :only [transaction]]
        [kameleon.app-groups]
        [kameleon.entities]
        [metadactyl.validation]
        [slingshot.slingshot :only [throw+]])
  (:require [clojure-commons.error-codes :as error-codes]))

(defn- categorize-app
  "Associates an app with an app category."
  [{app-id :app_id category-ids :category_ids}]
  (decategorize-app app-id)
  (dorun (map (partial add-app-to-category app-id) category-ids)))

(defn- validate-app-info
  "Validates the app information in a categorized app.  At this time, we only
  require the identifier field."
  [app-id path]
  (let [app (get-app-by-id app-id)]
    (when (nil? app)
      (throw+ {:error_code error-codes/ERR_NOT_FOUND
               :app-id     app-id
               :path       path}))))

(defn- load-category
  [category-id]
  (first (select app_categories (with app_categories) (where {:id category-id}))))

(defn- validate-category-id
  [path category-id]
  (let [category (load-category category-id)]
    (when (nil? category)
      (throw+ {:error_code error-codes/ERR_NOT_FOUND
               :path       path}))
    (when (seq (:app_categories category))
      (throw+ {:error_code error-codes/ERR_BAD_OR_MISSING_FIELD
               :reason     (str "category " category-id " contains subcategories")
               :path       path}))))

(defn- validate-category-ids
  [category-ids path]
  (when (zero? (count category-ids))
    (throw+ {:error_code error-codes/ERR_BAD_OR_MISSING_FIELD
             :path       path}))
  (dorun (map (partial validate-category-id path) category-ids)))

(defn- validate-category
  "Validates each categorized app in the request."
  [{app-id :app_id category-ids :category_ids :as category} path]
  (validate-app-info app-id path)
  (validate-category-ids category-ids path))

(defn- validate-request-body
  "Validates the request body."
  [body]
  (validate-json-object body "" #(validate-json-object-array-field
                                  % :categories %2 validate-category)))

(defn categorize-apps
  "A service that categorizes one or more apps in the database."
  [{:keys [categories] :as body}]
  (validate-request-body body)
  (transaction (dorun (map categorize-app categories))))
