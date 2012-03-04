(ns rubiks.core
  (:use [clojure.test]))

;;;  cube definitions and fundamental properties  ;;;

(def w :white)
(def y :yellow)
(def b :blue)
(def g :green)
(def r :red)
(def o :orange)

(defn solid-face
  "Return a solid face (a vector) of the given color."
  [color]
  (vec (repeat 9 color)))

(def solved-cube
  "
  The order works thusly.  For the :front face:

  1 2 3
  4 5 6
  7 8 9

  For the :right, :left, and :back faces, spin the cube around so that you're
  facing whichever face, and use the same scheme.  Spin the cube as though it
  were a globe.

  For the :top face, rotate the cube down (as though you were pouring coffee
  into your lap), and use the same scheme.

  For the :bottom face, same trick only this time you're pouring the coffee
  away from you.
  "
  {:top (solid-face :white)
   :front (solid-face :orange)
   :left (solid-face :blue)
   :right (solid-face :green)
   :back (solid-face :red)
   :bottom (solid-face :yellow)})

(defn R
  "Turn right face clockwise."
  [{:keys [top front left right back bottom] :as cube}]
  {:left left
   :top [(top 0) (top 1) (front 2)
         (top 3) (top 4) (front 5)
         (top 6) (top 7) (front 8)]
   :right [(right 6) (right 3) (right 0)
           (right 7) (right 4) (right 1)
           (right 8) (right 5) (right 2)]
   :back [(top 8) (back 1) (back 2)
          (top 5) (back 4) (back 5)
          (top 2) (back 7) (back 8)]
   :bottom [(bottom 0) (bottom 1) (back 6)
            (bottom 3) (bottom 4) (back 3)
            (bottom 6) (bottom 7) (back 0)]
   :front [(front 0) (front 1) (bottom 2)
           (front 3) (front 4) (bottom 5)
           (front 6) (front 7) (bottom 8)]})

(def R2 (comp R R))
(def R3 (comp R R R))

(defn U
  "Turn top face clockwise."
  [{:keys [top front left right back bottom] :as cube}]
  {:bottom bottom
   :top [(top 6) (top 3) (top 0)
         (top 7) (top 4) (top 1)
         (top 8) (top 5) (top 2)]
   :front [(right 0) (right 1) (right 2)
           (front 3) (front 4) (front 5)
           (front 6) (front 7) (front 8)]
   :left [(front 0) (front 1) (front 2)
          (left 3) (left 4) (left 5)
          (left 6) (left 7) (left 8)]
   :right [(back 0) (back 1) (back 2)
           (right 3) (right 4) (right 5)
           (right 6) (right 7) (right 8)]
   :back [(left 0) (left 1) (left 2)
          (back 3) (back 4) (back 5)
          (back 6) (back 7) (back 8)]})

(def U2 (comp U U))
(def U3 (comp U U U))

(defn move [cube & moves]
  (reduce (fn [c m] (m c))
          cube
          moves))

;;;  threads/solver  ;;;

(defn in? [ls n] (some (partial = n) ls))
(def not-in? (comp not in?))

(defn construct-thread [cube]
  {:cube cube
   :move-history []})

(defn all-future-threads
  "Return a set of all possible future threads (one step in advance only)."
  [{:keys [cube move-history] :as thread}]
  (let [last-move (peek move-history)]
    (cond (or (= last-move :R) (= last-move :R2) (= last-move :R3))
          #{{:cube (U cube) , :move-history (conj move-history :U)}
            {:cube (U2 cube) , :move-history (conj move-history :U2)}
            {:cube (U3 cube) , :move-history (conj move-history :U3)}}
          ,,
          (or (= last-move :U) (= last-move :U2) (= last-move :U3))
          #{{:cube (R cube) , :move-history (conj move-history :R)}
            {:cube (R2 cube) , :move-history (conj move-history :R2)}
            {:cube (R3 cube) , :move-history (conj move-history :R3)}}
          ,,
          :else
          #{{:cube (R cube) , :move-history (conj move-history :R)}
            {:cube (R2 cube) , :move-history (conj move-history :R2)}
            {:cube (R3 cube) , :move-history (conj move-history :R3)}
            {:cube (U cube) , :move-history (conj move-history :U)}
            {:cube (U2 cube) , :move-history (conj move-history :U2)}
            {:cube (U3 cube) , :move-history (conj move-history :U3)}})))

