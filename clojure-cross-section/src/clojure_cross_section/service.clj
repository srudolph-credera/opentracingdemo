(ns clojure-cross-section.service
  (:require [clojure.data.json :as json]
            [io.pedestal.http :as http]
            [io.pedestal.http.body-params :as body-params]))

(def cross-section
  ;; Fetch the cross section activity values
  {:name :cross-section
   :leave
   (fn [context]
     (let [{min-x :minX
            min-y :minY
            max-x :maxX
            max-y :maxY} (get-in context [:request :query-params])]
       (assoc context :response {:status 200
                                 :body [0.5 0.68 0.22 0.005 0.37 0.87]})))})

(def to-json
  ;; Convert Clojure edn to JSON
  {:name :to-json
   :leave
   (fn [context]
     (-> context
       (update-in [:response :body] json/write-str)
       (assoc-in [:response :headers "Content-Type"] "application/json")))}) 
;; Defines "/section" route with its associated :get handler.
(def routes
  #{["/section" :get [to-json cross-section]
     :constraints {:minX #"[0-9]+"
                   :minY #"[0-9]+"
                   :maxX #"[0-9]+"
                   :maxY #"[0-9]+"}]})

;; Consumed by clojure-cross-section.server/create-server
(def service {:env :prod
              ::http/routes routes
              ::http/type :jetty
              ::http/host "localhost"
              ::http/port 8082
              ::http/container-options {:h2c? true
                                        :h2? false
                                        :ssl? false}})

