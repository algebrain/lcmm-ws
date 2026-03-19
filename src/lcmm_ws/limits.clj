(ns lcmm-ws.limits)

(defn message-too-large?
  [max-bytes raw-message]
  (and (int? max-bytes)
       (pos? max-bytes)
       (> (alength (.getBytes (str raw-message) "UTF-8")) max-bytes)))

(defn subscriptions-limit-hit?
  [max-subscriptions session]
  (and (int? max-subscriptions)
       (not (neg? max-subscriptions))
       (>= (count (:subscriptions session)) max-subscriptions)))
