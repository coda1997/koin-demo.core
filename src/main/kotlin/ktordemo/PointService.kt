package ktordemo

import com.github.jasync.sql.db.*
import java.lang.StringBuilder


interface PointService {
    fun getPointsByTime(time: Long,floor: Int): List<Point>

    fun updatePointById(id: Long, point: Point): Boolean

    fun deletePointById(id: Long): Boolean

    fun addPoint(point: Point): Boolean

}

val pointServiceImpl = PointServiceImpl()

class PointServiceImpl : PointService {

    override fun getPointsByTime(time: Long, floor:Int): List<Point> {
        val future = connection.sendPreparedStatement("select * from point where pid_time >= $time and floor = $floor")
        val queryRes = future.get()
        val list = queryRes.rows?.map {
            val id = it[0] as Long
            Point(id, latitude = it[1] as Double, longitude = it[2] as Double, floor = it[3] as Int, wifiScanRes = getWifiScanRes(id))
        }
        return list ?: emptyList()
    }

    override fun updatePointById(id: Long, point: Point): Boolean {
        deletePointById(id)
        addPoint(point)
        return true
    }

    override fun deletePointById(id: Long): Boolean {
        val future = connection.sendPreparedStatement("delete from point where pid_time = $id")
        future.get()
        deleteWifiScanRes(id)
        return true
    }
    private fun deleteWifiScanRes(fk:Long){
        val sids = connection.sendPreparedStatement("select id from wifi_scan_res where pid = $fk").get().rows!!
        sids.forEach {
            deleteOriginalRes(it[0] as Int)
        }
        val future = connection.sendPreparedStatement("delete from wifi_scan_res where pid = $fk")
        future.get()
    }
    private fun deleteOriginalRes(id:Int){
        val futrue = connection.sendPreparedStatement("delete from original_res where s_id = $id")
        futrue.get()
    }

    override fun addPoint(point: Point): Boolean {
        val f = connection.sendPreparedStatement("insert into point values(${point.id},${point.latitude},${point.longitude},${point.floor})")
        f.get()
        addWifiScanRess(point.wifiScanRes)
        return true
    }
    private fun addWifiScanRess(ress:List<WifiScanRes>){
        if (ress.isEmpty()){
            return
        }
        val result = connection.inTransaction { connection1 ->
            val stringBuilder = StringBuilder().append("insert into wifi_scan_res (ctime, pid) values ")
            ress.forEach { stringBuilder.append("('${it.ctime}',${it.pid})") }
            val future = connection1.sendPreparedStatement(stringBuilder.toString())
            future.get()
            connection1.sendPreparedStatement("select id from wifi_scan_res where pid = ${ress[0].pid}")
        }.get()
        val id = result.rows!!.map { it[0] }
        ress.forEachIndexed { index, wifiScanRes -> wifiScanRes.id=(id[index]as Long).toInt()}
        connection.inTransaction { c->
            val stringBuilder = StringBuilder().append("insert into original_res (ssid,level,s_id) values ")
            ress.forEach { o->
                o.ress.forEach {
                    stringBuilder.append("(${it.ssid},${it.level},${o.id})")
                }
            }
            val future = c.sendPreparedStatement(stringBuilder.toString())
            future
        }.get()
    }



//    private fun addWifiScanRes(wifiScanRes: WifiScanRes) {
//        val result = connection.inTransaction {
//            val future = it.sendPreparedStatement("insert into wifi_scan_res (ctime, pid) values('${wifiScanRes.ctime}', ${wifiScanRes.pid})")
//            // val idResult = it.sendPreparedStatement()
//            future.get()
//            it.sendPreparedStatement("select LAST_INSERT_ID()")
//        }.get()
//
//        val id = result.rows!![0][0] as Long
//        wifiScanRes.ress.forEach {
//            addOriginalScanRes(it.ssid, it.level, id.toInt())
//        }
//    }

    private fun getWifiScanRes(pid: Long): List<WifiScanRes> {
        val future = connection.sendPreparedStatement("select * from wifi_scan_res where pid = $pid")
        val scanRess = future.get().rows?:return emptyList()
        val result = mutableListOf<WifiScanRes>()
        scanRess.forEach {
            val id = it[0] as Int
            val originalRes= getOriginalScanRes(id)
            result.add(WifiScanRes(id,it[2] as String,originalRes,it[1] as Long ))
        }
        return result
    }

    private fun getOriginalScanRes(forginKey: Int): List<OriginalRes> {
        val futre = connection.sendPreparedStatement("select * from original_res where s_id = $forginKey")
        val res = futre.get().rows ?: return emptyList()
        val ress = mutableListOf<OriginalRes>()
        res.forEach {
            ress.add(OriginalRes(id = it[0] as Int, ssid = it[1] as String, level = it[2] as Int, sid = it[3] as Int))
        }
        return ress
    }

    private fun addOriginalScanRes(ssid: String, level: Int, forginKey: Int) {
        val future = connection.sendPreparedStatement("insert into original_res (ssid,level,s_id) values ('$ssid', $level, $forginKey)")
        val query = future.get()
        println(query.statusMessage)
    }

}
