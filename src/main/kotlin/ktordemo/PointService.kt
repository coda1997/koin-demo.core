package ktordemo

interface PointService{
    fun getPointsByTime(time:Long):List<Point>

    fun updatePointById(id: Int, point: Point):Boolean

    fun deletePointById(id: Int):Boolean

    fun addPoint(point: Point):Boolean

}
val pointServiceImpl = PointServiceImpl()
class PointServiceImpl :PointService {

    override fun getPointsByTime(time: Long): List<Point> {

        return listOf()
    }

    override fun updatePointById(id: Int, point: Point): Boolean {
        return false
    }

    override fun deletePointById(id: Int): Boolean {
        return false
    }

    override fun addPoint(point: Point): Boolean {
        return false
    }
}
