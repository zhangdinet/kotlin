data class DataClass(var a: Int, val b: Int = 1, val c: Int = 2)

val (a, b, c) = DataClass(0, 1, 2)
a
b
c

val (_, b1, c1) = DataClass(0, 1, 2)
b1
c1

val (a2) = DataClass(0)
a2

var (a3) = DataClass(0)
a3
a3 = 2
a3
