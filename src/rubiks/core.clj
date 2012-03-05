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

(defrecord Cube [back bottom front left right top])  ; note alphabetical order

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
  (Cube. (solid-face :red)     ; back
         (solid-face :yellow)  ; bottom
         (solid-face :orange)  ; front
         (solid-face :blue)    ; left
         (solid-face :green)   ; right
         (solid-face :white))) ; top

; I have no idea why sometimes cubes need to be compared face-by-face, and why
; sometimes = is enough.
(defn solved? [cube]
  (every? identity
          (map (fn [side] (= (side cube)
                             (side solved-cube)))
               (keys solved-cube))))

(defn R
  "Turn right face clockwise."
  [{:keys [top front left right back bottom] :as cube}]
  (Cube.
    ; back
    [(top 8) (back 1) (back 2)
     (top 5) (back 4) (back 5)
     (top 2) (back 7) (back 8)]
    ; bottom
    [(bottom 0) (bottom 1) (back 6)
     (bottom 3) (bottom 4) (back 3)
     (bottom 6) (bottom 7) (back 0)]
    ; front
    [(front 0) (front 1) (bottom 2)
     (front 3) (front 4) (bottom 5)
     (front 6) (front 7) (bottom 8)]
    ; left
    left
    ; right
    [(right 6) (right 3) (right 0)
     (right 7) (right 4) (right 1)
     (right 8) (right 5) (right 2)]
    ; top
    [(top 0) (top 1) (front 2)
     (top 3) (top 4) (front 5)
     (top 6) (top 7) (front 8)]))

(def R2 (comp R R))
(def R3 (comp R R R))

(defn U
  "Turn top face clockwise."
  [{:keys [top front left right back bottom] :as cube}]
  (Cube.
    ; back
    [(left 0) (left 1) (left 2)
     (back 3) (back 4) (back 5)
     (back 6) (back 7) (back 8)]
    ; bottom
    bottom
    ; front
    [(right 0) (right 1) (right 2)
     (front 3) (front 4) (front 5)
     (front 6) (front 7) (front 8)]
    ; left
    [(front 0) (front 1) (front 2)
     (left 3) (left 4) (left 5)
     (left 6) (left 7) (left 8)]
    ; right
    [(back 0) (back 1) (back 2)
     (right 3) (right 4) (right 5)
     (right 6) (right 7) (right 8)]
    ; top
    [(top 6) (top 3) (top 0)
     (top 7) (top 4) (top 1)
     (top 8) (top 5) (top 2)]))

(def U2 (comp U U))
(def U3 (comp U U U))

(defn move
  "Perform a series of turns on the cube."
  [cube & moves]
  (reduce (fn [c m] (m c))
          cube
          moves))

;;;  breadth-first solver  ;;;

;; THIS SECTION IS DEPRECATED!
;; depth-first prevents overflows

(defn in? [ls n] (some (partial = n) ls))
(def not-in? (comp not in?))

(defrecord History [cube move-history])

(defn all-future-threads
  "Return a set of all possible future threads (one step in advance only)."
  [{:keys [cube move-history] :as thread}]
  (let [prev-move (peek move-history)]
    (cond (or (= prev-move :R) (= prev-move :R2) (= prev-move :R3))
          #{(History. (U cube) (conj move-history :U))
            (History. (U2 cube) (conj move-history :U2))
            (History. (U3 cube) (conj move-history :U3))}
          ,,
          (or (= prev-move :U) (= prev-move :U2) (= prev-move :U3))
          #{(History. (R cube) (conj move-history :R))
            (History. (R2 cube) (conj move-history :R2))
            (History. (R3 cube) (conj move-history :R3))}
          ,,
          :else
          #{(History. (R cube) (conj move-history :R))
            (History. (R2 cube) (conj move-history :R2))
            (History. (R3 cube) (conj move-history :R3))
            (History. (U cube) (conj move-history :U))
            (History. (U2 cube) (conj move-history :U2))
            (History. (U3 cube) (conj move-history :U3))})))

(defn fail-msg-max-attempts "Failure: max attempts reached.")

(defn breadth-first-solve [initial-state max-depth]
  (loop [attempts 1
         tried-states [initial-state]
         active-threads [(History. initial-state [])]]
    (let [all-future-threads (mapcat all-future-threads active-threads)
          solved-threads (filter (fn [thread]
                                   (solved? (:cube thread)))
                                 all-future-threads)]
      ;(println (format "Step %s:  number of threads %s"
      ;                 attempts
      ;                 (count all-future-threads)))
      (cond (not (empty? solved-threads))
            (map :move-history solved-threads)
            ,,
            (>= attempts max-depth)
            fail-msg-max-attempts
            ,,
            :else
            (recur
              (inc attempts)
              (concat tried-states (map :cube all-future-threads))
              all-future-threads)))))

;;;  depth-first solver  ;;;

(def move-sym-->move {:R R
              :R2 R2
              :R3 R3
              :U U
              :U2 U2
              :U3 U3})

