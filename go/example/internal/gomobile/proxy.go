package gomobile

import (
	"errors"
	"io"
	"net"
	"sync"
)

var (
	errProxyClosed = errors.New("proxy is closed")
)

// proxy
// TODO proxy conn of Dial to Accept
type proxy struct {
	addr     string
	pool     sync.Pool
	connCh   chan net.Conn
	doneOnce sync.Once
	done     chan struct{}
}

func newProxy(addr string) *proxy {
	return &proxy{
		addr:   addr,
		connCh: make(chan net.Conn),
		done:   make(chan struct{}),
	}
}

func (p *proxy) Dial(recvSafe io.WriteCloser) (io.WriteCloser, error) {
	dst, err := net.Dial("tcp", p.addr)
	if err != nil {
		return nil, err
	}

	closed := make(chan struct{})
	go func() {
		select {
		case <-p.done:
			recvSafe.Close()
			dst.Close()
		case <-closed:
		}
	}()

	go func() {
		io.Copy(recvSafe, dst)
		recvSafe.Close()
		dst.Close()
		close(closed)
	}()

	return dst, nil
}

func (p *proxy) Put(b []byte) {
	p.pool.Put(b)
}

// Accept waits for and returns the next connection to the listener.
func (p *proxy) Accept() (net.Conn, error) {
	select {
	case c := <-p.connCh:
		return c, nil
	case <-p.done:
		return nil, errProxyClosed
	}
}

// Close closes the listener.
// Any blocked Accept operations will be unblocked and return errors.
func (p *proxy) Close() error {
	p.doneOnce.Do(func() { close(p.done) })
	return nil
}

// Addr returns the listener's network address.
func (p *proxy) Addr() net.Addr { return proxyAddr{} }

type proxyAddr struct{}

func (proxyAddr) Network() string { return "proxy" }
func (proxyAddr) String() string  { return "proxy" }
