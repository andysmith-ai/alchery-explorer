(ns explorer.api
  "Thin client for the alchery HTTP API. The explorer touches alchery only here —
  never its database or internals."
  (:require [clojure.data.json :as json]
            [clojure.string :as str]
            [explorer.config :as config])
  (:import [java.net URI URLEncoder]
           [java.net.http HttpClient HttpRequest HttpRequest$BodyPublishers
            HttpResponse$BodyHandlers]
           [java.time Duration]))

(defn- send-req [method path body]
  (let [builder (-> (HttpRequest/newBuilder (URI/create (str (config/api-url) path)))
                    (.timeout (Duration/ofSeconds 60))
                    (.header "Content-Type" "application/json"))
        req     (-> (case method
                      :get  (.GET builder)
                      :post (.POST builder (HttpRequest$BodyPublishers/ofString
                                             (json/write-str (or body {})))))
                    .build)
        resp    (.send (HttpClient/newHttpClient) req (HttpResponse$BodyHandlers/ofString))]
    {:status (.statusCode resp)
     :body   (try (json/read-str (.body resp) :key-fn keyword)
                  (catch Exception _ (.body resp)))}))

(defn- query-string [m]
  (->> m
       (keep (fn [[k v]] (when (some? v)
                           (str (name k) "=" (URLEncoder/encode (str v) "UTF-8")))))
       (str/join "&")))

(defn search     [query k] (:body (send-req :post "/search" {:query query :k k})))
(defn list-nodes [params]  (:body (send-req :get (str "/nodes?" (query-string params)) nil)))
(defn ingest-url [url]     (send-req :post "/documents" {:url url}))
(defn add-note   [text]    (send-req :post "/notes" {:text text}))

(defn get-node [id]
  (let [{:keys [status body]} (send-req :get (str "/nodes/" id) nil)]
    (when (= 200 status) body)))
