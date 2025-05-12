package app
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
  override def port: Int = 8080

  val userRoutes = new routes.UserRoutes()
//  val dataRoutes = new routes.DataRoutes()

  override def allRoutes: Seq[cask.MainRoutes] = Seq(userRoutes)
}
