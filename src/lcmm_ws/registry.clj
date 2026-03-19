(ns lcmm-ws.registry)

(defn make-registry []
  (atom {:subscription-handlers []
         :event-projections {}}))

(defn register-subscription-handler!
  [registry {:keys [module topic-kind match-topic? authorize-subscribe] :as spec}]
  (when-not (keyword? module)
    (throw (ex-info "module must be a keyword" {:spec spec})))
  (when-not (keyword? topic-kind)
    (throw (ex-info "topic-kind must be a keyword" {:spec spec})))
  (when-not (ifn? match-topic?)
    (throw (ex-info "match-topic? must be a function" {:spec spec})))
  (when-not (ifn? authorize-subscribe)
    (throw (ex-info "authorize-subscribe must be a function" {:spec spec})))
  (swap! registry update :subscription-handlers conj spec)
  spec)

(defn register-event-projection!
  [registry {:keys [module event-type project] :as spec}]
  (when-not (keyword? module)
    (throw (ex-info "module must be a keyword" {:spec spec})))
  (when-not (keyword? event-type)
    (throw (ex-info "event-type must be a keyword" {:spec spec})))
  (when-not (ifn? project)
    (throw (ex-info "project must be a function" {:spec spec})))
  (swap! registry update-in [:event-projections event-type] (fnil conj []) spec)
  spec)

(defn find-subscription-handler [registry topic]
  (first (filter (fn [{:keys [match-topic?]}]
                   (boolean (match-topic? topic)))
                 (:subscription-handlers @registry))))

(defn event-projections [registry event-type]
  (get-in @registry [:event-projections event-type] []))
