An extremely weak Rubik's Cube solver.

I'm primarily interesting in finding better algorithms for completing the cube
after [F2L][f2l].  I use a modified [Petrus method][petrus] so the edges are
already aligned.  This program was written with the idea of finding
[2-gen][2gen] algorithms for completing the cube from this state.

[This forum post][forum] was my original inspiration.

### Why 2-gen?

2-gen algorithms make the trade-off of longer algorithms for faster execution.
During a 2-gen algorithm, there's no need to rotate the cube or shift your
grip.  Additionally, they often make [finger tricks][tricks] that much easier.

[f2l]: http//www.speedsolving.com/wiki/index.php/First_Two_Layers
[petrus]: http//lar5.com/cube/
[forum]: http//www.speedsolving.com/forum/showthread.php?16047-OCELL-CPLL-a-2-gen-friendly-alternative-to-COLL-EPLL
[2gen]: http://www.speedsolving.com/wiki/index.php/2-Gen
[tricks]: http://www.cubewhiz.com/fingertricks.html

Some of my favorite algorithms, as discovered by this program, are listed in
this file.

## Notation

Since this project is only interested in solving the last layer, we can adopt a
simplified notation.  The following diagram depicts a solved last layer, as
seen from above.

      - - -
    < 0 0 0 >
    < 0 0 0 >
    < 0 0 0 >
      + + +

Thus,

    -  =  back
    +  =  front
    <  =  left
    >  =  right
    0  =  top
    .  =  sticker is in position (simplifying convention)

For the purpose of identifying patterns, the `0`s and `.`s are usually most
helpful.

## Common combinations

These combinations are so common, they're often referred to by name.  Many of
the names come from Petrus.

### Sune

Used for *orienting* corners.

    R U R' U R U2 R' U2

      0 > >
    - < . - 0
    . . . . +
    . . . > +
      . - 0

- sune' `U' R U2 R' U' R U' R'`

- mirrored sune `L' U' L U' L' U2 L U'`
- mirrored sune' `U L' U2 L U L' U L`

- niklas `L U' R' U L' U' R U`
- niklas' `U' R' U L U' R U L'`

## Permutated edges

### Permutate 3 edges

The bad edges are on `top`.  Position the good edge at `back`.  The edge in
`front` will move to the `left` face.

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

