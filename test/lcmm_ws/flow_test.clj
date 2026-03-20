(ns lcmm-ws.flow-test
  (:require [clojure.test :refer [deftest is]]
            [lcmm-ws.core :as core]
            [lcmm-ws.flow :as flow]))

(defn- authorize-subscribe [{:keys [session topic]}]
  (case (first topic)
    :user
    {:ok? (= (get-in session [:subject :user-id])
             (second topic))}

    :feed
    {:ok? true}

    {:ok? false}))

(deftest process-subscribe-success
  (let [hub (core/make-hub)]
    (core/register-session! hub {:session-id "s1"
                                 :connection-id "c1"
                                 :subject {:user-id "u1"}
                                 :subscriptions #{}})
    (is (= {:ok? true :topic [:user "u1"] :already-subscribed? false}
           (flow/process-subscribe! {:hub hub
                                     :session-id "s1"
                                     :topic [:user "u1"]
                                     :authorize-subscribe authorize-subscribe
                                     :max-subscriptions 2})))
    (is (core/subscribed? hub "s1" [:user "u1"]))))

(deftest process-subscribe-errors
  (let [hub (core/make-hub)]
    (core/register-session! hub {:session-id "s1"
                                 :connection-id "c1"
                                 :subject {:user-id "u1"}
                                 :subscriptions #{[:user "u1"] [:user "u2"]}})
    (is (= {:ok? false :reason :session-not-found}
           (flow/process-subscribe! {:hub hub
                                     :session-id "missing"
                                     :topic [:user "u1"]
                                     :authorize-subscribe authorize-subscribe
                                     :max-subscriptions 2})))
    (is (= {:ok? false :reason :invalid-topic}
           (flow/process-subscribe! {:hub hub
                                     :session-id "s1"
                                     :topic nil
                                     :authorize-subscribe authorize-subscribe
                                     :max-subscriptions 2})))
    (is (= {:ok? false :reason :subscription-rejected}
           (flow/process-subscribe! {:hub hub
                                     :session-id "s1"
                                     :topic [:booking "b1"]
                                     :authorize-subscribe authorize-subscribe
                                     :max-subscriptions 2})))
    (is (= {:ok? false :reason :subscription-rejected}
           (flow/process-subscribe! {:hub hub
                                     :session-id "s1"
                                     :topic [:user "u3"]
                                     :authorize-subscribe authorize-subscribe
                                     :max-subscriptions 5})))
    (is (= {:ok? false :reason :subscription-limit-hit}
           (flow/process-subscribe! {:hub hub
                                     :session-id "s1"
                                     :topic [:feed "main"]
                                     :authorize-subscribe authorize-subscribe
                                     :max-subscriptions 1})))
    (is (= {:ok? false :reason :subscription-limit-hit}
           (flow/process-subscribe! {:hub hub
                                     :session-id "s1"
                                     :topic [:feed "other"]
                                     :authorize-subscribe authorize-subscribe
                                     :max-subscriptions 1})))
    (is (= {:ok? false :reason :subscription-rejected}
           (flow/process-subscribe! {:hub hub
                                     :session-id "s1"
                                     :topic [:feed "main"]
                                     :max-subscriptions 5})))))

(deftest process-subscribe-repeat
  (let [hub (core/make-hub)]
    (core/register-session! hub {:session-id "s1"
                                 :connection-id "c1"
                                 :subject {:user-id "u1"}
                                 :subscriptions #{[:user "u1"]}})
    (is (= {:ok? true :topic [:user "u1"] :already-subscribed? true}
           (flow/process-subscribe! {:hub hub
                                     :session-id "s1"
                                     :topic [:user "u1"]
                                     :authorize-subscribe authorize-subscribe
                                     :max-subscriptions 5})))))

(deftest process-unsubscribe
  (let [hub (core/make-hub)]
    (core/register-session! hub {:session-id "s1"
                                 :connection-id "c1"
                                 :subject {:user-id "u1"}
                                 :subscriptions #{[:user "u1"]}})
    (is (= {:ok? true :topic [:user "u1"] :was-subscribed? true}
           (flow/process-unsubscribe! {:hub hub
                                       :session-id "s1"
                                       :topic [:user "u1"]})))
    (is (= {:ok? true :topic [:user "u1"] :was-subscribed? false}
           (flow/process-unsubscribe! {:hub hub
                                       :session-id "s1"
                                       :topic [:user "u1"]})))
    (is (= {:ok? false :reason :invalid-topic}
           (flow/process-unsubscribe! {:hub hub
                                       :session-id "s1"
                                       :topic nil})))
    (is (= {:ok? false :reason :session-not-found}
           (flow/process-unsubscribe! {:hub hub
                                       :session-id "missing"
                                       :topic [:user "u1"]})))))
