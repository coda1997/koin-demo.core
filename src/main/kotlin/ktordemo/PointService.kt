package ktordemo

interface PointService {
    fun getPointsByTime(time: Long): List<Point>

    fun updatePointById(id: Int, point: Point): Boolean

    fun deletePointById(id: Int): Boolean

    fun addPoint(point: Point): Boolean

}

val pointServiceImpl = PointServiceImpl()

class PointServiceImpl : PointService {

    override fun getPointsByTime(time: Long): List<Point> {
        connection.connect().get()
        val future = connection.sendPreparedStatement("select * from point where time >= $time")
        val queryRes = future.get()
        val list = queryRes.rows?.map {
            Point(it[0]!! as Long, latitude = 0,longitude = 0,floor = 0,buildingName = "shilintong")
        }
        connection.disconnect().get()
        return list?: emptyList()
    }

    override fun updatePointById(id: Int, point: Point): Boolean {
        connection.connect().get()
        val future = connection.sendPreparedStatement("select * from point where id = $id")
        val query = future.get()
        return if ( query.rows?.size?:0==0){
            connection.disconnect().get()
            false
        }else{
            val update = connection.sendPreparedStatement("update on point where id = $id")
            true
        }
    }

    override fun deletePointById(id: Int): Boolean {
        return false
    }

    override fun addPoint(point: Point): Boolean {
        return false
    }
}
