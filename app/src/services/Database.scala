package services

import doobie._
import doobie.implicits._
import cats.effect._
import doobie.hikari.HikariTransactor
import com.zaxxer.hikari.HikariDataSource
import org.postgresql.ds.PGSimpleDataSource
import cats.effect.unsafe.implicits.global

import scala.concurrent.ExecutionContext

object Database {

  // Creamos un ExecutionContext para las operaciones en el hilo de fondo
//  implicit val cs: ContextShift[IO] = IO.contextShift(ExecutionContext.global)

  // Método para crear el HikariTransactor
  def createTransactor: HikariTransactor[IO] = {
    val dataSource = new HikariDataSource()
    dataSource.setJdbcUrl("jdbc:postgresql://localhost:5432/your_database")
    dataSource.setUsername("your_user")
    dataSource.setPassword("your_password")
    dataSource.setMaximumPoolSize(10) // Número máximo de conexiones en el pool


    // HikariTransactor para la conexión asincrónica
    HikariTransactor[IO](dataSource, ExecutionContext.global)
  }

  // Método para ejecutar una consulta simple
  def queryExample: IO[Int] = {
    // Creamos un transactor para la conexión a la base de datos
    val xa = createTransactor

    // Ejemplo de consulta SQL con Doobie
    val query = sql"SELECT COUNT(*) FROM your_table".query[Int].unique

    // Ejecutamos la consulta
    query.transact(xa)
  }

  // Método para hacer una inserción masiva con raw SQL
  def bulkInsert(data: List[String]): IO[Int] = {
    val xa = createTransactor

    // Crear una consulta de inserción masiva
    val insertQuery = {
      val values = data.map(d => s"('$d')").mkString(", ")
      sql"INSERT INTO your_table (column_name) VALUES $values".update.run
    }

    // Ejecutar la consulta
    insertQuery.transact(xa)
  }
}
