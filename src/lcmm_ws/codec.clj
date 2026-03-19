(ns lcmm-ws.codec
  (:require [clojure.data.json :as json]))

(defn encode [value]
  (json/write-str value))

(defn decode [text]
  (json/read-str text :key-fn keyword))

(defn subscribed [topic]
  (encode {:type "subscribed"
           :topic topic}))

(defn unsubscribed [topic]
  (encode {:type "unsubscribed"
           :topic topic}))

(defn pong []
  (encode {:type "pong"}))

(defn error-message [code message]
  (encode {:type "error"
           :code code
           :message message}))

(defn event-message [{:keys [event topic payload correlation-id]}]
  (encode {:type "event"
           :event event
           :topic topic
           :payload payload
           :correlationId correlation-id}))
