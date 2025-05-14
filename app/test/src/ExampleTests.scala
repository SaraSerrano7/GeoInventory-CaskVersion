package app
import io.undertow.Undertow
import utest._

import java.sql.DriverManager
import java.nio.file.Files
import java.nio.file.Paths
import scala.app.MinimalApplication
import scala.app.MinimalApplication.getClass
import scala.io.Source

import scala.sys.process._

import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.sql.DriverManager
import scala.util.Using

object ExampleTests extends TestSuite{

  // Recoge las props de testDB
  private val testDbUrl  = "jdbc:postgresql://localhost:5432/test_LocalGeoInventory_Cask"
  private val testDbUser = "xxxxx"
  private val testDbPass = "xxxxxx"

  // Establece las props de sistema **una sola vez**, antes de arrancar Cask
  sys.props += ("JDBC_URL"  -> testDbUrl)
  sys.props += ("JDBC_USER" -> testDbUser)
  sys.props += ("JDBC_PASS" -> testDbPass)

  def withServer[T](example: cask.main.Main)(f: String => T): T = {
    val server = Undertow.builder
//      .setWorkerThreads(200)
//      .setIoThreads(Runtime.getRuntime.availableProcessors())
      .addHttpListener(8081, "localhost")
      .setHandler(example.defaultHandler)
      .build
    server.start()
    val res =
      try f("http://localhost:8081")
      finally server.stop()
    res
  }

  def dropAllTables(): Unit = {
    val dropCmd =
      """psql -U dbuser -d test_LocalGeoInventory_Cask -h localhost -c "DO $$ DECLARE r RECORD; BEGIN FOR r IN (SELECT tablename FROM pg_tables WHERE schemaname = 'public') LOOP EXECUTE 'DROP TABLE IF EXISTS public.' || quote_ident(r.tablename) || ' CASCADE'; END LOOP; END $$;" """
    val result = dropCmd.!
    assert(result == 0)
  }


  def initDb(): Unit = {
    try {
      val url_db = "jdbc:postgresql://localhost:5432/test_LocalGeoInventory_Cask"
      val user = "xxxxx"
      val password = "xxxxxx"

      val loader = getClass.getClassLoader
      val url = loader.getResource("sql/init_db.sql")

      println(s"URL de sql/init_db.sql: $url")

      if (url == null) {
        println("El archivo no está en el classpath")
      } else {
        val source = scala.io.Source.fromURL(url)
        val content = source.getLines().mkString("\n")
//        println("Contenido del archivo:\n" + content)
        source.close()
      }

      val stream = Option(getClass.getClassLoader.getResourceAsStream("sql/init_db.sql"))
        .getOrElse(throw new RuntimeException("No se encontró sql/init_db.sql en resources"))
      val sql = Source.fromInputStream(stream).mkString
      stream.close()

      val connection = DriverManager.getConnection(url_db, user, password)
      val statement = connection.createStatement()

      println("[initDb] Ejecutando script completo…")
      statement.execute(sql)

      statement.close()
      connection.close()
      println("[initDb] Tablas creadas o ya existían.")

    } catch {
      case e: Throwable =>
        System.err.println(s"[initDb] ERROR ejecutando init_db.sql: ${e.getMessage}")
        e.printStackTrace()
    }

  }

  def check_db_content(): Unit = {
    Using.Manager { use =>
//      val conn = use(DriverManager.getConnection("jdbc:postgresql://localhost:5432/tu_db", "usuario", "contraseña"))
      val url_db = "jdbc:postgresql://localhost:5432/LocalGeoInventory_Cask"
      val user = "xxxxxx"
      val password = "xxxxxx"

      val conn = DriverManager.getConnection(url_db, user, password)

      val stmt = use(conn.createStatement())

      val rs1 = use(stmt.executeQuery("SELECT COUNT(*) FROM geojson_feature"))
      rs1.next()
      val countFeatures = rs1.getInt(1)

      val rs2 = use(stmt.executeQuery("SELECT COUNT(*) FROM geojson_feature_properties"))
      rs2.next()
      val countProps = rs2.getInt(1)

      println(s"📊 Registros: geojson_feature = $countFeatures, geojson_feature_properties = $countProps")
    }
  }

  val tests = Tests {

    test("UploadGeoJSONFilesPerformance") - withServer(MinimalApplication) { host =>

      val formatter = DateTimeFormatter.ofPattern("HH:mm:ss")


      val resourceDir = getClass.getClassLoader.getResource("files")
      val dir = new java.io.File(resourceDir.toURI)
      val geojsons = Option(dir.listFiles).getOrElse(Array.empty).filter(_.getName.endsWith(".geojson"))



      val resultados = scala.collection.mutable.ListBuffer.empty[(String, Long)]

      var now = LocalTime.now().format(formatter)
      println(s"\n🕒 Hora: $now - Preparando DB")

//      dropAllTables()
      initDb()

      now = LocalTime.now().format(formatter)
      println(s"\n🕒 Hora: $now - DB lista")


      for (file <- geojsons) {
        System.gc()
        check_db_content()

        val fileName = file.getName
        val start = System.nanoTime()

        val response = requests.post(
          url = s"$host/upload-file",
          data = requests.MultiPart(
            requests.MultiItem("fileName", fileName),
            requests.MultiItem("project", "proyecto_cultivos_herbaceos"),
            requests.MultiItem("location", "proyecto_cultivos_herbaceos"),
            requests.MultiItem("teams", """["team_patata"]"""),
//            requests.MultiItem("categories", ""),
            requests.MultiItem("geojson_file", file, fileName)
          ),
          readTimeout = 120000
        )

        val end = System.nanoTime()
        val durationMs = (end - start) / 1_000_000

        println(f"$fileName: ${durationMs}ms | Status: ${response.statusCode}")
        resultados += ((fileName, durationMs))

        assert(response.statusCode == 200)

        now = LocalTime.now().format(formatter)
        println(s"\n🕒 Hora: $now - Fichero procesado ${file.getName} (${durationMs}ms)")

        check_db_content()

        val total = resultados.map(_._2).sum
        val avg = if (resultados.nonEmpty) total / resultados.size else 0
        println(f"\nTiempo promedio: $avg ms")

      }



      println("\nResumen:")
      resultados.foreach { case (f, ms) => println(f"$f - $ms ms") }
    }

  }
}
