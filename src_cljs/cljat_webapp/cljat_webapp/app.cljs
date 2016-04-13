(ns ^:figwheel-always cljat-webapp.app
  (:require-macros [cljs.core.async.macros :refer [go go-loop]])
  (:require [clojure.walk :as walk]
            [cljs.core.async :refer [<! >! chan timeout close! sliding-buffer take! put! alts!]]
            [reagent.core :as r]
            [taoensso.sente :as sente]
            [ajax.core :refer [GET POST]]
            [cljat-webapp.site]
            [cljat-webapp.comm :refer [init-ws! ajax-chan]]))

(enable-console-print!)

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Stats

(def user-info (r/atom {}))

(def friends-info (r/atom []))

(def threads-info (r/atom [{:tid 1
                            :title "corgi group"
                            :users [2 3]}]))

(def sidebar-tab-stats (r/atom {:active :threads
                                :friends {}
                                :threads {:cur 0}}))

#_(def messages (r/atom [{:timestamp 13
                        :sent-from 1
                        :sent-time "xx-xx-xxxx"
                        :msg-str "test msg test msg test msg hahhahaha"}
                       {:timestamp 14
                        :sent-from 3
                        :sent-time "xx-xx-xxxx"
                        :msg-str "test msg test msg test msg hahhahahatest msg test msg test msg hahhahahatest msg test msg test msg hahhahahatest msg test msg test msg hahhahahatest msg test msg test msg hahhahaha"}
                       {:timestamp 15
                        :sent-from 1
                        :sent-time "xx-xx-xxxx"
                        :msg-str "test msg test msg test msg hahhahaha"}
                       {:timestamp 16
                        :sent-from 1
                        :sent-time "xx-xx-xxxx"
                        :msg-str "test msg test msg test msg hahhahaha"}]))

(def threads-msg-inbox (r/atom {}))

(defn find-user [users uid]
  (->
    (filter #(= (:uid %) uid) users)
    (first)))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Messages
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

#_(defn msg-item [{:keys [msg-id sent-from sent-time msg-str]}]
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

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn user-avatar-url-fake [user-id]
  (str "/images/avatars/avatar" (inc (mod user-id 8)) ".png"))

(defn user-avatar [user]
  [:div {:class "friend-avatar"}
   [:img {:class "img-avatar pull-left" :src (user-avatar-url-fake (:uid user))}]])

(defn thread-avatar [thread]
  (fn [thread]
    (let [props-img (fn [thread]
                      (let [img (if (-> (:users thread) (count) (> 1))
                                  "/images/avatars/group.png"
                                  (user-avatar-url-fake (first (:users thread))))]
                        {:class "img-avatar pull-left" :src img}))]
      [:div {:class "thread-avatar"}
       [:img (props-img thread)]])))

(defn chat-avatar [msg]
  (fn [msg]
    (let [props (fn [msg]
                  (if (= (.-uid js/cljat) (:sent-from msg))
                    {:class "pull-right"}
                    {:class "pull-left"}))]
      [:div (props msg)
       [:img {:class "chat-avatar" :src (user-avatar-url-fake (:sent-from msg))}]])))

(defn user-profile [user]
  [:div {:class "user-profile"}
   [user-avatar user]
   [:div {:class ""}
    [:small {:class ""} (:nickname user)]
    [:div
     [:small {:class ""} (:email user)]]]])

(defn friend-list-item [friend]
  [:a {:class "friend-item list-group-item"}
   [user-avatar friend]
   [:div {:class "friend-item-body media-body"}
    [:small {:class "list-group-item-heading"} (:nickname friend)]
    [:div
     [:small {:class "list-group-item-text c-gray"} (:email friend)]]]])

(defn friends-list [friends]
  (fn [friends]
    [:div {:class "friends-list list-group"}
     (for [friend friends]
       ^{:key (:uid friend)} [friend-list-item friend])]))

