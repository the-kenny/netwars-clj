(ns netwars.net.page.game
  (:use hiccup.core
        hiccup.element
        hiccup.form
        hiccup.page))

(defn ^:private unit-info-table []
  [:table {:id "unit-details"
           :style "visibility: hidden"}
   [:tr
    [:td {:colspan 2}
     [:canvas {:id "unit-canvas" :style "display: none"}]]]
   [:tr
    [:td "HP:"] [:td {:id "unit-hp"}]]
   [:tr
    [:td "Fuel:"] [:td {:id "unit-fuel"}]]])

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
       [:td {:rowspan 2}
        [:canvas {:id "gameBoard"}]]
       [:td {:id "controls"}
        (unit-info-table)
        [:div {:id "terrain-details"}]]]
      [:tr
       [:td {:valign "bottom"}
        (submit-button {:id "end-turn-button"} "End Turn")]]]]
    (include-js "netwars.js")]))
