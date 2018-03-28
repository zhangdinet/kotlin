// WITH_RUNTIME
// PROBLEM: none
import java.io.File

class MyFile : File("file") {
    override fun getCanonicalFile(): File {
        return super.getCanonicalFile<caret>()
    }
}
