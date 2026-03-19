(ns lcmm-ws.integration-test
  (:require [clojure.test :refer [deftest is]]
            [lcmm-ws.bridge :as bridge]
            [lcmm-ws.codec :as codec]
            [lcmm-ws.core :as core]
            [lcmm-ws.http-kit :as ws.http-kit]
            [lcmm-ws.registry :as registry]
            [lcmm-ws.transport :as transport]
            [org.httpkit.server :as http-kit])
  (:import [java.net URI]
           [java.net.http HttpClient WebSocket WebSocket$Listener]
           [java.util.concurrent CompletableFuture LinkedBlockingQueue TimeUnit]))

(defn- await-result [^CompletableFuture future]
  (.get future 5 TimeUnit/SECONDS))

(defn- ws-listener [queue opened-future]
  (reify WebSocket$Listener
    (onOpen [_ web-socket]
      (.complete opened-future web-socket)
      (.request web-socket 1))
    (onText [_ web-socket data _last?]
      (.offer queue (str data))
      (.request web-socket 1)
      nil)
    (onError [_ _ error]
      (.offer queue (str "ERROR:" (.getMessage error))))
    (onClose [_ _ status-code reason]
      (.offer queue (str "CLOSE:" status-code ":" reason))
      nil)))

(defn- normalize-topic [topic]
  (-> topic
      vec
      ((fn [parts]
         (if (seq parts)
           (update parts 0 keyword)
           parts)))))

(deftest websocket-roundtrip-and-bridge
  (let [hub (core/make-hub)
        reg (registry/make-registry)
        transport-state (ws.http-kit/make-transport)
        stop-server (atom nil)
        session-id (atom nil)
        server-port 18089
        handler (fn [request]
                  (ws.http-kit/as-handler
                   request
                   {:transport-state transport-state
                    :on-open (fn [{:keys [connection-id]}]
                               (reset! session-id connection-id)
                               (core/register-session! hub {:session-id connection-id
                                                            :connection-id connection-id}))
                    :on-text (fn [{:keys [connection-id text]}]
                               (let [msg (codec/decode text)]
                                 (case (:type msg)
                                   "subscribe"
                                   (do
                                     (core/subscribe! hub connection-id (normalize-topic (:topic msg)))
                                     (transport/send-text! (:transport transport-state)
                                                           connection-id
                                                           (codec/subscribed (vec (:topic msg)))))
                                   "ping"
                                   (transport/send-text! (:transport transport-state)
                                                         connection-id
                                                         (codec/pong))
                                   nil)))
                    :on-close (fn [{:keys [connection-id]}]
                                (core/unregister-session! hub connection-id))}))
        server (http-kit/run-server handler {:port server-port})
        queue (LinkedBlockingQueue.)
        opened (CompletableFuture.)
        client (-> (HttpClient/newHttpClient)
                   (.newWebSocketBuilder)
                   (.buildAsync (URI/create (str "ws://127.0.0.1:" server-port "/ws"))
                                (ws-listener queue opened)))]
    (reset! stop-server server)
    (try
      (let [websocket (await-result client)]
        (.sendText websocket (codec/encode {:type "subscribe" :topic ["user" "u1"]}) true)
        (is (= {:type "subscribed" :topic ["user" "u1"]}
               (codec/decode (.poll queue 5 TimeUnit/SECONDS))))
        (registry/register-event-projection!
         reg
         {:module :booking
          :event-type :booking/created
          :project (fn [{:keys [envelope]}]
                     [{:topic [:user (get-in envelope [:payload :user-id])]
                       :message {:type "event"
                                 :event "booking/created"
                                 :payload (:payload envelope)}}])})
        (bridge/dispatch-event! {:registry reg
                                 :hub hub
                                 :transport (:transport transport-state)}
                                :booking/created
                                {:payload {:user-id "u1"
                                           :booking-id "b1"}})
        (is (= {:type "event"
                :event "booking/created"
                :payload {:user-id "u1"
                          :booking-id "b1"}}
               (select-keys (codec/decode (.poll queue 5 TimeUnit/SECONDS))
                            [:type :event :payload])))
        (.sendClose websocket WebSocket/NORMAL_CLOSURE "bye")
        (.get opened 5 TimeUnit/SECONDS))
      (finally
        (when-let [stop @stop-server]
          (stop))))))
