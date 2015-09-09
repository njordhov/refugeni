(ns app.jokes
  (:require-macros
   [cljs.core.async.macros :refer [go go-loop alt!]])
  (:require
   [cljs.core.async :as async :refer [chan close! timeout put!]]
   [app.json :as json :refer [fetch-json]]
   [clojure.string :as string :refer [replace]]))

(def endpoint {:url "http://api.icndb.com/jokes/random"
               :extract #(get-in % ["value" "joke"]) })

(defn fresh-jokes
  "Channel buffering collections of n jokes from The Internet Chuck Norris Database"
  ([n] (fresh-jokes n 1))
  ([n buf & {:keys [concur] :or {concur n}}]
   (let [out (chan buf (comp
                        (map (:extract endpoint))
                        (map #(replace % #"&quot;" "\""))
                        (partition-all n)))]
     (async/pipeline-async concur out
                           (fn [url ch](fetch-json url #(put! ch % (partial close! ch))))
                             ;; Preferable but cannot do yet due to bug in core.async:
                             ;; http://dev.clojure.org/jira/browse/ASYNC-108
                             ;; (async/to-chan (repeat (:url endpoint)))
                           (let [ch (chan n)]
                             (async/onto-chan ch (repeat (:url endpoint)))
                             ch))
     out)))
