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

(defn image-bytes [id]
  (let [req (.build (HttpRequest/newBuilder (URI/create (str (config/api-url) "/nodes/" id "/image"))))
        r   (.send (HttpClient/newHttpClient) req (HttpResponse$BodyHandlers/ofByteArray))]
    (when (= 200 (.statusCode r)) (.body r))))

(defn chunks [id]
  (let [b (:body (send-req :get (str "/nodes/" id "/chunks") nil))]
    (when (sequential? b) b)))

;; collections
(defn collections [] (let [b (:body (send-req :get "/collections" nil))] (when (sequential? b) b)))
(defn members [cid] (let [b (:body (send-req :get (str "/collections/" cid) nil))] (when (sequential? b) b)))
(defn node-collections [id]
  (let [b (:body (send-req :get (str "/nodes/" id "/collections") nil))] (when (sequential? b) b)))
(defn ensure-collection    [name]     (:body (send-req :post "/collections" {:name name})))
(defn add-to-collection    [cid node] (send-req :post (str "/collections/" cid "/add") {:node node}))
(defn remove-from-collection [cid node] (send-req :post (str "/collections/" cid "/remove") {:node node}))
