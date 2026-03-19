(ns lcmm-ws.transport)

(defprotocol Transport
  (send-text! [transport connection-id text]
    "Send text to a connection. Returns {:ok? true} or {:ok? false :reason kw}.")
  (close-connection! [transport connection-id]
    "Close a connection. Returns {:ok? true} or {:ok? false :reason kw}."))
