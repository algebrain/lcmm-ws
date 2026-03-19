(ns lcmm-ws.registry-test
  (:require [clojure.test :refer [deftest is]]
            [lcmm-ws.registry :as registry]))

(deftest subscription-handlers
  (let [reg (registry/make-registry)
        spec {:module :booking
              :topic-kind :user
              :match-topic? (fn [topic] (= :user (first topic)))
              :authorize-subscribe (fn [_] {:ok? true})}]
    (registry/register-subscription-handler! reg spec)
    (is (= spec (registry/find-subscription-handler reg [:user "u1"])))
    (is (nil? (registry/find-subscription-handler reg [:booking "b1"])))))

(deftest event-projection-registry
  (let [reg (registry/make-registry)
        spec {:module :booking
              :event-type :booking/created
              :project (fn [_] [])}]
    (registry/register-event-projection! reg spec)
    (is (= [spec] (registry/event-projections reg :booking/created)))
    (is (= [] (registry/event-projections reg :booking/deleted)))))
