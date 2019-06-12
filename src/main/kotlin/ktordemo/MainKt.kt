package ktordemo

import com.github.jasync.sql.db.*
import com.github.jasync.sql.db.mysql.pool.*
import com.github.jasync.sql.db.pool.*
import com.google.gson.*
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
import java.util.concurrent.*


data class Data(val code: Int = 200, val msg: String = "OK", val data: RespondBody? = null)

data class RespondBody(val points: List<Point>?=null, val building: Building? = null,val buildings:List<Building>?=null, val delete:List<Point>?=null)

data class Building(val name: String, val floors: List<Floor>? = null)

data class Floor(val name: String, val content: String)

// 缺少设备id字段，后续需要添加
data class Point(val id: Long, val latitude: Double, val longitude: Double, val floor: Int, val buildingName: String="shilintong", val wifiScanRes: List<WifiScanRes> = emptyList(), val blueToothScanRes: List<BlueToothScanRes> = emptyList())

class BlueToothScanRes // not used yet

data class WifiScanRes(var id:Int=0,val ctime:String, var ress:List<OriginalRes>,var pid:Long)

data class OriginalRes(val id:Int=0,val ssid:String, val level:Int,var sid:Int)


fun Application.module() {
    install(StatusPages){
        exception<Throwable>{
            cause->
            call.respond(Data(HttpStatusCode.InternalServerError.value,cause.stackTrace.joinToString("\n")))
        }
    }

    install(ContentNegotiation) {
        gson {
//            setPrettyPrinting()
        }
    }
    install(Compression){
        gzip()
    }
    routing {
        root()
    }
}
lateinit var  connection: Connection
fun main(args: Array<String>) {
    val file = FileReader(File("config.json")).readText()
    val config = Gson().fromJson<Config>(file,Config::class.java)
    connection= ConnectionPool(
            MySQLConnectionFactory(
                    Configuration(
                            username = config.userName,
                            password = config.password,
                            host = config.dataBaseIp,
                            port = config.dataBasePort,
                            database = config.dataBaseName
                    )
            ), PoolConfiguration(
            100,
            TimeUnit.MINUTES.toMillis(1500),
            10_000,
            TimeUnit.MINUTES.toMillis(3000)
    )
    )

    connection.connect().get()
    embeddedServer(Netty, port = config.port, module = Application::module, configure = {
        responseWriteTimeoutSeconds=120
    }).start(wait = true)
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
            val floor = call.request.queryParameters["floor"]?.toInt()?:1
            val res=pointServiceImpl.getPointsByTime(call.parameters["time"]?.toLong()?:0,floor)
            call.respond(Data(data = RespondBody(points = res)))
        }
        patch {
            val id = call.parameters["time"]?.toLong()?:-1L
            if (id==-1L){
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
            val id = call.parameters["time"]?.toLong()?:-1L
            if (id==-1L){
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
            val floor = call.request.queryParameters["floor"]?.toInt()?:1
            call.respond(Data(data = RespondBody(points = pointServiceImpl.getPointsByTime(0,floor))))
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
