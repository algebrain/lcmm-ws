(ns lcmm-ws.limits-test
  (:require [clojure.test :refer [deftest is]]
            [lcmm-ws.limits :as limits]))

(deftest message-too-large
  (is (false? (limits/message-too-large? 4 "test")))
  (is (true? (limits/message-too-large? 3 "test")))
  (is (false? (limits/message-too-large? nil "test"))))

(deftest subscriptions-limit
  (is (false? (limits/subscriptions-limit-hit? 2 {:subscriptions #{[:a 1]}})))
  (is (true? (limits/subscriptions-limit-hit? 1 {:subscriptions #{[:a 1]}})))
  (is (true? (limits/subscriptions-limit-hit? 0 {:subscriptions #{}}))))
