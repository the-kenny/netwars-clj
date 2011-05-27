(ns netwars.gui-utils
  (:import [java.awt Color Graphics Dimension]
           [javax.swing JFrame JPanel]))

(defn show-image [buffered-image]
  (println buffered-image)
  (let [size (Dimension. (.getWidth buffered-image) (.getHeight buffered-image))
        panel (doto (proxy [JPanel] []
                      (paint [g] (.drawImage g buffered-image 0 0 nil)))
                (.setPreferredSize size))
        frame  (new JFrame)]
    (doto frame
      (.add panel)
      (.setSize size)
      .pack
      .show
      .requestFocus
      .toFront)
    (java.awt.EventQueue/invokeLater #(doto frame .toFront .repaint))))

