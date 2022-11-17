object PacketBuilder {

    enum class UtkastOperations(val eventName: String) {
        CREATED("created"),UPDATED("updated"),DELETED("deletec")
    }

    fun created(eventId:String, tittel: String, link: String, ident: String){

    }
}