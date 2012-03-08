An extremely weak Rubik's Cube solver.

I'm primarily interesting in finding better algorithms for completing the cube
after [F2L][f2l].  I use a modified [Petrus method][petrus] so the edges are
already aligned.  This program was written with the idea of finding [2-gen][2gen]
algorithms for completing the cube from this state.

I was originally inspired by [this forum post][post].

## Why 2-gen?

2-gen algorithms make that trade-off of longer algorithms for faster execution.
During a 2-gen algorithm, there's no need to rotate the cube or shift your
grip.

[f2l]: http//www.speedsolving.com/wiki/index.php/First_Two_Layers
[petrus]: http//lar5.com/cube/
[post]: http//www.speedsolving.com/forum/showthread.php?16047-OCELL-CPLL-a-2-gen-friendly-alternative-to-COLL-EPLL
[2gen]: http://www.speedsolving.com/wiki/index.php/2-Gen

Some of my favorite algorithms, as discovered by this program, are below.

## Permutated edges

### Permutate 3 edges

The bad edges will all be on `top`.  Position the good edge at `back`.  The
edge in `front` will move to the `left` face.

    R2 U R U R' U' R' U' R' U R'

and the inverse

    R U' R U R U R U' R' U' R2

### Permutate 4 edges (diagonals)

All incorrect edges are on `top`.  Switches the `front` and `left` edges.

    R2 U' R2 U' R' U2 R2 U2 R2 U2 R' U R2 U R2

### Permutate 4 edges (across)

All bad edges are on `top`. Throws edges `front` to `back` and `left` to
`right`.

    R2 U2 R' U2 R2 U2 R2 U2 R' U2 R2

## Twisted corners

### Twist 3 corners

All twisted corners are on `top`.  Put the untwisted corner at `top` `front`
`right`.  Will rotate the `top` `back` `right` corner clockwise.

    R' U R2 U R' U R U2 R U2 R U R' U R2 U

and to rotate the corners counterclockwise

    U' R2 U' R U' R' U2 R' U2 R' U' R U' R2 U' R

### Twist 4 corners, Pi shape

All twisted corners are on `top`.  The `top` color appears twice on the `front`
face.  One `top` color appears on the `left` face, and one on the `right`.

    U R U R2 U' R2 U' R2 U2 R2 U' R' U R U2 R'

and the inverse

    R U2 R' U' R U R2 U2 R2 U R2 U R2 U' R' U'

