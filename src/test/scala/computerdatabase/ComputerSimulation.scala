package computerdatabase

import scala.concurrent.duration._

import io.gatling.core.Predef._
import io.gatling.http.Predef._
import io.gatling.jdbc.Predef._

class ComputerSimulation extends Simulation {

	val httpProtocol = http
		.baseUrl("https://computer-database.gatling.io")
		.inferHtmlResources(BlackList(""".*\.js""", """.*\.css""", """.*\.gif""", """.*\.jpeg""", """.*\.jpg""", """.*\.ico""", """.*\.woff""", """.*\.woff2""", """.*\.(t|o)tf""", """.*\.png""", """.*detectportal\.firefox\.com.*"""), WhiteList())
		.acceptHeader("text/html,application/xhtml+xml,application/xml;q=0.9,image/avif,image/webp,image/apng,*/*;q=0.8,application/signed-exchange;v=b3;q=0.9")
		.acceptEncodingHeader("gzip, deflate")
		.acceptLanguageHeader("en-US,en;q=0.9,de;q=0.8,de-DE;q=0.7,ru;q=0.6")
		.upgradeInsecureRequestsHeader("1")
		.userAgentHeader("Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/91.0.4472.101 Safari/537.36")

	object Search {

		val searchFeeder = csv("data/computer_search.csv").random
		val search = exec(http("Load_Homepage")
			.get("/computers"))
			.pause(1)
			.feed(searchFeeder)
			.exec(http("Search_Computer_${searchCriterion}")
				.get("/computers?f=${searchCriterion}")
				.check(css("a:contains('${searchComputerName}')", "href").saveAs("computerURL")))
			.pause(1)
			.exec(http("Select_Computer_${searchComputerName}")
				.get("${computerURL}"))
			.pause(1)
	}

	object Browse {
		val browse = {
			repeat(5, "i"){
				exec(http("Browse_Page_${i}")
					.get("/computers?p=${i}&n=10&s=name&d=asc"))
					.pause(1)
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

	setUp(admins.inject(atOnceUsers(1)),
		users.inject(atOnceUsers(1))).protocols(httpProtocol)
}