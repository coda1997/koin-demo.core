package ktordemo

import com.github.jasync.sql.db.*

interface PointService {
    fun getPointsByTime(time: Long): List<Point>

    fun updatePointById(id: Int, point: Point): Boolean

    fun deletePointById(id: Int): Boolean

    fun addPoint(point: Point): Boolean

}

val pointServiceImpl = PointServiceImpl()

class PointServiceImpl : PointService {

    override fun getPointsByTime(time: Long): List<Point> {
        val future = connection.sendPreparedStatement("select * from point where pid_time >= $time")
        val queryRes = future.get()
        val list = queryRes.rows?.map {
            Point(it[0] as Long, latitude = it[1] as Double,longitude = it[2] as Double,floor = it[3] as Int)
        }
        return list?: emptyList()
    }

    override fun updatePointById(id: Int, point: Point): Boolean {
        deletePointById(id)
        addPoint(point)
        return true
    }

    override fun deletePointById(id: Int): Boolean {
        val future = connection.sendPreparedStatement("delete from point where pid_time = $id")
        future.get()
        return true
    }

    override fun addPoint(point: Point): Boolean {
        val f = connection.sendPreparedStatement("insert into point values(${point.id},${point.latitude},${point.longitude},${point.floor})")
        f.get()
        point.wifiScanRes.forEach {

            addWifiScanRes(it)
        }
        return true
    }

    private  fun addWifiScanRes(wifiScanRes: WifiScanRes){
        val result = connection.inTransaction {
            val future = it.sendPreparedStatement("insert into wifi_scan_res (ctime, pid) values('${wifiScanRes.ctime}', ${wifiScanRes.pid})")
            // val idResult = it.sendPreparedStatement()
            future.get()
            it.sendPreparedStatement("select LAST_INSERT_ID()")
        }.get()

        val id = result.rows!![0] as Int
        wifiScanRes.ress.forEach {
            addOriginalScanRes(it.ssid,it.level,id)
        }
    }

    private fun deleteOrigianlScanRes(forginKey:Int){
        val future = connection.sendPreparedStatement("delete from original_res where s_id = $forginKey")
        future.get()
    }

    private fun addOriginalScanRes(ssid:String, level:Int,forginKey:Int){
        val future = connection.sendPreparedStatement("insert into original_res (ssid,level,s_id) values ('$ssid', $level, $forginKey)")
        val query = future.get()
        println(query.statusMessage)
    }

}
