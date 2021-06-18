package gatlingdemostore

import scala.concurrent.duration._
import io.gatling.core.Predef._
import io.gatling.http.Predef._
import io.gatling.jdbc.Predef._

import scala.util.Random

class DemostoreSimulation extends Simulation {

	val domain = "demostore.gatling.io"

	val httpProtocol = http
		.baseUrl("http://"+domain)

	val cateforyFeeder = csv("data/category_detail.csv").random
	val jsonFeederProducts = jsonFile("data/product_detail.json").random
	val csvFeederLoginDetail = csv("data/login_detail.csv").circular

	val rnd = new Random()

	def randomString(length: Int): String = {
		rnd.alphanumeric.filter(_.isLetter ).take(length).mkString
	}

	val initSession = exec(flushCookieJar)
		.exec(session => session.set("randomNumber", rnd.nextInt()))
		.exec(session => session.set("customerLoggedIn", false))
		.exec(session => session.set("cartTotal", 0.0))
		.exec(addCookie(Cookie("sessionID", randomString(10)).withDomain(domain)))

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
				.exec(session => {
					val currentCartTotal = session("cartTotal").as[Double]
					val itemPrice = session("price").as[Double]
					session.set("cartTotal", (currentCartTotal+itemPrice))
				})
		}
	}

	object Checkout{
		def viewCart = {
			doIf(session => !session("customerLoggedIn").as[Boolean]){
				exec(Customer.login)
			}
			.exec(
				http("Load Cart Page")
					.get("/cart/view")
					.check(status.is(200))
					.check(substring("Cart Overview"))
					.check(css("#grandTotal").is("$$${cartTotal}"))

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
						.formParam("username", "${username}")
						.formParam("password", "${password}")
						.check(status.is(200))
				)
				.exec(session => session.set("customerLoggedIn", true))
		}

		def completeCheckout = {
			exec(
				{
					http("Checkout Cart")
						.get("/cart/checkout")
						.check(status.is(200))
						.check(substring("Thanks for your order!"))
				}
			)
		}
	}

	val scn = scenario("DemostoreSimulation")
		.exec(initSession)
		.exec(CMSPages.homePage)
		.pause(2)
		.exec(CMSPages.about)
		.pause(1)
		.exec(Catalog.Category.view)
		.pause(2)
		.exec(Product.add)
		.pause(1)
		.exec(Checkout.viewCart)
		.pause(2)
		.exec(Customer.completeCheckout)

	setUp(scn.inject(atOnceUsers(1))).protocols(httpProtocol)
}