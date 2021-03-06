(ns metadactyl.routes.admin
  (:use [metadactyl.metadata.reference-genomes :only [add-reference-genome
                                                      delete-reference-genome
                                                      replace-reference-genomes
                                                      update-reference-genome]]
        [metadactyl.metadata.tool-requests]
        [metadactyl.routes.domain.app]
        [metadactyl.routes.domain.app.category]
        [metadactyl.routes.domain.reference-genome]
        [metadactyl.routes.domain.tool]
        [metadactyl.routes.params]
        [metadactyl.user :only [current-user]]
        [compojure.api.sweet])
  (:require [clojure-commons.error-codes :as ce]
            [metadactyl.service.apps :as apps]
            [metadactyl.util.config :as config]
            [metadactyl.util.service :as service]
            [compojure.route :as route])
  (:import [java.util UUID]))

(defroutes* admin-tool-requests
  (GET* "/" [:as {uri :uri}]
        :query [params ToolRequestListingParams]
        :return ToolRequestListing
        :summary "List Tool Requests"
        :description "This endpoint lists high level details about tool requests that have been submitted.
        Administrators may use this endpoint to track tool requests for all users."
        (service/trap uri list-tool-requests params))

  (GET* "/:request-id" [:as {uri :uri}]
        :path-params [request-id :- ToolRequestIdParam]
        :query [params SecuredQueryParams]
        :return ToolRequestDetails
        :summary "Obtain Tool Request Details"
        :description "This service obtains detailed information about a tool request. This is the service
        that the DE support team uses to obtain the request details."
        (service/trap uri get-tool-request request-id))

  (POST* "/:request-id/status" [:as {uri :uri}]
         :path-params [request-id :- ToolRequestIdParam]
         :query [params SecuredQueryParams]
         :body [body (describe ToolRequestStatusUpdate "A Tool Request status update.")]
         :return ToolRequestDetails
         :summary "Update the Status of a Tool Request"
         :description "This endpoint is used by Discovery Environment administrators to update the status
         of a tool request."
         (service/trap uri update-tool-request request-id (config/uid-domain) current-user body)))

(defroutes* admin-apps
  (POST* "/" [:as {uri :uri}]
         :query [params SecuredQueryParams]
         :body [body (describe AppCategorizationRequest "An App Categorization Request.")]
         :summary "Categorize Apps"
         :description "This endpoint is used by the Admin interface to add or move Apps to into multiple
         Categories."
         (service/trap uri apps/categorize-apps current-user body))

  (POST* "/shredder" [:as {uri :uri}]
         :query [params SecuredQueryParams]
         :body [body (describe AppDeletionRequest "List of App IDs to delete.")]
         :summary "Permanently Deleting Apps"
         :description "This service physically removes an App from the database, which allows
         administrators to completely remove Apps that are causing problems."
         (service/trap uri apps/permanently-delete-apps current-user body))

  (DELETE* "/:app-id" [:as {uri :uri}]
           :path-params [app-id :- AppIdPathParam]
           :query [params SecuredQueryParams]
           :summary "Logically Deleting an App"
           :description "An app can be marked as deleted in the DE without being completely removed from
           the database using this service. This endpoint is the same as the non-admin endpoint,
           except an error is not returned if the user does not own the App."
           (service/trap uri apps/admin-delete-app current-user app-id))

  (PATCH* "/:app-id" [:as {uri :uri}]
          :path-params [app-id :- AppIdPathParam]
          :query [params SecuredQueryParams]
          :body [body (describe AdminAppPatchRequest "The App to update.")]
          :return AppDetails
          :summary "Update App Details and Labels"
          :description "This service is capable of updating high-level information of an App, including
          'deleted' and 'disabled' flags, as well as just the labels within a single-step app that
          has already been made available for public use.
          <b>Note</b>: Although this endpoint accepts all App Group and Parameter fields within the
          'groups' array, only their 'description', 'label', and 'display' (only in parameter
          arguments) fields will be processed and updated by this endpoint."
          (service/coerced-trap uri AppDetails
                                apps/admin-update-app current-user (assoc body :id app-id)))

  (PATCH* "/:app-id/documentation" [:as {uri :uri body :body}]
          :path-params [app-id :- AppIdPathParam]
          :query [params SecuredQueryParams]
          :body [body (describe AppDocumentationRequest "The App Documentation Request.")]
          :return AppDocumentation
          :summary "Update App Documentation"
          :description "This service is used by DE administrators to update documentation for a single
          App"
          (service/coerced-trap uri AppDocumentation
                                apps/admin-edit-app-docs current-user app-id body))

  (POST* "/:app-id/documentation" [:as {uri :uri body :body}]
         :path-params [app-id :- AppIdPathParam]
         :query [params SecuredQueryParams]
         :body [body (describe AppDocumentationRequest "The App Documentation Request.")]
         :return AppDocumentation
         :summary "Add App Documentation"
         :description "This service is used by DE administrators to add documentation for a single App"
         (service/coerced-trap uri AppDocumentation
                               apps/admin-add-app-docs current-user app-id body)))

