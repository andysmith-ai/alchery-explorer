(ns explorer.service
  "The explorer web server: renders HTML by calling the alchery API."
  (:require [explorer.api :as api]
            [explorer.views :as views]
            [explorer.config :as config]
            [clojure.string :as str]
            [clojure.tools.logging :as log])
  (:import [com.sun.net.httpserver HttpServer HttpHandler]
           [java.net InetSocketAddress URLDecoder]))

(defn- params
  "Parse a url-encoded query string or form body into a keyword map."
  [s]
  (into {} (for [pair (some-> s (str/split #"&")) :when (seq pair)]
             (let [[k v] (str/split pair #"=" 2)]
               [(keyword (URLDecoder/decode k "UTF-8"))
                (URLDecoder/decode (or v "") "UTF-8")]))))

(defn- html! [exchange status ^String body]
  (let [bytes (.getBytes body "UTF-8")]
    (.add (.getResponseHeaders exchange) "Content-Type" "text/html; charset=utf-8")
    (.sendResponseHeaders exchange status (alength bytes))
    (with-open [os (.getResponseBody exchange)] (.write os bytes))))

(defn- redirect! [exchange to]
  (.add (.getResponseHeaders exchange) "Location" to)
  (.sendResponseHeaders exchange 303 -1)
  (.close (.getResponseBody exchange)))

(defn- seqable [x] (when (sequential? x) x))

(defn- home [exchange]
  (let [{:keys [q mode]} (params (.getQuery (.getRequestURI exchange)))
        q     (some-> q str/trim not-empty)
        nodes (seqable (cond
                         (nil? q)        (api/list-nodes {:limit 30})
                         (= mode "text") (api/list-nodes {:q q :limit 30})
                         :else           (api/search q 20)))]
    (html! exchange 200 (views/home {:q q :mode mode :nodes nodes}))))

(defn- route [exchange]
  (let [method (.getRequestMethod exchange)
        path   (.getPath (.getRequestURI exchange))]
    (cond
      (and (= method "GET") (= path "/"))
      (home exchange)

      (and (= method "POST") (= path "/ingest"))
      (do (api/ingest-url (:url (params (slurp (.getRequestBody exchange))))) (redirect! exchange "/"))

      (and (= method "POST") (= path "/note"))
      (do (api/add-note (:text (params (slurp (.getRequestBody exchange))))) (redirect! exchange "/"))

      (and (= method "GET") (str/starts-with? path "/node/"))
      (let [id (subs path (count "/node/"))]
        (if-let [n (api/get-node id)]
          (html! exchange 200 (views/node n (api/chunks id)))
          (html! exchange 404 (views/not-found))))

      :else
      (html! exchange 404 (views/not-found)))))

(defn -main [& _]
  (let [port (config/port)]
    (doto (HttpServer/create (InetSocketAddress. port) 0)
      (.createContext "/" (reify HttpHandler
                            (handle [_ exchange]
                              (try (route exchange)
                                   (catch Exception e
                                     (log/error e "request failed")
                                     (html! exchange 500 (views/not-found)))
                                   (finally (.close exchange))))))
      (.setExecutor nil)
      (.start))
    (log/info "alchery-explorer on port" port "-> api" (config/api-url))
    @(promise)))
