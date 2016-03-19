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
                        :msg-str "Lorem ipsum dolor sit amet, consectetur adipiscing elit. Curabitur bibendum ornare dolor, quis ullamcorper ligula sodales."
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
                        :msg-str "hehehehheehhehehe"
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

(defn sidebar-container []
  (fn []
    [:div {:id "sidebar-container" :class "cp-container"}
     [:div {:class "sidebar-panel tabbable tabs-below"}
      [:div {:class "tab-content"}
       [:div {:class "tab-pane active"}
        "sidebar pane 1"]
       [:div {:class "tab-pane"}
        "sidebar pane 2"]]
      [:ul {:class "sidebar-tabs nav nav-justified"}
       [:li {:id "contacts-tab" :class "sidebar-tab highlighted"}
        [:a "tab-one"]]
       [:li {:id "chat-tab" :class "sidebar-tab"}
        [:a "tab-two"]]]]]))

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

(defn mount-root []
  (r/render-component
   [app]
   (.getElementById js/document "app")))

(defn fig-reload []
  (.log js/console "figwheel reloaded! ")
  (mount-root))

(defn ^:export run []
  (mount-root))
