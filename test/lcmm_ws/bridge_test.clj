(ns lcmm-ws.bridge-test
  (:require [clojure.test :refer [deftest is]]
            [lcmm-ws.bridge :as bridge]
            [lcmm-ws.codec :as codec]
            [lcmm-ws.core :as core]
            [lcmm-ws.registry :as registry]
            [lcmm-ws.transport :as transport]))

(deftest dispatch-event-to-topic
  (let [reg (registry/make-registry)
        hub (core/make-hub)
        sent (atom [])
        fake-transport (reify transport/Transport
                         (send-text! [_ connection-id text]
                           (swap! sent conj [connection-id (codec/decode text)])
                           {:ok? true})
                         (close-connection! [_ _]
                           {:ok? true}))]
    (core/register-session! hub {:session-id "s1"
                                 :connection-id "c1"
                                 :subscriptions #{[:user "u1"]}})
    (registry/register-event-projection!
     reg
     {:module :booking
      :event-type :booking/created
      :project (fn [{:keys [envelope]}]
                 [{:topic [:user (get-in envelope [:payload :user-id])]
                   :message {:type "event"
                             :event "booking/created"
                             :payload (:payload envelope)}}])})
    (let [result (bridge/dispatch-event! {:registry reg
                                          :hub hub
                                          :transport fake-transport}
                                         :booking/created
                                         {:payload {:user-id "u1"
                                                    :booking-id "b1"}})]
      (is (= 1 (count result)))
      (is (= [["c1" {:type "event"
                     :event "booking/created"
                     :payload {:user-id "u1"
                               :booking-id "b1"}}]]
             @sent)))))
