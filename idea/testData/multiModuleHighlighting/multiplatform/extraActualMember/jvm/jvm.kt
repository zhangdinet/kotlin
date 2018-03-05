actual class C {
    fun zzz() {
    }
}

fun works() {
    C().zzz()
}

fun shouldWork() {
    getC()?.zzz()
}