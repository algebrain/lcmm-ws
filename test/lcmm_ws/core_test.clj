(ns lcmm-ws.core-test
  (:require [clojure.test :refer [deftest is]]
            [lcmm-ws.core :as core]
            [lcmm-ws.transport :as transport]))

(deftest session-lifecycle
  (let [hub (core/make-hub)]
    (core/register-session! hub {:session-id "s1" :connection-id "c1"})
    (is (= 1 (core/session-count hub)))
    (is (= "c1" (:connection-id (core/get-session hub "s1"))))
    (core/unregister-session! hub "s1")
    (is (zero? (core/session-count hub)))))

(deftest subscription-lifecycle
  (let [hub (core/make-hub)]
    (core/register-session! hub {:session-id "s1" :connection-id "c1"})
    (core/subscribe! hub "s1" [:user "u1"])
    (core/subscribe! hub "s1" [:user "u1"])
    (is (core/subscribed? hub "s1" [:user "u1"]))
    (is (= ["s1"] (core/matching-session-ids hub [:user "u1"])))
    (core/unsubscribe! hub "s1" [:user "u1"])
    (is (not (core/subscribed? hub "s1" [:user "u1"])))))

(deftest send-to-session-and-topic
  (let [hub (core/make-hub)
        sent (atom [])
        fake-transport (reify transport/Transport
                         (send-text! [_ connection-id text]
                           (swap! sent conj [connection-id text])
                           {:ok? true})
                         (close-connection! [_ _]
                           {:ok? true}))]
    (core/register-session! hub {:session-id "s1" :connection-id "c1" :subscriptions #{[:user "u1"]}})
    (core/register-session! hub {:session-id "s2" :connection-id "c2" :subscriptions #{[:user "u2"]}})
    (is (= {:ok? true} (core/send-to-session! hub fake-transport "s1" "hello")))
    (is (= [{:session-id "s1" :result {:ok? true}}]
           (core/send-to-topic! hub fake-transport [:user "u1"] "world")))
    (is (= [["c1" "hello"] ["c1" "world"]] @sent))))

(deftest send-failure-cleans-session
  (let [hub (core/make-hub)
        fake-transport (reify transport/Transport
                         (send-text! [_ _ _]
                           {:ok? false :reason :send-failed})
                         (close-connection! [_ _]
                           {:ok? true}))]
    (core/register-session! hub {:session-id "s1" :connection-id "c1"})
    (is (= {:ok? false :reason :send-failed}
           (core/send-to-session! hub fake-transport "s1" "boom")))
    (is (nil? (core/get-session hub "s1")))))
