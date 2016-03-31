(ns ^:figwheel-always cljat-webapp.app
  (:require-macros [cljs.core.async.macros :refer [go go-loop]])
  (:require [cljs.core.async :refer [<! >! chan timeout close! sliding-buffer take! put! alts!]]
            [reagent.core :as r]
            [cljsjs.react-bootstrap]
            [taoensso.sente :as sente]))

(enable-console-print!)

(.log js/console "welcome to cljat!")

(def button (r/adapt-react-class (aget js/ReactBootstrap "Button")))
(def row (r/adapt-react-class (aget js/ReactBootstrap "Row")))
(def col (r/adapt-react-class (aget js/ReactBootstrap "Col")))

(def messages (r/atom [{:msg-id "msg-0000"
                        :sent-from "Jack Sparrow"
                        :sent-time "xx-xx-xxxx"
                        :msg-str "Lorem ipsum dolor sit amet, consectetur adipiscing elit. Curabitur bibendum ornare dolor, quis ullamcorper ligula sodales."
                        }

                       {:msg-id "msg-0001"
                        :sent-from "user-0"
                        :sent-time "xx-xx-xxxx"
                        :msg-str "hahahahah"
                        }

                       {:msg-id "msg-0002"
                        :sent-from "Mike"
                        :sent-time "xx-xx-xxxx"
                        :msg-str "Lorem ipsum dolor sit amet, consectetur adipiscing elit. Curabitur bibendum ornare dolor, quis ullamcorper ligula sodales."
                        }

                       {:msg-id "msg-0003"
                        :sent-from "user-0"
                        :sent-time "xx-xx-xxxx"
                        :msg-str "Lorem ipsum dolor sit amet, consectetur adipiscing elit. Curabitur bibendum ornare dolor, quis ullamcorper ligula sodalels."
                        }

                       {:msg-id "msg-0004"
                        :sent-from "Mike"
                        :sent-time "xx-xx-xxxx"
                        :msg-str "hehehehheehhehehe"
                        }
                       
                       {:msg-id "msg-0005"
                        :sent-from "Mike"
                        :sent-time "xx-xx-xxxx"
                        :msg-str "hehehehheehhehehe"
                        }

                       {:msg-id "msg-0006"
                        :sent-from "Mike"
                        :sent-time "xx-xx-xxxx"
                        :msg-str "laskdjflkjsadfjk"
                        }]
                      ))

(def client-info (r/atom {:user-name "user-0"}))
(def chat-channel-states (r/atom [{:id "u-a-b"
                                   :members ["user-a", "user-b"]}
                                  {:id "u-b-c"
                                   :members ["user-b", "user-c"]}
                                  {:id "u-b-c1"
                                   :members ["user-b", "user-c"]}
                                  {:id "u-b-c2"
                                   :members ["user-b", "user-c"]}
                                  {:id "u-b-c3"
                                   :members ["user-b", "user-c"]}
                                  {:id "u-b-c4"
                                   :members ["user-b", "user-c"]}
                                  {:id "u-b-c5"
                                   :members ["user-b", "user-c"]}
                                  {:id "u-b-c6"
                                   :members ["user-b", "user-c"]}
                                  {:id "u-b-c7"
                                   :members ["user-b", "user-c"]}
                                  {:id "u-b-c8"
                                   :members ["user-b", "user-c"]}
                                  {:id "u-b-c9"
                                   :members ["user-b", "user-c"]}
                                  {:id "u-b-c10"
                                   :members ["user-b", "user-c"]}
                                  {:id "u-b-c11"
                                   :members ["user-b", "user-c"]}
                                  {:id "u-b-c66"
                                   :members ["user-b", "user-c"]}
                                  {:id "u-b-c77"
                                   :members ["user-b", "user-c"]}
                                  {:id "u-b-c88"
                                   :members ["user-b", "user-c"]}
                                  {:id "u-b-c99"
                                   :members ["user-b", "user-c"]}
                                  {:id "u-b-c101"
                                   :members ["user-b", "user-c"]}
                                  {:id "u-b-c110"
                                   :members ["user-b", "user-c"]}
                                  {:id "u-b-c666"
                                   :members ["user-b", "user-c"]}
                                  {:id "u-b-c710"
                                   :members ["user-b", "user-c"]}
                                  {:id "u-b-c888"
                                   :members ["user-b", "user-c"]}
                                  {:id "u-b-c999"
                                   :members ["user-b", "user-c"]}
                                  {:id "u-b-c1111"
                                   :members ["user-b", "user-c"]}
                                  {:id "u-b-c1222"
                                   :members ["user-b", "user-c"]}]))

