(ns ^:figwheel-always cljat-webapp.app
  (:require-macros [cljs.core.async.macros :refer [go go-loop]])
  (:require [cljs.core.async :refer [<! >! chan timeout close! sliding-buffer take! put! alts!]]
            [reagent.core :as r]
            [cljsjs.react-bootstrap]))

(enable-console-print!)

(.log js/console "welcome to cljat!")

(def button (r/adapt-react-class (aget js/ReactBootstrap "Button")))
(def row (r/adapt-react-class (aget js/ReactBootstrap "Row")))
(def col (r/adapt-react-class (aget js/ReactBootstrap "Col")))
(def panel (r/adapt-react-class (aget js/ReactBootstrap "Panel")))

(def messages (r/atom [{:msg-id "msg-0000"
                        :sent-from "Jack Sparrow"
                        :sent-time "xx-xx-xxxx"
                        :msg-str "Lorem ipsum dolor sit amet, consectetur adipiscing elit. Curabitur bibendum ornare dolor, quis ullamcorper ligula sodales."
                        }

                       {:msg-id "msg-0000"
                        :sent-from "user-0"
                        :sent-time "xx-xx-xxxx"
                        :msg-str "Lorem ipsum dolor sit amet, consectetur adipiscing elit. Curabitur bibendum ornare dolor, quis ullamcorper ligula sodales."
                        }

                       {:msg-id "msg-0000"
                        :sent-from "Mike"
                        :sent-time "xx-xx-xxxx"
                        :msg-str "Lorem ipsum dolor sit amet, consectetur adipiscing elit. Curabitur bibendum ornare dolor, quis ullamcorper ligula sodales."
                        }

                       {:msg-id "msg-0000"
                        :sent-from "user-0"
                        :sent-time "xx-xx-xxxx"
                        :msg-str "Lorem ipsum dolor sit amet, consectetur adipiscing elit. Curabitur bibendum ornare dolor, quis ullamcorper ligula sodales."
                        }

                       {:msg-id "msg-0000"
                        :sent-from "Mike"
                        :sent-time "xx-xx-xxxx"
                        :msg-str "Lorem ipsum dolor sit amet, consectetur adipiscing elit. Curabitur bibendum ornare dolor, quis ullamcorper ligula sodales."
                        }]

                      ))

(def client-info (r/atom {:user-name "user-0"}))

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
       [msg-item msg])]))

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

(defn chat-container []
  (fn []
    [row {:id "chat-container"}
     [col {:md 5}
      [:div {:class "panel panel-primary"}
       [chat-box-panel-header]
       [chat-box-panel-body]
       [chat-box-panel-footer]]]]))

(defn app []
  (let [messages (r/atom {})]
    (fn []
      [:div#app-wrap.container
       [chat-container messages]])))

(defn mount-root []
  (r/render-component
   [app]
   (.getElementById js/document "app")))

(defn fig-reload []
  (.log js/console "figwheel reloaded! ")
  (mount-root))

(defn ^:export run []
  (mount-root))