(defrecord Node [cube-state history])

(defn bnn [node move-sym]  ; build next node
  (Node. (move (:cube-state node) (move-sym move-sym-->move))
         (conj (:history node) move-sym)))

; optimization note: because we're doing 2-gen only, we can add a beneficial
; constraint: alternate R and U moves.
(defn depth-first-solve
  "Solve the given state, looking for solutions up to a particular depth."
  [initial-cube depth]
  (loop [queue [(Node. initial-cube [])]]
    (let [top (peek queue)
          bot (if (empty? queue) [] (pop queue))]
      (cond
        ; nothing left in queue to try
        (empty? queue)
        :solution-not-found
        ,,
        ; solution found
        (solved? (:cube-state top))
        (:history top)
        ,,
        ; end of the road for this branch
        (= depth (count (:history top)))
        (recur bot)
        ,,
        ; add options to queue
        :else
        (recur (let [prev-move (peek (:history top))]
                 (cond
                   (in? [:R :R2 :R3] prev-move)
                   (-> bot
                     (conj (bnn top :U))
                     (conj (bnn top :U2))
                     (conj (bnn top :U3)))
                   ,,
                   (in? [:U :U2 :U3] prev-move)
                   (-> bot
                     (conj (bnn top :R))
                     (conj (bnn top :R2))
                     (conj (bnn top :R3)))
                   ,,
                   :else
                   (-> bot
                     (conj (bnn top :R))
                     (conj (bnn top :R2))
                     (conj (bnn top :R3))
                     (conj (bnn top :U))
                     (conj (bnn top :U2))
                     (conj (bnn top :U3))))))))))

(defn solve
  "A harness for the depth-first solver.  Tries to solve at a depth of 1, 2, 3,
  4, ... up to max.

  Prints current depth if :print is provided."
  [initial-cube max-depth & opts]
  (loop [depth (min 5 max-depth)]  ; 5 isn't so bad anyway
    (if (in? opts :print) (println "Current depth: " depth))
    (let [result (depth-first-solve initial-cube depth)]
      (cond
        ; solution!
        (not= result :solution-not-found)
        result
        ,,
        ; too deep
        (= depth max-depth)
        :solution-not-found
        ,,
        ; keep trying
        :else
        (recur (inc depth))))))

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
  (is (solved? (move solved-cube R R R R)))
  (is (solved? (move solved-cube R2 R2)))
  (is (solved? (move solved-cube R R3))))


(deftest
  U-identities
  (is (solved? (move solved-cube U U U U)))
  (is (solved? (move solved-cube U2 U2)))
  (is (solved? (move solved-cube U U3))))

(deftest
  gibberish-move
  (is (= (move solved-cube R U R U2 R3 U3 R)
         (Cube. [g r r w r r g r r]
                [y y w y y w y y y]
                [g g b o o r o o r]
                [b g r b b b b b b]
                [o o o g g b g g o]
                [w w w o w w y y w]))))

(deftest
  test-in?
  (is (in? (range 5) 4))
  (is (not (in? (range 5) 10))))

(deftest
  test-not-in?
  (is (not-in? (range 5) 10))
  (is (not (not-in? (range 5) 4))))

(deftest
  solve-single-move-breadth
  (is (= [[:R3]] (breadth-first-solve (move solved-cube R) 1))))

(deftest
  solve-double-move-breadth
  (is (= [[:U3 :R3]] (breadth-first-solve (move solved-cube R U) 2))))

(deftest
  solve-depth-test
  (is (= fail-msg-max-attempts (breadth-first-solve (move solved-cube R U) 1))))

(deftest
  solve-triple-move
  (is (= [[:R2 :U3 :R3]] (breadth-first-solve (move solved-cube R U R2) 3))))

(deftest
  solve-4-deep-move
  (is (= [[:U2 :R2 :U3 :R3]] (breadth-first-solve (move solved-cube R U R2 U2) 4))))

(deftest
  solve-5-deep-move
  (is (= [[:R3 :U2 :R2 :U3 :R3]] (breadth-first-solve (move solved-cube R U R2 U2 R) 5))))

;;;  *very* long-running tests  ;;;