(defn is-channel-seleted? [ch]
  (:active ch))

(defn chat-channel-title-str [ch]
  (:id ch))

(def sidebar-panel-states (r/atom {:contacts {:active false
                                              :nav {:id "contacts-tab"
                                                    :icon "glyphicon-user"}}
                                   :chat {:active true
                                          :nav {:id "chat-tab"
                                                :icon "glyphicon-comment"}}}))


;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Messages
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn msg-item [{:keys [msg-id sent-from sent-time msg-str]}]
  (fn [{:keys [msg-id sent-from sent-time msg-str]}]
    (let [sent-from-me? (fn [sent-from]
                          (= sent-from (:user-name @client-info)))
          is-me (sent-from-me? sent-from)
          li-props (if is-me "right clearfix" "left clearfix")
          strong-props (if is-me "pull-right primary-font" "primary-font")
          small-props (if is-me "text-muted" "pull-right text-muted")]
      [:li {:class li-props}
       [:div {:class "msg-body clearfix"}
        [:div {:class "header"}
         [:strong {:class strong-props} (if is-me "ME" sent-from)]
         [:small {:class small-props}
          [:span {:class "glyphicon glyphicon-time"}]
          "12 mins ago"]]
        [:p msg-str]]])))

(defn thread-list []
  (fn []
    [:ul {:class "chat-thread-list"}
     (for [msg @messages]
       ^{:key msg} [msg-item msg])]))

(defn chat-box-panel-header []
  (fn []
    [:div {:id "chat-box-header" :class "panel-heading"}
     [:span {:class "glyphicon glyphicon-comment"}]
     " Chat" 
     [:div {:class "btn-group pull-right"}
      [button {:bsSize "xs"}
       [:span {:class "glyphicon glyphicon-minus icon_minim"}]]
      [button {:bsSize "xs"}
       [:span {:class "glyphicon glyphicon-remove icon_close"}]]]]))

(defn chat-box-panel-body []
  (fn []
    [:div {:class "chat-thread-container panel-body"}
     [thread-list]]))

(defn chat-box-panel-footer []
  (fn []
    [:div {:class "panel-footer"}
     [:div {:class "input-group"}
      [:input {:id "btn-input"
               :class "form-control input-sm"
               :type "text"
               :placeholder "Type your message here ..."}]
      
      [:span {:class "input-group-btn"}
       [button {:id "btn-send" :bsStyle "warning" :bsSize "sm"} "Send"]]]]))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Sidebar
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
(defn chat-channel-item [channel]
  (let [highlighted (r/atom false)]
    (fn [channel]
      (let [props-for (fn [ch]
                        {:li {:class (str "chat-channel" (if @highlighted " active"))
                              :on-mouse-over (fn [_] (swap! highlighted (constantly true)))
                              :on-mouse-out (fn [e] (swap! highlighted (constantly false)))}
                         :a {:class "clearfix"}
                         :div {:class "chat-channel-title"}})
            props (props-for channel)]
        [:li (:li props)
         [:a (:a props)
          [:div (:div props)
           [:strong (chat-channel-title-str channel)]]]]))))

(defn chat-channel-list []
  (fn []
    [:ul {:class "chat-channel-list"}
     (for [channel @chat-channel-states]
       ^{:key (:id channel)} [chat-channel-item channel])]))

(defn sidebar-panel [{:keys [name active]}]
  (fn [{:keys [name active]}]
    (js/console.log active)
    (let [props-panel (fn [active]
                        {:class (str "tab-pane" (if active " active"))})]
      [:div (props-panel active)
       (cond
         (= name :chat)
         [chat-channel-list]
         (= name :contacts) (str name))])))

(defn activate-panel [name]
  (reduce #(update-in %1
                      [%2 :active]
                      (fn [_] (if (= %2 name) true false)))
          @sidebar-panel-states
          [:chat :contacts]))

(defn sidebar-nav [{:keys [name id icon active]}]
  (fn [{:keys [name id icon active]}]
    (let [props-for-li (fn [name active]
                         {:id name
                          :class (str "sidebar-tab" (if active " highlighted"))})
          props-for-a (fn [name] {:on-click (fn [_] (reset! sidebar-panel-states (activate-panel name)))})
          props-for-icon (fn [icon] {:class (str "glyphicon " icon)})]
      
      [:li (props-for-li id active)
       [:a (props-for-a name)
        [:span (props-for-icon icon)]]])))

