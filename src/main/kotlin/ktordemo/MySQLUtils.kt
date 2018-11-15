package ktordemo

import com.github.jasync.sql.db.*
import com.github.jasync.sql.db.mysql.pool.*
import com.github.jasync.sql.db.pool.*
import java.util.concurrent.*

val connection: Connection = ConnectionPool(
        MySQLConnectionFactory(
                Configuration(
                        username = "hipe_root",
                        password = "hipe_root",
                        host = "120.78.190.36",
                        port = 3306,
                        database = "hipe"
                )
        ), PoolConfiguration(
        100,
        TimeUnit.MINUTES.toMillis(15),
        10_000,
        TimeUnit.MINUTES.toMillis(30)
)
)

