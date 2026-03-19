(ns lcmm-ws.protocol-test
  (:require [clojure.test :refer [deftest is]]
            [lcmm-ws.codec :as codec]
            [lcmm-ws.protocol :as protocol]))

(deftest topic-conversion
  (is (= [:user "u1"] (protocol/wire-topic->internal ["user" "u1"])))
  (is (= ["user" "u1"] (protocol/internal-topic->wire [:user "u1"])))
  (is (nil? (protocol/wire-topic->internal ["user"])))
  (is (nil? (protocol/internal-topic->wire [:user])))
  (is (nil? (protocol/wire-topic->internal [:user "u1"])))
  (is (nil? (protocol/internal-topic->wire ["user" "u1"]))))

(deftest parse-client-messages
  (is (= {:type :ping}
         (protocol/parse-client-message (codec/encode {:type "ping"}))))
  (is (= {:type :subscribe
          :topic [:user "u1"]}
         (protocol/parse-client-message
          (codec/encode {:type "subscribe"
                         :topic ["user" "u1"]}))))
  (is (= {:type :unsubscribe
          :topic [:user "u1"]}
         (protocol/parse-client-message
          (codec/encode {:type "unsubscribe"
                         :topic ["user" "u1"]}))))
  (is (nil? (protocol/parse-client-message (codec/encode {:type "unknown"}))))
  (is (nil? (protocol/parse-client-message "{not-json")))
  (is (nil? (protocol/parse-client-message (codec/encode {:type "subscribe"
                                                          :topic ["user"]})))))
