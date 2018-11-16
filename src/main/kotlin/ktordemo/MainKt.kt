package ktordemo

import io.ktor.application.*
import io.ktor.features.*
import io.ktor.gson.*
import io.ktor.http.*
import io.ktor.request.*
import io.ktor.response.*
import io.ktor.routing.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import java.io.*
import java.text.*


data class Data(val code: Int = 200, val msg: String = "OK", val data: RespondBody? = null)

data class RespondBody(val points: List<Point>?=null, val building: Building? = null,val buildings:List<Building>?=null)

data class Building(val name: String, val floors: List<Floor>? = null)

data class Floor(val name: String, val content: String)

data class Point(val id: Long, val latitude: Long, val longitude: Long, val floor: Int, val buildingName: String="shilintong", val wifiScanRes: WifiScanRes?=null, val blueToothScanRes: BlueToothScanRes?=null)

class BlueToothScanRes // not used yet

class WifiScanRes


fun Application.module() {
    install(StatusPages){
        exception<Throwable>{
            cause->
            call.respond(Data(HttpStatusCode.InternalServerError.value,cause.localizedMessage))
        }
    }

    install(ContentNegotiation) {
        gson {
            setDateFormat(DateFormat.LONG)
            setPrettyPrinting()
        }
    }
    routing {
        root()
    }
}

fun main(args: Array<String>) {
    embeddedServer(Netty, port = 8080, module = Application::module).start(wait = true)
}

fun Routing.root() {
    get("/health_check") {
        call.respondText("OK")
    }
    get("/building") {

        call.respond(Data(data = RespondBody(buildings = getBuildingNames())))
    }
    route("/building/{bid}") {

        get {
            val bid = call.parameters["bid"]?:""
            call.respond(Data(data = RespondBody(building = getBuildingInfo(bid))))
        }
    }
    route("/point/{time}"){
        get{
            val res=pointServiceImpl.getPointsByTime(call.parameters["time"]?.toLong()?:0)
            call.respond(Data(data = RespondBody(points = res)))
        }
        post {
            val id = call.parameters["time"]?.toInt()?:-1
            if (id==-1){
                call.respond(Data(code = HttpStatusCode.NotFound.value,msg = "point id cannot be -1"))
            }else{
                val point = call.receive<Point>()// receive only once
                val res = pointServiceImpl.updatePointById(id, point)
                if (res){
                    call.respond(Data())
                }else{
                    call.respond(Data(code = HttpStatusCode.NotFound.value,msg = "point $point not found"))
                }
            }
        }

        delete {
            val id = call.parameters["time"]?.toInt()?:-1
            if (id==-1){
                call.respond(Data(code = HttpStatusCode.NotFound.value,msg = "point id cannot be -1"))
            }else{
                if (pointServiceImpl.deletePointById(id)){
                    call.respond(Data())
                }else{
                    call.respond(Data(code = HttpStatusCode.NotFound.value,msg = "point id = $id not found"))
                }
            }
        }

    }
    route("/point"){
        get{
            call.respond(Data(data = RespondBody(points = pointServiceImpl.getPointsByTime(0))))
        }
        post {
            val point = call.receive<Point>()
            pointServiceImpl.addPoint(point)
            call.respond(Data())
        }
    }




}
private fun getBuildingNames():List<Building>{
    val file = File("src/building")
    val files = file.listFiles()
    return files.filter { !it.isFile }.mapTo(mutableListOf()){
        Building(it.name)
    }
}

private fun getBuildingInfo(path: String): Building {
    val file = File("src/building/$path")
    val floors = file.listFiles()
    val temp = arrayListOf<Floor>()
    floors.forEach {
        val content = FileReader(it).readText()
        temp.add(Floor(it.name.substringBefore('.'),content))
    }
    return Building(path,temp.toList())
}
