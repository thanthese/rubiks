An extremely weak Rubik's Cube solver.

I'm primarily interesting in finding better algorithms for completing the cube after [F2L](http://www.speedsolving.com/wiki/index.php/First_Two_Layers).  I use a modified [Petrus method](http://lar5.com/cube/), so the edges are already aligned.  This program was written with the idea of finding 2-gen algorithms for completing the cube from this state.

I was originally inspired by [this forum post](http://www.speedsolving.com/forum/showthread.php?16047-OCELL-CPLL-a-2-gen-friendly-alternative-to-COLL-EPLL).

## Solutions found

### Twist 3 corners

All twisted corners are on `top`.  Put the untwisted corner at `top` `front` `right`.  Will rotate the `top` `back` `right` corner clockwise.

    R' U R2 U R' U R U2 R U2 R U R' U R2 U

and to rotate the corners counterclockwise:

    U' R2 U' R U' R' U2 R' U2 R' U' R U' R2 U' R
