package scala.app

import java.sql.DriverManager
import scala.io.Source

object MinimalApplication extends cask.MainRoutes{
//  @cask.get("/")
//  def hello() = {
//    "Hello World!"
//  }
//
//  @cask.post("/do-thing")
//  def doThing(request: cask.Request) = {
//    request.text().reverse
//  }
//
//  initialize()


  def initDb(): Unit = {
    try {
      val url_db = "jdbc:postgresql://localhost:5432/LocalGeoInventory_Cask"
      val user = "dbuser"
      val password = "irtapass"

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

//      val sql = Source.fromResource("services/init_db.sql").mkString

      // Split por `;` para ejecutar múltiples sentencias si es necesario
      for (query <- sql.split(";").map(_.trim).filter(_.nonEmpty)) {
        println(s"[initDb] Ejecutando SQL: ${query.take(60)}…")
        statement.execute(query)
      }

      statement.close()
      connection.close()
      println("[initDb] Tablas creadas o ya existían.")

    } catch {
      case e: Throwable =>
        // No detener la aplicación: solo loguea
        System.err.println(s"[initDb] ERROR ejecutando init_db.sql: ${e.getMessage}")
        e.printStackTrace()
    }

  }

  initDb()

  override def port: Int = 8080

  val userRoutes = new routes.UserRoutes()
//  val dataRoutes = new routes.DataRoutes()

  override def allRoutes: Seq[cask.MainRoutes] = Seq(userRoutes)
}
