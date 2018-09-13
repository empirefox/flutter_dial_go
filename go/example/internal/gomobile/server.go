package gomobile

import (
	"io"
	"log"
	"net"
	"net/http"
	"time"

	"github.com/empirefox/flutter_dial_go/go/forgo"
	"github.com/labstack/echo"
	"github.com/labstack/echo/middleware"
	"golang.org/x/net/http2"
)

func StartServer1() io.Closer {
	ln, err := forgo.Listen(9998)
	if err != nil {
		// TODO report error
		log.Fatal(err)
	}

	// Echo instance
	e := echo.New()

	e.Listener = ln
	e.Debug = true

	// Middleware
	e.Use(middleware.Recover())

	// Routes
	e.GET("/", hello)
	e.POST("/echo", echoback)

	go func() {
		err := e.Start("")
		if err != nil {
			log.Fatal(err)
		}
	}()

	return e
}

func StartServer2() io.Closer {
	ln, err := forgo.Listen(9999)
	if err != nil {
		// TODO report error
		log.Fatal(err)
	}

	// Echo instance
	e := echo.New()

	e.Listener = ln
	e.Debug = true

	// Middleware
	e.Use(middleware.Recover())

	// Routes
	e.GET("/", hello)
	e.POST("/echo", echoback)

	s1 := &http.Server{Handler: e}
	s2 := &http2.Server{}
	http2.ConfigureServer(s1, s2)

	// Start server
	go func() {
		err := SimpleListenAndServe(ln, func(c net.Conn) {
			s2.ServeConn(c, &http2.ServeConnOpts{BaseConfig: s1})
		})
		if err != nil {
			log.Fatal(err)
		}
	}()

	return e
}

func hello(c echo.Context) error {
	return c.String(http.StatusOK, "Hello from golang!")
}

func echoback(c echo.Context) error {
	c.Response().WriteHeader(http.StatusOK)
	defer c.Request().Body.Close()
	_, err := io.Copy(c.Response(), c.Request().Body)
	return err
}

func SimpleListenAndServe(listener net.Listener, serveConn func(c net.Conn)) error {
	var tempDelay time.Duration // how long to sleep on accept failure
	for {
		c, e := listener.Accept()
		if e != nil {
			if ne, ok := e.(net.Error); ok && ne.Temporary() {
				if tempDelay == 0 {
					tempDelay = 5 * time.Millisecond
				} else {
					tempDelay *= 2
				}
				if max := 1 * time.Second; tempDelay > max {
					tempDelay = max
				}
				time.Sleep(tempDelay)
				continue
			}
			return e
		}
		tempDelay = 0
		go serveConn(c)
	}
}
