import default.*

object KotlinxSerialization: KotlinxDefaults {
    val json = dependency("kotlinx-serialization-json", version = "1.4.1")
}

object SulkyUlid: DependencyGroup {
    override val version get() = "8.2.0"
    override val groupId get() = "de.huxhorn.sulky"

    val sulkyUlid get() = dependency("de.huxhorn.sulky.ulid")
}

object TmsCommonLib17: DependencyGroup {
    override val groupId get() = "com.github.navikt"
    override val version get() = "1.7.1"
    val commonLib get() = dependency("tms-common-lib")

}