(ns lcmm-ws.http-kit-test
  (:require [clojure.test :refer [deftest is]]
            [lcmm-ws.http-kit :as ws.http-kit]
            [lcmm-ws.transport :as transport]))

(deftest transport-registers-and-unregisters-connections
  (let [state (ws.http-kit/make-transport)
        channel (Object.)]
    (ws.http-kit/register-connection! state "c1" channel)
    (is (= channel (get @(:channels state) "c1")))
    (is (= channel (ws.http-kit/unregister-connection! state "c1")))
    (is (nil? (get @(:channels state) "c1")))))

(deftest missing-connection-results
  (let [state (ws.http-kit/make-transport)]
    (is (= {:ok? false :reason :connection-not-found}
           (transport/send-text! (:transport state) "missing" "hi")))
    (is (= {:ok? false :reason :connection-not-found}
           (transport/close-connection! (:transport state) "missing")))))
