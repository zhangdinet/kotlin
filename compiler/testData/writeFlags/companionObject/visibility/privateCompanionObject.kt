class C {
    fun bar() { foo() }

    private companion object PrivateCompanion {
        fun foo() {}
    }
}

// TESTED_OBJECT_KIND: property
// TESTED_OBJECTS: C, PrivateCompanion
// FLAGS: ACC_PRIVATE, ACC_FINAL, ACC_STATIC