(defroutes* admin-categories
  (GET* "/" [:as {uri :uri}]
        :query [params CategoryListingParams]
        :return AppCategoryListing
        :summary "List App Categories"
        :description "This service is used by DE admins to obtain a list of public app categories along
        with the 'Trash' virtual category."
        (service/trap uri apps/get-admin-app-categories current-user params))

  (POST* "/" [:as {uri :uri}]
         :query [params SecuredQueryParams]
         :body [body (describe AppCategoryRequest "The details of the App Category to add.")]
         :return AppCategoryAppListing
         :summary "Add an App Category"
         :description "This endpoint adds an App Category under the given parent App Category, as long as
         that parent Category doesn't already have a subcategory with the given name and it doesn't
         directly contain its own Apps."
         (service/trap uri apps/admin-add-category current-user body))

  (POST* "/shredder" [:as {uri :uri}]
         :query [params SecuredQueryParams]
         :body [body (describe AppCategoryIdList "A List of App Category IDs to delete.")]
         :return AppCategoryIdList
         :summary "Delete App Categories"
         :description "App Categories can be deleted using this endpoint. The App Category and all of its
         subcategories will be deleted by this service, but no Apps will be removed. The response
         contains a list of Category IDs for which the deletion failed (including any subcategories
         of a Category already included in the request)."
         (service/trap uri apps/admin-delete-categories current-user body))

  (DELETE* "/:category-id" [:as {uri :uri}]
           :path-params [category-id :- AppCategoryIdPathParam]
           :query [params SecuredQueryParams]
           :summary "Delete an App Category"
           :description "This service physically removes an App Category from the database, along with all
           of its child Categories, as long as none of them contain any Apps."
           (service/trap uri apps/admin-delete-category current-user category-id))

  (PATCH* "/:category-id" [:as {uri :uri}]
          :path-params [category-id :- AppCategoryIdPathParam]
          :query [params SecuredQueryParams]
          :body [body (describe AppCategoryPatchRequest "Details of the App Category to update.")]
          :summary "Update an App Category"
          :description "This service renames or moves an App Category to a new parent Category, depending
          on the fields included in the request."
          (service/trap uri apps/admin-update-category current-user (assoc body :id category-id))))

(defroutes* reference-genomes
  (POST* "/" [:as {uri :uri}]
         :query [params SecuredQueryParams]
         :body [body (describe ReferenceGenomeRequest "The Reference Genome to add.")]
         :return ReferenceGenome
         :summary "Add a Reference Genome."
         :description "This endpoint adds a Reference Genome to the Discovery Environment."
         (ce/trap uri #(add-reference-genome body)))

  (PUT* "/" [:as {uri :uri}]
            :query [params SecuredQueryParams]
            :body [body (describe ReferenceGenomesSetRequest "List of Reference Genomes to set.")]
            :return ReferenceGenomesList
            :summary "Replace Reference Genomes."
            :description "This endpoint replaces ALL the Reference Genomes in the Discovery Environment,
            so if a genome is not listed in the request, it will not show up in the DE."
            (ce/trap uri #(replace-reference-genomes body)))

  (PATCH* "/:reference-genome-id" [:as {uri :uri}]
          :path-params [reference-genome-id :- ReferenceGenomeIdParam]
          :query [params SecuredQueryParams]
          :body [body (describe ReferenceGenomeRequest "The Reference Genome fields to update.")]
          :return ReferenceGenome
          :summary "Update a Reference Genome."
          :description "This endpoint modifies the name, path, and deleted fields of a Reference Genome in
          the Discovery Environment."
          (ce/trap uri #(update-reference-genome (assoc body :id reference-genome-id))))

  (DELETE* "/:reference-genome-id" [:as {uri :uri}]
           :path-params [reference-genome-id :- ReferenceGenomeIdParam]
           :query [params SecuredQueryParams]
           :summary "Delete a Reference Genome."
           :description "A Reference Genome can be marked as deleted in the DE without being completely
           removed from the database using this service. <b>Note</b>: an attempt to delete a
           Reference Genome that is already marked as deleted is treated as a no-op rather than an
           error condition. If the Reference Genome doesn't exist in the database at all, however,
           then that is treated as an error condition."
           (ce/trap uri #(delete-reference-genome reference-genome-id))))
