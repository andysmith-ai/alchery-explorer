(ns explorer.config
  "Runtime configuration from the environment.")

(defn- env [k default] (or (System/getenv k) default))

(defn api-url [] (env "ALCHERY_API_URL" "http://127.0.0.1:8080"))
(defn port    [] (parse-long (env "ALCHERY_EXPLORER_PORT" "8091")))
