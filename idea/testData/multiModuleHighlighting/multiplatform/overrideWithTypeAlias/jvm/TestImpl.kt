actual typealias ExpectedType = String

class TestImpl : Test {
    override fun doSomething(type: ExpectedType) {
        type.toString()
    }
}