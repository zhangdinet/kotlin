// WITH_RUNTIME
var Thread.otherName: String
    get() = getName()
    set(value) = setName<caret>(value)
