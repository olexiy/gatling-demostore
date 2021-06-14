package computerdatabase

import scala.concurrent.duration._

import io.gatling.core.Predef._
import io.gatling.http.Predef._
import io.gatling.jdbc.Predef._

class ComputerSimulation extends Simulation {

	val httpProtocol = http
		.baseUrl("https://computer-database.gatling.io")

	object Search {

		val searchFeeder = csv("data/computer_search.csv").circular
		val search = exec(http("Load_Homepage")
			.get("/computers"))
			.pause(3)
			.feed(searchFeeder)
			.exec(http("Search_Computer_${searchCriterion}")
				.get("/computers?f=${searchCriterion}")
				.check(css("a:contains('${searchComputerName}')", "href").saveAs("computerURL")))
			.pause(3)
			.exec(http("Select_Computer_${searchComputerName}")
				.get("${computerURL}"))
			.pause(3)
	}

	object Browse {
		val browse = {
			repeat(5, "i"){
				exec(http("Browse_Page_${i}")
					.get("/computers?p=${i}&n=10&s=name&d=asc"))
					.pause(4)
			}
		}
	}

	object Create{
		val create = exec(http("LoadCreate_Computer_Page")
			.get("/computers/new"))
			.pause(1)
			.exec(http("Create_Computer")
				.post("/computers")
				.formParam("name", "Macbook Air")
				.formParam("introduced", "2020-01-01")
				.formParam("discontinued", "")
				.formParam("company", "3")
			.check(status.is(200)))
	}

	val admins = scenario("Admins").exec(Search.search, Browse.browse, Create.create)

	val users = scenario("Users").exec(Search.search, Browse.browse)

	setUp(admins.inject(atOnceUsers(5)),

		users.inject(
			nothingFor(5),
			atOnceUsers(1),
			rampUsers(3) during (10),
			constantUsersPerSec(2) during (20)
		))
		.protocols(httpProtocol)
}