(defn sidebar-navs-list []
  (fn []
    (let [props-for-nav (fn [name]
                          (let [nav-states (get-in @sidebar-panel-states [name :nav])
                                active (get-in @sidebar-panel-states [name :active])]
                            (assoc nav-states :name name :active active )))]
      [:ul {:class "sidebar-tabs nav nav-justified"}
       [sidebar-nav (props-for-nav :contacts)]
       [sidebar-nav (props-for-nav :chat)]
       ])))

(defn sidebar-panel-group []
  (fn []
    (let [props-for-panel (fn [name]
                            (let [active (get-in @sidebar-panel-states [name :active])]
                              {:name name :active active}))]
      [:div {:class "tab-content"}
       [sidebar-panel (props-for-panel :contacts)]
       [sidebar-panel (props-for-panel :chat)]])))

(defn sidebar-container []
  (fn []
    [:div {:id "sidebar-container" :class "cp-container"}
     [:div {:class "sidebar-panel"}
      [sidebar-panel-group]
      [sidebar-navs-list]]]))


(defn chat-container []
  (fn []
    [:div {:id "chat-container" :class "cp-container"}
     [:div {:class "chat-panel panel panel-primary"}
      ;; [chat-box-panel-header]
      [chat-box-panel-body]
      [chat-box-panel-footer]]]))

(defn header-container []
  (fn []
    [:div {:id "header-container"}
     [:nav {:class "navbar navbar-default"}]]))

(defn app []
  (fn []
    [:div {:id "app-wrap" :class "wrap"} 
     [:div {:id "header-wrap" :class "wrap"}
      [row {:id "header-row"}
       [col {:md 12}
        [header-container]]
       ]]
     [:div {:id "main-wrap" :class "wrap"}
      [row {:id "main-row"}
       [col {:class "col" :md 4}
        [sidebar-container]]
       [col {:class "col" :md 8}
        [chat-container]]
       ]]]))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(def msgs (r/atom [{}]))
(def new-msg-ch (chan))
(def stop-ch (chan))

(defn add-message [msgs new-msg]
  ;; keep the most recent 10 messages
  (cons new-msg msgs))

(defn receive-msgs [msgs recv-ch]
  ;; get the message from the receiving channel, add it to messages atom
  (go-loop []
    (let [[val ch] (alts! [recv-ch stop-ch])]
      (js/console.log "client recv!")
      (when-let [{:keys [id data event] :as ev-msg} val]
        (swap! msgs add-message event)
        (recur)))))

(defn send-msgs [send-fn]
  (go-loop []
    (when-let [msg (<! new-msg-ch)]
      (js/console.log "message sent!")
      (send-fn [:cljat.webapp/hello-msg {:data msg}])
      (recur))))


(defn message-input [new-msg-channel]
  (let [!input-value (r/atom nil)]
    (fn [new-msg-channel]
      [:div
       [:h3 "Send a message to server:"]
       [:input {:type "text"
                :size 50
                :autofocus true
                :value @!input-value
                :on-change (fn [e]
                             (reset! !input-value (.-value (.-target e))))
                :on-key-press (fn [e]
                                (when (= 13 (.-charCode e))
                                  (put! new-msg-channel @!input-value)
                                  (reset! !input-value "")))}]])))

(defn message-list [msgs]
  (fn [msg]
    [:div
     [:h3 "Messages from the server"]
     [:ul
      (if-let [msgs (seq @msgs)]
        (for [msg msgs]
          ^{:key msg} [:li (pr-str msg)])
        [:li "None yet"])]]))

(defn message-component []
  [:div
   [message-list msgs]
   [message-input new-msg-ch]])


(defn mount-root []
  (r/render-component
   ;; [app]
   [message-component]
   (.getElementById js/document "app")))

(defn fig-reload []
  (.log js/console "figwheel reloaded! ")
  (mount-root))

(defn ^:export run []
  (let [{:keys [chsk ch-recv send-fn state]}
        (sente/make-channel-socket-client! "/ws" {:type :ws})]
    (send-msgs send-fn)
    (receive-msgs msgs ch-recv)
    (mount-root)))