(defn threads-list-item [thread]
  (fn [thread]
    (let [thread-title (if (-> (:users thread) (count) (> 1))
                         (:title thread)
                         (:nickname (->> (first (:users thread)) (find-user @friends-info))))
          props-a (fn []
                    {:id (str "thread-item-" (:tid thread)) :class (if (= (:tid thread) (get-in @sidebar-tab-stats [:threads :cur]))
                                                                     "thread-item list-group-item active"
                                                                     "thread-item list-group-item")
                     :on-click (fn [e]
                                 (let [tid (->> e
                                             (.-currentTarget)
                                             (.-id)
                                             (re-find #"\d+"))]
                                   (js/console.log "clicked thread: " tid)
                                   (reset! sidebar-tab-stats (assoc-in @sidebar-tab-stats [:threads :cur] (int tid)))))})]
      [:a (props-a)
       [thread-avatar thread]
       [:div {:class "thread-item-body media-body"}
        [:small {:class "list-group-item-heading"} thread-title]]])))

(defn threads-list [threads]
  (fn [threads]
    [:div {:class "threads-list list-group"}
     (for [thread threads]
       ^{:key (:tid thread)} [threads-list-item thread])]))


(defn sidebar-friends-pane [sidebar-stats]
  (r/create-class
    {:component-will-mount (fn [_]
                             (js/console.log "sidebar friends pane -- will mount"))
     :component-did-mount (fn [_] (js/console.log "sidebar friends pane -- did mount"))
     :reagent-render (fn [sidebar-stats]
                       [:div {:id "sidebar-pane" :class "pane"}
                        [:div {:class "title"}
                         [friends-list @friends-info]]])}))

(defn sidebar-threads-pane [sidebar-stats]
  (r/create-class
    {:component-will-mount (fn [_]
                             (js/console.log "sidebar threads pane -- will mount")
                             #_(go
                               (let [resp (<! (ajax-chan GET "/app/threads-info" {:user-id (.-uid js/cljat)}))]
                                 (reset! threads-info (->
                                                        (js->clj resp)
                                                        (walk/keywordize-keys)
                                                        (get-in [:data]))))))
     :component-did-mount (fn [_] (js/console.log "sidebar threads pane -- did mount"))
     :reagent-render (fn [sidebar-stats]
                       [:div {:id "sidebar-pane" :class "pane"}
                        [:div {:class "title"}
                         [threads-list @threads-info]]])}))

(defn sidebar-header [user]
  (r/create-class
    {:component-will-mount (fn [_]
                             (do
                               (js/console.log "sidebar header -- will mount")
                               (go
                                 (let [resp (<! (ajax-chan GET "/app/user-info" {}))]
                                   (reset! user-info (->
                                                       (js->clj resp)
                                                       (walk/keywordize-keys)
                                                       (get-in [:data])))))))
     :component-did-mount (fn [_]
                            (js/console.log "sidebar header -- did mount"))
     :component-will-update (fn [_]
                              (js/console.log "sidebar header -- will update"))
     :reagent-render (fn [user]
                       (js/console.log "sidebar header -- render" user)
                       [:div {:id "sidebar-header" :class "header"}
                        [user-profile user]])}))

(defn sidebar-pane [sidebar-stats]
  (fn [sidebar-stats]
    (if (= (:active sidebar-stats) :friends)
      [sidebar-friends-pane sidebar-stats]
      [sidebar-threads-pane sidebar-stats])))

(defn sidebar-tabs [sidebar-stats]
  (fn [sidebar-stats]
    (let [props-li (fn [id]
                     (if (= (:active sidebar-stats) id)
                       {:id (name id) :class "tab-btn active"}
                       {:id (name id) :class "tab-btn"}))
          props-a (fn [id]
                    {:on-click (fn [_]
                                 (reset! sidebar-tab-stats (update sidebar-stats :active (constantly id))))})]
      [:div {:id "sidebar-tabs" :class "bottom-tabs"}
       [:ul {:class "tabs"}
        [:li (props-li :friends)
         [:a (props-a :friends) "FRIENDS"]]
        [:li (props-li :threads)
         [:a (props-a :threads) "CHAT"]]]])))

(defn sidebar []
  (fn []
    [:div {:id "sidebar"}
     [sidebar-header @user-info]
     [sidebar-pane @sidebar-tab-stats]
     [sidebar-tabs @sidebar-tab-stats]]))

(defn chat-header []
  (fn []
    [:div {:id "chat-header" :class "header"}
     [:div {:class "title"} "Chat Header"]]))


(defn chat-msg-item [msg]
  (fn [msg]
    (let [props (fn [msg]
                  (if (= (.-uid js/cljat) (:sent-from msg))
                    {:class "message-feed right"}
                    {:class "message-feed left"}))]
      [:li {:class "msg-list-item"}
       [:div (props msg)
        [chat-avatar msg]
        [:div {:class "media-body"}
         [:div {:class "mf-content"} (:msg-str msg)]
         #_[:small {:class "mf-date"}]]]])))

(defn chat-msg-list [msgs]
  (fn [msgs]
    [:ul {:id "chat-msg-list" :class "list-group"}
     (for [msg msgs]
       ^{:key (:timestamp msg)} [chat-msg-item msg])]))

(defn chat-pane [thread-id]
  (r/create-class
    {:component-will-mount (fn [_]
                             (js/console.log "chat pane -- will mount"))
     :component-did-mount (fn [_]
                            (js/console.log "chat pane -- did mount"))
     :component-will-update (fn [_]
                              (js/console.log "chat pane -- will update"))
     :reagent-render (fn [thread-id]
                       (js/console.log "chat pane -- render: thread id: " thread-id)
                       [:div {:id "chat-pane" :class "pane"}
                        [chat-msg-list (@threads-msg-inbox thread-id)]])}))

(defn chat-input [ch-out]
  (let [input-value (r/atom "")]
    (fn []
      (let [on-change (fn [e]
                        (reset! input-value (.-value (.-target e))))
            on-key-press (fn [e]
                           (when (= 13 (.-charCode e))
                             (put! ch-out [:cljat/chat-msg {:data {:sent-from (.-uid js/cljat)
                                                                   :sent-to (get-in @sidebar-tab-stats [:threads :cur])
                                                                   :msg-str @input-value}}])
                             (js/console.log "message sent")
                             (reset! input-value "")))]
        [:div {:id "chat-input" :class "input-box"}
         [:input {:id "msg-input"
                  :value @input-value
                  :type "text"
                  :size 50
                  :placeholder "Write Message ..."
                  :on-change on-change
                  :on-key-press on-key-press}]
         [:span [:input {:id "send-btn" :type "submit" :value "send"}]]]))))

(defn chat [ch-out]
  (fn []
    [:div {:id "chat"}
     [chat-header]
     [chat-pane (get-in @sidebar-tab-stats [:threads :cur])]
     [chat-input ch-out]]))

(defn add-message [msg-data]
  (js/console.log "msg data: " (:timestamp msg-data))
  (let [sent-to (:sent-to msg-data)]
    #_(swap! messages conj msg-data)
    (swap! threads-msg-inbox (fn [cur tid new-msg-data]
                               (update cur tid (fn [v]
                                                 (if-not v
                                                   [new-msg-data]
                                                   (conj v new-msg-data))
                                                 ))) sent-to msg-data)))

(defn handle-msg [msg-id {msg-data :data}]
  (js/console.log "received msg id: " (:timestamp msg-data))
  (cond
    (= msg-id :cljat/chat-msg) (add-message msg-data)))

(defn app []
  (let [ch-out (chan)
        {:keys [ch-in stop-ws]} (init-ws! "/app/ws" ch-out (str (.-uid js/cljat)))]
    (r/create-class
      {:component-will-mount (fn [_]
                               (js/console.log "app component -- will mount")
                               
                               (go
                                 (js/console.log "ajax -- fetch friends list")
                                 (let [resp (<! (ajax-chan GET "/app/friends-info" {:user-id (.-uid js/cljat)}))]
                                   (reset! friends-info (->
                                                          (js->clj resp)
                                                          (walk/keywordize-keys)
                                                          (get-in [:data]))))

                                 (js/console.log "ajax -- fetch threads list")
                                 (let [resp (<! (ajax-chan GET "/app/threads-info" {:user-id (.-uid js/cljat)}))]
                                   (let [threads-resp (->
                                                          (js->clj resp)
                                                          (walk/keywordize-keys)
                                                          (get-in [:data]))]
                                     (reset! threads-info threads-resp)
                                     (if-not (empty? threads-resp)
                                       (reset! sidebar-tab-stats
                                         (assoc-in @sidebar-tab-stats [:threads :cur] (-> threads-resp (get 0) :tid))))))

                                 (js/console.log "ws -- send user-join msg to the ws server")
                                 #_(>! ch-in "haha")
                                 (>! ch-out [:cljat/user-join {:data {:uid (.-uid js/cljat)
                                                                      :tids (reduce #(conj %1 (:tid %2)) [] @threads-info)}}]))
                               
                               ;; message receiving loop
                               (go-loop []
                                 (let [val (<! ch-in)]
                                   (js/console.log "client recv!")
                                   (when-let [{:keys [event id ?data] :as ev-msg} val]
                                     #_(js/console.log "received msg haha id: " (-> ?data (get 0)))
                                     (handle-msg (?data 0) (?data 1))
                                     (recur)))))
       :component-did-mount (fn [_]
                              (js/console.log "app component -- did  mount"))
       :component-will-unmount (fn [_]                                 
                                 (go
                                   
                                   (js/console.log "app component -- will unmount")
                                   (<! (timeout 2000)))
                                 (stop-ws))
       :reagent-render (fn []
                         [:div {:id "window"}
                          [sidebar]
                          [chat ch-out]])})))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

#_(def msgs (r/atom [{}]))

#_(defn add-message [msgs new-msg]
  ;; keep the most recent 10 messages
  (cons new-msg msgs))

#_(defn receive-msgs [msgs recv-ch]
  ;; get the message from the receiving channel, add it to messages atom
  (go-loop []
    (let [[val ch] (alts! [recv-ch stop-ch])]
      (js/console.log "client recv!")
      (when-let [{:keys [id data event] :as ev-msg} val]
        (swap! msgs add-message event)
        (recur)))))

#_(defn send-msgs [send-fn]
  (go-loop []
    (when-let [msg (<! new-msg-ch)]
      (js/console.log "message sent!")
      (send-fn [:cljat.webapp/hello-msg {:data msg}])
      (recur))))


#_(defn message-input [new-msg-channel]
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

#_(defn message-list [msgs]
  (fn [msg]
    [:div
     [:h3 "Messages from the server"]
     [:ul
      (if-let [msgs (seq @msgs)]
        (for [msg msgs]
          ^{:key msg} [:li (pr-str msg)])
        [:li "None yet"])]]))

#_(defn message-component []
  [:div
   [message-list msgs]
   [message-input new-msg-ch]])


(defn mount-root []
  (if-let [root-dom (.getElementById js/document "app")]
    (r/render-component
      [app]
      ;;[message-component]
      root-dom)))

(defn fig-reload []
  (.log js/console "figwheel reloaded! ")
  (mount-root))

(defn ^:export run []
  (mount-root))
