(ns lcmm-ws.core
  (:require [lcmm-ws.transport :as transport]))

(defn make-hub []
  (atom {:sessions {}}))

(defn register-session!
  [hub {:keys [session-id subscriptions] :as session}]
  (when-not (some? session-id)
    (throw (ex-info "session-id is required" {:session session})))
  (swap! hub assoc-in [:sessions session-id] (assoc session :subscriptions (or subscriptions #{})))
  (get-in @hub [:sessions session-id]))

(defn unregister-session!
  [hub session-id]
  (let [session (get-in @hub [:sessions session-id])]
    (swap! hub update :sessions dissoc session-id)
    session))

(defn get-session [hub session-id]
  (get-in @hub [:sessions session-id]))

(defn session-count [hub]
  (count (get @hub :sessions)))

(defn subscribe! [hub session-id topic]
  (swap! hub update-in [:sessions session-id :subscriptions] (fnil conj #{}) topic)
  (get-session hub session-id))

(defn unsubscribe! [hub session-id topic]
  (swap! hub update-in [:sessions session-id :subscriptions] (fnil disj #{}) topic)
  (get-session hub session-id))

(defn subscribed? [hub session-id topic]
  (contains? (get-in @hub [:sessions session-id :subscriptions] #{}) topic))

(defn matching-session-ids [hub topic]
  (->> (get @hub :sessions)
       (keep (fn [[session-id {:keys [subscriptions]}]]
               (when (contains? subscriptions topic)
                 session-id)))
       (into [])))

(defn send-to-session!
  [hub transport session-id text]
  (if-let [{:keys [connection-id]} (get-session hub session-id)]
    (let [result (transport/send-text! transport connection-id text)]
      (when-not (:ok? result)
        (unregister-session! hub session-id))
      result)
    {:ok? false :reason :session-not-found}))

(defn send-to-topic!
  [hub transport topic text]
  (reduce (fn [acc session-id]
            (let [result (send-to-session! hub transport session-id text)]
              (conj acc {:session-id session-id
                         :result result})))
          []
          (matching-session-ids hub topic)))
