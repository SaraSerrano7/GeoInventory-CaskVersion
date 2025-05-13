package app
import io.undertow.Undertow
import utest._

import java.sql.DriverManager
import java.nio.file.Files
import java.nio.file.Paths
import scala.app.MinimalApplication
import scala.app.MinimalApplication.getClass
import scala.io.Source

object ExampleTests extends TestSuite{
  def withServer[T](example: cask.main.Main)(f: String => T): T = {
    val server = Undertow.builder
      .addHttpListener(8081, "localhost")
      .setHandler(example.defaultHandler)
      .build
    server.start()
    val res =
      try f("http://localhost:8081")
      finally server.stop()
    res
  }


  def initDb(): Unit = {
    try {
      val url_db = "jdbc:postgresql://localhost:5432/LocalGeoInventory_Cask"
      val user = "xxxxxx"
      val password = "xxxxxxx"

      val loader = getClass.getClassLoader
      val url = loader.getResource("sql/init_db.sql")

      println(s"URL de sql/init_db.sql: $url")

      if (url == null) {
        println("El archivo no está en el classpath")
      } else {
        val source = scala.io.Source.fromURL(url)
        val content = source.getLines().mkString("\n")
        println("Contenido del archivo:\n" + content)
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

  val tests = Tests {
//    test("MinimalApplication") - withServer(MinimalApplication) { host =>
//      val success = requests.get(host)
//
//      success.text() ==> "Hello World!"
//      success.statusCode ==> 200
//
//      requests.get(s"$host/doesnt-exist", check = false).statusCode ==> 404
//
//      requests.post(s"$host/do-thing", data = "hello").text() ==> "olleh"
//
//      requests.delete(s"$host/do-thing", check = false).statusCode ==> 405
//    }

    test("UploadGeoJSONFilesPerformance") - withServer(MinimalApplication) { host =>
//      val dir = new java.io.File("test/resources/files")
//      val geojsons = Option(dir.listFiles).getOrElse(Array.empty).filter(_.getName.endsWith(".geojson"))

//      val path = Paths.get("src/scala/resources/files")
//      val geojsons = Option(path.toFile.listFiles).getOrElse(Array.empty).filter(_.getName.endsWith(".geojson"))

      val resourceDir = getClass.getClassLoader.getResource("files")
      val dir = new java.io.File(resourceDir.toURI)
      val geojsons = Option(dir.listFiles).getOrElse(Array.empty).filter(_.getName.endsWith(".geojson"))



      val resultados = scala.collection.mutable.ListBuffer.empty[(String, Long)]

      for (file <- geojsons) {
        initDb()

        val fileName = file.getName
        val start = System.nanoTime()

        val response = requests.post(
          url = s"$host/upload-file",
          data = requests.MultiPart(
            requests.MultiItem("fileName", fileName),
            requests.MultiItem("project", "proyecto_cultivos_herbaceos"),
            requests.MultiItem("location", "proyecto_cultivos_herbaceos"),
            requests.MultiItem("teams", """["team_patata"]"""),
            requests.MultiItem("categories", "[]"),
            requests.MultiItem("geojson_file", file, fileName)
          )
        )

        val end = System.nanoTime()
        val durationMs = (end - start) / 1_000_000

        println(f"$fileName: ${durationMs}ms | Status: ${response.statusCode}")
        resultados += ((fileName, durationMs))

        assert(response.statusCode == 200)
        val total = resultados.map(_._2).sum
        val avg = if (resultados.nonEmpty) total / resultados.size else 0
        println(f"\nTiempo promedio: $avg ms")

      }



      println("\nResumen:")
      resultados.foreach { case (f, ms) => println(f"$f - $ms ms") }
    }

  }
}
