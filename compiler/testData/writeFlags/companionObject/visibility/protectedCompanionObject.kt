class C {
    fun bar() { foo() }

    protected companion object ProtectedCompanion {
        fun foo() {}
    }
}

// TESTED_OBJECT_KIND: property
// TESTED_OBJECTS: C, ProtectedCompanion
// FLAGS: ACC_PROTECTED, ACC_FINAL, ACC_STATIC