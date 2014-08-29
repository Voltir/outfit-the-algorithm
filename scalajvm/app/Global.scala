import play.api._
import play.api.mvc._
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import scala.concurrent.Future

object Global extends WithFilters(SSLFilter) {

}

object SSLFilter extends Filter {
  def apply(nextFilter: (RequestHeader) => Future[Result])(rh: RequestHeader): Future[Result] = {
    if(rh.headers.get("x-forwarded-proto").getOrElse("").contains("https")) {
      nextFilter(rh)
    } else {
      Future(Results.MovedPermanently("https://" + rh.host + rh.uri))
    }
  }
}
