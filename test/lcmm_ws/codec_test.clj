(ns lcmm-ws.codec-test
  (:require [clojure.test :refer [deftest is]]
            [lcmm-ws.codec :as codec]))

(deftest service-messages
  (is (= {:type "subscribed" :topic ["user" "u1"]}
         (codec/decode (codec/subscribed ["user" "u1"]))))
  (is (= {:type "unsubscribed" :topic ["user" "u1"]}
         (codec/decode (codec/unsubscribed ["user" "u1"]))))
  (is (= {:type "pong"}
         (codec/decode (codec/pong))))
  (is (= {:type "error" :code "bad" :message "Bad"}
         (codec/decode (codec/error-message "bad" "Bad")))))

(deftest event-message
  (is (= {:type "event"
          :event "booking/created"
          :topic ["user" "u1"]
          :payload {:bookingId "b1"
                    :slotId "s1"
                    :note nil}
          :correlationId "c1"}
         (codec/decode
          (codec/event-message {:event "booking/created"
                                :topic ["user" "u1"]
                                :payload {:bookingId "b1"
                                          :slotId "s1"
                                          :note nil}
                                :correlation-id "c1"})))))
