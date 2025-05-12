package routes

class UserRoutes extends cask.MainRoutes {
  @cask.get("/hello")
  def hello() = "Hola desde UserRoutes"

  @cask.get("/hello2")
  def hello2() = {
    "Hoooooooooooooooooola"
  }

  initialize()
}