(defn fail-msg-max-attempts "Failure: max attempts reached.")

(defn solve [initial-state max-depth]
  (loop [attempts 1
         tried-states [initial-state]
         active-threads [(construct-thread initial-state)]]
    (let [all-future-threads (mapcat all-future-threads active-threads)
          novel-threads all-future-threads
          ;(filter (fn [thread]
          ;                        (not-in? tried-states (:cube thread)))
          ;                      all-future-threads)
          solved-threads (filter (fn [thread]
                                   (= solved-cube (:cube thread)))
                                 novel-threads)]
      (println (format "Step %s: novel threads: %s, all-future-threads %s, ratio %s"
                       attempts
                       (count novel-threads)
                       (count all-future-threads)
                       (/ (count novel-threads) (count all-future-threads))))
      (if (not (empty? solved-threads))
        (map :move-history solved-threads)
        (if (>= attempts max-depth)
          fail-msg-max-attempts
          (recur
            (inc attempts)
            (concat tried-states (map :cube novel-threads))
            novel-threads))))))

;;;  tests  ;;;

(def count-colors (comp frequencies flatten vals))
(def expected-counts {:white 9 :orange 9 :blue 9 :green 9 :red 9 :yellow 9})

(deftest
  R-produces-right-num-of-colors
  (is (= expected-counts (count-colors (R solved-cube)))))

(deftest
  U-produces-right-num-of-colors
  (is (= expected-counts (count-colors (U solved-cube)))))

(deftest
  move-shortcut
  (is (= (U2 (R3 (U (R2 (U solved-cube)))))
         (move solved-cube U R2 U R3 U2))))

(deftest
  R-identities
  (is (= solved-cube (move solved-cube R R R R)))
  (is (= solved-cube (move solved-cube R2 R2)))
  (is (= solved-cube (move solved-cube R R3))))

(deftest
  U-identities
  (is (= solved-cube (move solved-cube U U U U)))
  (is (= solved-cube (move solved-cube U2 U2)))
  (is (= solved-cube (move solved-cube U U3))))

(deftest
  gibberish-move
  (is (= (move solved-cube R U R U2 R3 U3 R)
         {:top [w w w o w w y y w]
          :front [g g b o o r o o r]
          :bottom [y y w y y w y y y]
          :left [b g r b b b b b b]
          :right [o o o g g b g g o]
          :back [g r r w r r g r r]})))

(deftest
  test-in?
  (is (in? (range 5) 4))
  (is (not (in? (range 5) 10))))

(deftest
  test-not-in?
  (is (not-in? (range 5) 10))
  (is (not (not-in? (range 5) 4))))

(deftest
  solve-single-move
  (is (= [[:R3]] (solve (move solved-cube R) 1))))

(deftest
  solve-double-move
  (is (= [[:U3 :R3]] (solve (move solved-cube R U) 2))))

(deftest
  solve-depth-test
  (is (= fail-msg-max-attempts (solve (move solved-cube R U) 1))))

(deftest
  solve-triple-move
  (is (= [[:R2 :U3 :R3]] (solve (move solved-cube R U R2) 3))))

(deftest
  solve-4-deep-move
  (is (= [[:U2 :R2 :U3 :R3]] (solve (move solved-cube R U R2 U2) 4))))

(deftest
  solve-5-deep-move
  (is (= [[:R3 :U2 :R2 :U3 :R3]] (solve (move solved-cube R U R2 U2 R) 5))))

;;;  *very* long-running tests  ;;;

(comment

; 6 deep
(= [[:U2 :R3 :U3 :R :U3 :R3]]
         (time (solve (move solved-cube R U R3 U R U2) 6)))
; 4,438 ms: no optimizations
; 2,871 ms: ratio of 1 (no accidental back-tracking)
;    72 ms: don't check that states are unique

; reverse sune (8 deep)
(= [[:U3 :R :U2 :R3 :U3 :R :U3 :R3]]
         (time (solve (move solved-cube R U R3 U R U2 R3 U) 8)))
; 356,186 ms: no optimization
; 235,637 ms: ratio of 1 (no accidental back-tracking)
;     435 ms: don't check that states are unique

; 10 deep
(= [[:U :R :U3 :R :U2 :R3 :U3 :R :U3 :R3]]
         (time (solve (move solved-cube R U R3 U R U2 R3 U R3 U3) 10)))

)
