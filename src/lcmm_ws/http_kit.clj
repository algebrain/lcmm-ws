(ns lcmm-ws.http-kit
  (:require [org.httpkit.server :as http-kit]
            [lcmm-ws.transport :as transport]))

(defn- send-result [ok? reason error]
  (cond-> {:ok? ok?}
    reason (assoc :reason reason)
    error (assoc :error error)))

(defn make-transport []
  (let [channels (atom {})]
    {:channels channels
     :transport
     (reify transport/Transport
       (send-text! [_ connection-id text]
         (if-let [channel (get @channels connection-id)]
           (try
             (http-kit/send! channel text false)
             (send-result true nil nil)
             (catch Throwable e
               (send-result false :send-failed e)))
           (send-result false :connection-not-found nil)))
       (close-connection! [_ connection-id]
         (if-let [channel (get @channels connection-id)]
           (do
             (swap! channels dissoc connection-id)
             (try
               (http-kit/close channel)
               (send-result true nil nil)
               (catch Throwable e
                 (send-result false :close-failed e))))
           (send-result false :connection-not-found nil))))}))

(defn register-connection!
  [{:keys [channels]} connection-id channel]
  (swap! channels assoc connection-id channel)
  connection-id)

(defn unregister-connection!
  [{:keys [channels]} connection-id]
  (let [channel (get @channels connection-id)]
    (swap! channels dissoc connection-id)
    channel))

(defn as-handler
  [request {:keys [transport-state on-open on-text on-close on-handshake-rejected connection-id-fn]
            :or {connection-id-fn #(str (java.util.UUID/randomUUID))}}]
  (if-not (:websocket? request)
    (do
      (when on-handshake-rejected
        (on-handshake-rejected request))
      {:status 400
       :headers {"Content-Type" "text/plain; charset=utf-8"}
       :body "websocket upgrade required"})
    (let [connection-id (connection-id-fn)]
      (http-kit/as-channel
       request
       {:on-open
        (fn [channel]
          (register-connection! transport-state connection-id channel)
          (when on-open
            (on-open {:connection-id connection-id
                      :channel channel
                      :request request})))
        :on-receive
        (fn [_ raw-message]
          (when on-text
            (on-text {:connection-id connection-id
                      :request request
                      :text raw-message})))
        :on-close
        (fn [_ status]
          (unregister-connection! transport-state connection-id)
          (when on-close
            (on-close {:connection-id connection-id
                       :request request
                       :status status})))}))))
