package gatlingdemostore

import scala.concurrent.duration._

import io.gatling.core.Predef._
import io.gatling.http.Predef._
import io.gatling.jdbc.Predef._

class DemostoreSimulation extends Simulation {

	val domain = "demostore.gatling.io"

	val httpProtocol = http
		.baseUrl("http://"+domain)

	val cateforyFeeder = csv("data/category_detail.csv").random
	val jsonFeederProducts = jsonFile("data/product_detail.json").random
	val csvFeederLoginDetail = csv("data/login_detail").circular

	object CMSPages{
		def homePage = {
			exec(http("Load Home Page")
				.get("/")
				.check(status.is(200))
				.check(regex("<title>Gatling Demo-Store</title>").exists)
				.check(css("#_csrf", "content").saveAs("csrfValue")))
		}

		def about = {
			exec(http("Load About Us Page")
				.get("/about-us")
				.check(status.is(200))
				.check(substring("About Us")))
		}
	}

	object Catalog {
		object Category {
			def view = {
				feed(cateforyFeeder)
				.exec(http("Load Categories Page - ${categoryName}")
					.get("/category/${categorySlug}")
					.check(status.is(200))
					.check(css("#CategoryName").is("${categoryName}")))
			}
		}
	}

	object Product {
		def view = {
			feed(jsonFeederProducts)
				.exec(http("Load Product Page - ${name}")
				.get("/product/${slug}")
					.check(status.is(200))
					.check(css("#ProductDescription").is("${description}"))
				)
		}

		def add ={
			exec(view).
			exec(http("Add Product To Cart")
				.get("/cart/add/${id}")
				.check(substring("items in your cart"))
			)
		}
	}

	object Checkout{
		def viewCart = {
			exec(
				http("Load Cart Page")
					.get("/cart/view")
					.check(status.is(200))
			)
		}
	}

	object Customer{
		def login = {
			feed(csvFeederLoginDetail)
				.exec(
					http("Load Login Page")
						.get("/login")
						.check(status.is(200))
						.check(substring("Username:"))
				)
				.exec(
					http("Customer Login Action")
						.post("/login")
						.formParam("_csrf", "${csrfValue}")
				)
		}
	}

	val scn = scenario("DemostoreSimulation")
		.exec(CMSPages.homePage)
		.pause(2)
		.exec(CMSPages.about)
		.pause(1)
		.exec(Catalog.Category.view)
		.pause(2)
		.exec(Product.add)
		.pause(1)
		.exec(Checkout.viewCart)
		.pause(4)
		.exec(http("Login User")
			.post("/login")
			.formParam("_csrf", "${csrfValue}")
			.formParam("username", "user1")
			.formParam("password", "pass"))
		.pause(3)
		.exec(http("Checkout")
			.get("/cart/checkout"))

	setUp(scn.inject(atOnceUsers(3))).protocols(httpProtocol)
}