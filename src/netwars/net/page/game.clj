(ns netwars.net.page.game
  (:use hiccup.core
        hiccup.element
        hiccup.form
        hiccup.page))

(defn ^:private unit-info-table []
  [:table {:id "unit-details"
           ;; :style "visibility: hidden"
           }
   [:tr
    [:td {:colspan 2}
     [:canvas {:id "unit-canvas"
               :width  16
               :height 16}]]]
   [:tr
    [:th "HP:"] [:td {:id "unit-hp"}]]
   [:tr
    [:th "Fuel:"] [:td {:id "unit-fuel"}]]
   [:tr
    [:th "Movement:"] [:td {:id "unit-movement-type"}]]])

(defn ^:private terrain-info-table []
  [:table {:id "terrain-details"}
   [:tr
    [:td {:colspan 2}
     [:canvas {:id "terrain-canvas"
               :width  16
               :height 32}]]]
   [:tr
    [:th "Terrain:"] [:td {:id "terrain-name"}]]
   ;; TODO: Defense values
   ;; [:tr
   ;;  [:td "Defense Value:"] [:td {:id "terrain-defense-value"}]]
   ])

(defn ^:private player-info-table []
  [:table {:id "player-details"}
   [:tr
    [:th "Player:"] [:td {:id "player-name"}]]
   [:tr
    [:th "Funds:"] [:td {:id "player-funds"}]]])

(defn page []
  (html5
   [:head
    [:meta {:http-equiv "Content-Type"
            :content "text/html"
            :charset "UTF-8"}]
    (include-css "style.css"
                 "goog/menu.css"
                 "goog/menuitem.css"
                 "goog/menuseparator.css")]
   [:body
    [:div {:id "normal"}
     [:table {:id "mapBox"}
      [:tr
       [:td
        [:canvas {:id "gameBoard"}]]
       [:td {:id "controls"}
        [:table {:height "100%"}
         [:tr [:td (unit-info-table)]]
         [:tr [:td (terrain-info-table)]]
         [:tr [:td (player-info-table)]]
         [:tr
          [:th {:colspan 2
                :valign "bottom"}
           (submit-button {:id "end-turn-button"} "End Turn")]]]]]]]
    (include-js "netwars.js")]))
