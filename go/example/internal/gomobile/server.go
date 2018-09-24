package gomobile

import (
	"fmt"
	"io"
	"log"
	"net/http"

	pb "github.com/empirefox/flutter_dial_go/go/example/protos"
	"github.com/empirefox/flutter_dial_go/go/forgo"
	"github.com/labstack/echo"
	"github.com/labstack/echo/middleware"
	"golang.org/x/net/context"
	"google.golang.org/grpc"
	"google.golang.org/grpc/reflection"
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

	go func() {
		err := e.Start("")
		if err != nil {
			log.Fatal(err)
		}
	}()

	return e
}

func hello(c echo.Context) error {
	return c.String(http.StatusOK, "Hello from golang!")
}

// server is used to implement helloworld.GreeterServer.
type server struct {
	no int
}

// SayHello implements helloworld.GreeterServer
func (s *server) SayHello(ctx context.Context, in *pb.HelloRequest) (*pb.HelloReply, error) {
	s.no++
	return &pb.HelloReply{Message: fmt.Sprintf("Hello %s: %d", in.Name, s.no)}, nil
}

type closer struct {
	*grpc.Server
}

func (c *closer) Close() error {
	c.Stop()
	return nil
}

func StartServer2() io.Closer {
	ln, err := forgo.Listen(9999)
	if err != nil {
		// TODO report error
		log.Fatal(err)
	}

	s := grpc.NewServer()
	pb.RegisterGreeterServer(s, &server{})
	// Register reflection service on gRPC server.
	reflection.Register(s)
	go func() {
		err = s.Serve(ln)
		if err != nil {
			log.Fatalf("failed to serve grpc: %v", err)
		}
	}()

	return &closer{s}
}
