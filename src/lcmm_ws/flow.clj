(ns lcmm-ws.flow
  (:require [lcmm-ws.core :as core]
            [lcmm-ws.limits :as limits]))

(defn process-subscribe!
  [{:keys [hub session-id topic max-subscriptions authorize-subscribe]}]
  (let [session (core/get-session hub session-id)
        auth-result (when (and session topic (ifn? authorize-subscribe))
                      (authorize-subscribe {:session session
                                            :topic topic}))]
    (cond
      (nil? session)
      {:ok? false :reason :session-not-found}

      (nil? topic)
      {:ok? false :reason :invalid-topic}

      (not (ifn? authorize-subscribe))
      {:ok? false :reason :subscription-rejected}

      (not (:ok? auth-result))
      {:ok? false :reason :subscription-rejected}

      (core/subscribed? hub session-id topic)
      {:ok? true :topic topic :already-subscribed? true}

      (limits/subscriptions-limit-hit? max-subscriptions session)
      {:ok? false :reason :subscription-limit-hit}

      :else
      (do
        (core/subscribe! hub session-id topic)
        {:ok? true :topic topic :already-subscribed? false}))))

(defn process-unsubscribe!
  [{:keys [hub session-id topic]}]
  (let [session (core/get-session hub session-id)]
    (cond
      (nil? session)
      {:ok? false :reason :session-not-found}

      (nil? topic)
      {:ok? false :reason :invalid-topic}

      (not (core/subscribed? hub session-id topic))
      {:ok? true :topic topic :was-subscribed? false}

      :else
      (do
        (core/unsubscribe! hub session-id topic)
        {:ok? true :topic topic :was-subscribed? true}))))
