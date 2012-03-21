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
(def R' (comp R R R))

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
(def U' (comp U U U))

(defn sune
  "Perform your basic sune operation: R U R' U R U2 R' U.  (That's the form
  where doing 4 in a row takes you back to where you started, also know as the
  'swap two edges' version.)"
  [{:keys [top front left right back bottom] :as cube}]
  (Cube.
    ; back
    [(top 8) (back 1) (back 0)
     (back 3) (back 4) (back 5)
     (back 6) (back 7) (back 8)]
    ; bottom
    bottom
    ; front
    [(top 0) (left 1) (left 2)
     (front 3) (front 4) (front 5)
     (front 6) (front 7) (front 8)]
    ; left
    [(top 2) (front 1) (left 0)
     (left 3) (left 4) (left 5)
     (left 6) (left 7) (left 8)]
    ; right
    [(front 0) (right 1) (right 0)
     (right 3) (right 4) (right 5)
     (right 6) (right 7) (right 8)]
    ; top
    [(right 2) (top 1) (front 2)
     (top 7) (top 4) (top 5)
     (back 2) (top 3) (top 6)]))

(def sune'
  "The traditional sune in reverse order: U' R U2 R' U' R U' R'"
  (comp sune sune sune))

(defn m-sune
  "The mirrored sune: L' U' L U' L' U2 L U'."
  [{:keys [top front left right back bottom] :as cube}]
  (Cube.
    ; back
    [(back 2) (back 1) (top 6)
     (back 3) (back 4) (back 5)
     (back 6) (back 7) (back 8)]
    ; bottom
    bottom
    ; front
    [(right 0) (right 1) (top 2)
     (front 3) (front 4) (front 5)
     (front 6) (front 7) (front 8)]
    ; left
    [(left 2) (left 1) (front 2)
     (left 3) (left 4) (left 5)
     (left 6) (left 7) (left 8)]
    ; right
    [(right 2) (front 1) (top 0)
     (right 3) (right 4) (right 5)
     (right 6) (right 7) (right 8)]
    ; top
    [(front 0) (top 1) (left 0)
     (top 3) (top 4) (top 7)
     (top 8) (top 5) (back 0)]))

(def m-sune'
  "Mirrored sune, in reverse order: U L' U2 L U L' U L."
  (comp m-sune m-sune m-sune))

(defn move
  "Perform a series of turns on the cube."
  [cube & moves]
  (reduce #(%2 %1) cube moves))

(defn in? [ls n] (some (partial = n) ls))
(def not-in? (comp not in?))

(defn diff
  "A logical difference operation.  Remove vec of elements from vec."
  [ls ls-to-remove]
  (vec (remove (fn [elem] (in? ls-to-remove elem))
               ls)))

;;;  depth-first solver  ;;;

(def move-sym-->move {:R R, :R2 R2, :R' R'
                      :U U, :U2 U2, :U' U'
                      :sune sune, :sune' sune'
                      :m-sune m-sune, :m-sune' m-sune'})

(def all-move-syms (keys move-sym-->move))

(defrecord Node [cube-state history])

(defn build-next-node
  "Use the old new and a move symbol to generate a new node.  Remember that a
  node is made up of two things:
  1.  The current cube state (a Cube record, defining all the faces) and
  2.  A history of the moves (as symbols)"
  [node move-sym]
  (Node. ((move-sym-->move move-sym) (:cube-state node))
         (conj (:history node) move-sym)))

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
        (recur (let [prev-move (peek (:history top))
                     add-to-queue (fn [queue move-sym]
                                    (conj queue (build-next-node top move-sym)))
                     ; Restrict future moves based on past moves.  There's no
                     ; reason to knowingly go in circles.
                     move-syms (cond
                             (in? [:R :R2 :R'] prev-move)
                             (diff all-move-syms [:R :R2 :R'])
                             ,,
                             (in? [:U :U2 :U'] prev-move)
                             (diff all-move-syms [:U :U2 :U'])
                             ,,
                             (in? [:sune] prev-move)
                             (diff all-move-syms [:sune'])
                             ,,
                             (in? [:sune'] prev-move)
                             (diff all-move-syms [:sune])
                             ,,
                             (in? [:m-sune] prev-move)
                             (diff all-move-syms [:m-sune'])
                             ,,
                             (in? [:m-sune'] prev-move)
                             (diff all-move-syms [:m-sune])
                             ,,
                             :else
                             all-move-syms)]
                 (reduce add-to-queue bot move-syms)))))))

(defn solve
  "A harness for the depth-first solver.  Tries to solve at a depth of 1, 2, 3,
  4, ... up to max.

  Prints current depth if :print is provided."
  [initial-cube max-depth & opts]
  (loop [depth 1]
    (when (in? opts :print) (println "Current depth: " depth))
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
  (is (= (U2 (R' (U (R2 (U solved-cube)))))
         (move solved-cube U R2 U R' U2))))

(deftest
  R-identities
  (is (solved? (move solved-cube R R R R)))
  (is (solved? (move solved-cube R2 R2)))
  (is (solved? (move solved-cube R R'))))

(deftest
  U-identities
  (is (solved? (move solved-cube U U U U)))
  (is (solved? (move solved-cube U2 U2)))
  (is (solved? (move solved-cube U U'))))

(deftest
  sune-identities
  (is (solved? (move solved-cube sune sune sune sune)))
  (is (solved? (move solved-cube sune' sune))))

(deftest
  m-sune-identities
  (is (solved? (move solved-cube m-sune m-sune m-sune m-sune)))
  (is (solved? (move solved-cube m-sune' m-sune))))

(deftest
  gibberish-move
  (is (= (move solved-cube R U R U2 R' U' R)
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
  test-diff
  (is (= [1 3 5 6]
         (diff [1 2 3 4 5 6] [2 4]))))

;;;  depth-first testing  ;;;

(deftest
  depth-fails-nicely
  (is (= :solution-not-found
         (depth-first-solve (move solved-cube R U R' U R U2) 1))))

(deftest
  solve-single-move-depth
  (is (= [:R'] (depth-first-solve (move solved-cube R) 1))))

(deftest
  solve-double-move-depth
  (is (= [:U' :R'] (depth-first-solve (move solved-cube R U) 2))))

(deftest
  solve-5-deep-move-depth
  (is (= [:R' :U2 :R2 :U' :R'] (depth-first-solve (move solved-cube R U R2 U2 R) 5))))

(deftest
  solve-6-deep-depth
  (is (= [:R' :U :sune']
         (depth-first-solve (move solved-cube R U R' U R U2) 3))))
; 1.  600 ms
; 2.  100 ms: not recomputing cube state all the time
; 3.   25 ms: alternating Rs and Us

(deftest
  solve-6-deep-blind
  (is (= [:R' :U :sune']
         (solve (move solved-cube R U R' U R U2) 10))))
; 1.  600 ms
; 2.  100 ms: not recomputing cube state all the time
; 3.   25 ms: alternating Rs and Us

(comment  ; for those long-running tests

; 10 deep
(= [:U :R :U' :R :U2 :R' :U' :R :U' :R']
   (time (depth-first-solve (move solved-cube R U R' U R U2 R' U R' U') 10)))
; 3.  647 ms

; 12 deep
(= [:U' :R' :U :R :U' :R :U2 :R' :U' :R :U' :R']
   (time (depth-first-solve (move solved-cube R U R' U R U2 R' U R' U' R U) 12)))
; 3.  551 ms

; 13 deep
(= [:R2 :U' :R' :U :R :U' :R :U2 :R' :U' :R :U' :R']
   (time (depth-first-solve (move solved-cube R U R' U R U2 R' U R' U' R U R2) 13)))
; 3.  25,920 ms
; 4.  96,581 ms : assoc-in for updates
; 5 . 24,974 ms : refactored

; 15 deep (oh god!)
(= [:U' :R2 :U' :R :U :R' :U' :R2 :U :R2 :U :R' :U2 :R']
   (time (depth-first-solve (move solved-cube R U R' U R U2 R' U R' U' R U R U R) 15)))
; 3.  20,229 ms

)

;;;  actually using the thing to solve my problems  ;;;

(comment

; 3 twisted corners
; solution (17): [:U' :R' :U :R2 :U :R' :U :R :U2 :R :U2 :R :U :R' :U :R2 :U2]
; alt solution: [:sune :U2 :m-sune' :sune' :U']
(solve (Cube. [g r w r r r r r r]
              (solid-face y)
              [o o w o o o o o o]
              [r b b b b b b b b]
              [o g w g g g g g g]
              [b w r w w w w w g])
       21
       :print)

; 4 twisted corners, Pi shape
; All twisted corners are on `top`.  The `top` color appears twice on the
; `front` face.  One `top` color appears on the `left` face, and one on the
; `right`.
; solution: :U :R :U :R2 :U' :R2 :U' :R2 :U2 :R2 :U' :R' :U :R :U2 :R'
; alt solution: [:sune :sune :U2 :sune :sune :U2]
(solve (Cube. [g r b r r r r r r]
              (solid-face y)
              [w o w o o o o o o]
              [w b o b b b b b b]
              [o g w g g g g g g]
              [r w r w w w b w g])
  21
  :print)

; permutate 3 edges
; solution: :U' :R2 :U :R :U :R' :U' :R' :U' :R' :U :R' :U
; alt solution: [:U' :sune :m-sune :U]
(solve (Cube. [r o r r r r r r r]
              (solid-face y)
              [o b o o o o o o o]
              [b r b b b b b b b]
              (solid-face g)
              (solid-face w))
       21
       :print)

; permutate 4 edges (diagonals)
; solution: :R2 :U' :R2 :U' :R' :U2 :R2 :U2 :R2 :U2 :R' :U :R2 :U :R2
; alt solution: [:sune :U :m-sune' :sune' :U' :m-sune]
(solve (Cube. [r g r r r r r r r]
              (solid-face y)
              [o b o o o o o o o]
              [b o b b b b b b b]
              [g r g g g g g g g]
              (solid-face w))
       21
       :print)

; permutate 4 edges (across)
; solution: [:R2 :U2 :R' :U2 :R2 :U2 :R2 :U2 :R' :U2 :R2]
; alt solution: [:sune :U2 :sune :m-sune :U2 :m-sune]
(solve (Cube. [r o r r r r r r r]
              (solid-face y)
              [o r o o o o o o o]
              [b g b b b b b b b]
              [g b g g g g g g g]
              (solid-face w))
       21
       :print)

)

