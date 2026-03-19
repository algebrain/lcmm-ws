(ns lcmm-ws.bridge
  (:require [lcmm-ws.codec :as codec]
            [lcmm-ws.core :as core]
            [lcmm-ws.registry :as registry]))

(defn dispatch-event!
  [{:keys [registry hub transport] :as _ctx} event-type envelope]
  (reduce (fn [acc {:keys [project]}]
            (let [deliveries (or (project {:envelope envelope}) [])]
              (into acc
                    (for [{:keys [topic message]} deliveries]
                      {:topic topic
                       :results (core/send-to-topic! hub
                                                     transport
                                                     topic
                                                     (codec/encode message))}))))
          []
          (registry/event-projections registry event-type)))

(defn register-event-bridge!
  [{:keys [subscribe-fn registry hub transport event-type module project] :as spec}]
  (registry/register-event-projection!
   registry
   {:module module
    :event-type event-type
    :project project})
  (when-not (ifn? subscribe-fn)
    (throw (ex-info "subscribe-fn must be a function" {:spec spec})))
  (subscribe-fn event-type
                (fn [_ envelope]
                  (dispatch-event! {:registry registry
                                    :hub hub
                                    :transport transport}
                                   event-type
                                   envelope))
                {:meta {:lcmm-ws/event-bridge true
                        :module module
                        :event-type event-type}})
  spec)
