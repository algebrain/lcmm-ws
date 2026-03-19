(ns lcmm-ws.protocol
  (:require [lcmm-ws.codec :as codec]))

(defn wire-topic->internal [topic]
  (when (and (vector? topic)
             (= 2 (count topic))
             (string? (first topic)))
    [(keyword (first topic)) (second topic)]))

(defn internal-topic->wire [topic]
  (when (and (vector? topic)
             (= 2 (count topic))
             (keyword? (first topic)))
    [(name (first topic)) (second topic)]))

(defn parse-client-message [raw-text]
  (when (string? raw-text)
    (try
      (let [message (codec/decode raw-text)]
        (case (:type message)
          "ping"
          {:type :ping}

          "subscribe"
          (when-let [topic (wire-topic->internal (:topic message))]
            {:type :subscribe
             :topic topic})

          "unsubscribe"
          (when-let [topic (wire-topic->internal (:topic message))]
            {:type :unsubscribe
             :topic topic})

          nil))
      (catch Throwable _
        nil))))
