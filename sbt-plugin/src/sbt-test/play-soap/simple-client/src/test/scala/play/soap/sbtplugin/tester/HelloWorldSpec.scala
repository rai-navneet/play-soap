package play.soap.sbtplugin.tester

import java.net.ServerSocket
import javax.xml.ws.Endpoint

import org.apache.cxf.endpoint.Server
import org.apache.cxf.interceptor.{LoggingInInterceptor, LoggingOutInterceptor}
import org.apache.cxf.jaxws.EndpointImpl
import org.apache.cxf.transport.http_jetty.JettyHTTPServerEngine
import org.specs2.mutable.Specification
import org.specs2.time.NoTimeConversions
import play.soap.PlayJaxWsProxyFactoryBean
import play.soap.testservice.client._
import scala.collection.JavaConversions._
import scala.concurrent.{Await, Future}
import scala.concurrent.duration._

object HelloWorldSpec extends Specification with NoTimeConversions {

  sequential

  "HelloWorld" should {
    "say hello" in withClient { client =>
      await(client.sayHello("world")) must_== "Hello world"
    }

    "say hello to many people" in withClient { client =>
      await(client.sayHelloToMany(List("foo", "bar"))).toList must_== List("Hello foo", "Hello bar")
    }

    "say hello to one user" in withClient { client =>
      val user = new User
      user.setName("world")
      await(client.sayHelloToUser(user)).getUser.getName must_== "world"
    }
  }

  def await[T](future: Future[T]): T = Await.result(future, 10.seconds)

  def withClient[T](block: HelloWorld => T): T = withService { port =>
    val factory = new PlayJaxWsProxyFactoryBean
    factory.setServiceClass(classOf[HelloWorld])
    factory.setAddress(s"http://localhost:$port/helloWorld")
    val client = factory.create().asInstanceOf[HelloWorld]
    block(client)
  }

  def withService[T](block: Int => T): T = {
    val port = findAvailablePort()
    val endpoint = Endpoint.publish(s"http://localhost:$port/helloWorld", new play.soap.testservice.HelloWorldImpl)
    try {
      block(port)
    } finally {
      endpoint.stop()
      // Need to shutdown whole engine.  Note, Jetty's shutdown doesn't seem to happen synchronously, have to wait
      // a few seconds for the port to be released. This is why we use a different port each time.
      endpoint.asInstanceOf[EndpointImpl].getBus.shutdown(true)
    }
  }

  def findAvailablePort() = {
    val socket = new ServerSocket(0)
    try {
      socket.getLocalPort
    } finally {
      socket.close()
    }
  }

}