(comment

;; Optimizations
; 1.  no optimizations
; 2.  ratio of 1 (no accidental back-tracking)
; 3.  don't check that states are unique
; 4.  moved to using records for Cubes and History
; 5.  jvm-opts ["-Xmx1g" "-server"]

; 6 deep
(= [[:U2 :R3 :U3 :R :U3 :R3]]
         (time (breadth-first-solve (move solved-cube R U R3 U R U2) 6)))
; 1.  4,438 ms
; 2.  2,871 ms
; 3.     72 ms
; 4.     63 ms
; 5.     25 ms

; reverse sune (8 deep)
(= [[:U3 :R :U2 :R3 :U3 :R :U3 :R3]]
         (time (breadth-first-solve (move solved-cube R U R3 U R U2 R3 U) 8)))
; 1.  356,186 ms
; 2.  235,637 ms
; 3.      435 ms
; 4.      377 ms
; 5.      230 ms

; 10 deep
(= [[:U :R :U3 :R :U2 :R3 :U3 :R :U3 :R3]]
         (time (breadth-first-solve (move solved-cube R U R3 U R U2 R3 U R3 U3) 10)))
; 3.  out of memory
; 4.  out of memory
; 5.  2,462 ms

; 11 deep
(= [[:R3 :U :R :U3 :R :U2 :R3 :U3 :R :U3 :R3]]
         (time (breadth-first-solve (move solved-cube R U R3 U R U2 R3 U R3 U3 R) 11)))
; 5.  6,943 ms

; 12 deep
(= [[:U3 :R3 :U :R :U3 :R :U2 :R3 :U3 :R :U3 :R3]]
         (time (breadth-first-solve (move solved-cube R U R3 U R U2 R3 U R3 U3 R U) 12)))
; 5.  43,420 ms

; 15 deep (oh god!)
(= [[:R3 :U3 :R3 :U3 :R3 :U :R :U3 :R :U2 :R3 :U3 :R :U3 :R3]]
         (time (breadth-first-solve (move solved-cube R U R3 U R U2 R3 U R3 U3 R U R U R) 15)))
; 5.  ms

)

;;;  depth-first testing  ;;;

(deftest
  depth-fails-nicely
  (is (= :solution-not-found
         (depth-first-solve (move solved-cube R U R3 U R U2) 1))))

(deftest
  solve-single-move-depth
  (is (= [:R3] (depth-first-solve (move solved-cube R) 1))))

(deftest
  solve-double-move-depth
  (is (= [:U3 :R3] (depth-first-solve (move solved-cube R U) 2))))

(deftest
  solve-5-deep-move-depth
  (is (= [:R3 :U2 :R2 :U3 :R3] (depth-first-solve (move solved-cube R U R2 U2 R) 5))))

(deftest
  solve-6-deep-depth
  (is (= [:U2 :R3 :U3 :R :U3 :R3]
         (depth-first-solve (move solved-cube R U R3 U R U2) 6))))
; breadth: 25 ms
; 1.  600 ms
; 2.  100 ms: not recomputing cube state all the time
; 3.   25 ms: alternating Rs and Us

(deftest
  solve-6-deep-blind
  (is (= [:U2 :R3 :U3 :R :U3 :R3]
         (solve (move solved-cube R U R3 U R U2) 10))))
; breadth: 25 ms
; 1.  600 ms
; 2.  100 ms: not recomputing cube state all the time
; 3.   25 ms: alternating Rs and Us

(comment  ; for those long-running tests

; 10 deep
(= [:U :R :U3 :R :U2 :R3 :U3 :R :U3 :R3]
   (time (depth-first-solve (move solved-cube R U R3 U R U2 R3 U R3 U3) 10)))
; breadth:  2,462 ms
; 3.          647 ms

; 12 deep
(= [:U3 :R3 :U :R :U3 :R :U2 :R3 :U3 :R :U3 :R3]
   (time (depth-first-solve (move solved-cube R U R3 U R U2 R3 U R3 U3 R U) 12)))
; breadth:  43,420 ms
; 3.           551 ms

; 15 deep (oh god!)
(= [:U3 :R2 :U3 :R :U :R3 :U3 :R2 :U :R2 :U :R3 :U2 :R3]
   (time (depth-first-solve (move solved-cube R U R3 U R U2 R3 U R3 U3 R U R U R) 15)))
; breadth:  overflow error
; 3.        20,229 ms

)

;;;  actually using the thing to solve my problems  ;;;

(comment
  ; the solved cube, for reference
  (Cube. (solid-face :red)     ; back
         (solid-face :yellow)  ; bottom
         (solid-face :orange)  ; front
         (solid-face :blue)    ; left
         (solid-face :green)   ; right
         (solid-face :white))  ; top
)

(comment

; 3 twisted corners
; solution (17): [:U3 :R3 :U :R2 :U :R3 :U :R :U2 :R :U2 :R :U :R3 :U :R2 :U2]
(solve (Cube. [g r w
               r r r
               r r r]
              (solid-face y)
              [o o w
               o o o
               o o o]
              [r b b
               b b b
               b b b]
              [o g w
               g g g
               g g g]
              [b w r
               w w w
               w w g])
       21
       :print)

; 4 twisted corners, Pi shape
; All twisted corners are on `top`.  The `top` color appears twice on the
; `front` face.  One `top` color appears on the `left` face, and one on the
; `right`.
; solution: :U :R :U :R2 :U3 :R2 :U3 :R2 :U2 :R2 :U3 :R3 :U :R :U2 :R3
(solve (Cube. [g r b
               r r r
               r r r]
              (solid-face y)
              [w o w
               o o o
               o o o]
              [w b o
               b b b
               b b b]
              [o g w
               g g g
               g g g]
              [r w r
               w w w
               b w g])
  21
  :print)

)
