// FILE: lib/Anno.java
package lib;
public @interface Anno {
    String[] construct() default {};
    String value();
}

// FILE: test.kt
import lib.Anno

class Test {
    @Anno("1")
    @Anno(value = "2", construct = ["A", "B"])
    @Anno("3", construct = ["C"])
    val value: String = ""
}