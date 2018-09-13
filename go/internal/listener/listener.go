package listener

import (
	"context"
	"errors"
	"net"
	"strconv"
	"sync"
	"time"
)

var (
	ErrInvalidPort      = errors.New("invalid port")
	errPortUsed         = errors.New("port is already used")
	errDialTimeout      = errors.New("dial timeout")
	errListenerNotFound = errors.New("listener not found")
	errListenerClosed   = errors.New("listener is closed")
)

var listeners sync.Map

type Listener struct {
	port      uint16
	addr      addr
	connCh    chan net.Conn
	doneCh    chan struct{}
	closeOnce sync.Once
}

// Accept waits for and returns the next connection to the listener.
func (ln *Listener) Accept() (net.Conn, error) {
	select {
	case c := <-ln.connCh:
		return c, nil
	case <-ln.doneCh:
		return nil, errListenerClosed
	}
}

// Close closes the listener.
// Any blocked Accept operations will be unblocked and return errors.
func (ln *Listener) Close() error {
	ln.closeOnce.Do(func() {
		listeners.Delete(ln.port)
		close(ln.doneCh)
	})
	return nil
}

// Addr returns the listener's network address.
func (ln *Listener) Addr() net.Addr { return &ln.addr }

func Listen(port uint16) (net.Listener, error) {
	if port == 0 {
		return nil, ErrInvalidPort
	}

	_, ok := listeners.Load(port)
	if ok {
		return nil, errPortUsed
	}

	ln := &Listener{
		port:   port,
		addr:   addr("go:" + strconv.FormatUint(uint64(port), 10)),
		connCh: make(chan net.Conn),
		doneCh: make(chan struct{}),
	}
	listeners.Store(port, ln)
	return ln, nil
}

func Dial(port uint16, channelId int64, timeout time.Duration) (ClientConn, error) {
	if port == 0 {
		return nil, ErrInvalidPort
	}

	v, ok := listeners.Load(port)
	if !ok {
		return nil, errListenerNotFound
	}

	ctx := context.Background()
	var cancel context.CancelFunc
	if timeout > 0 {
		ctx, cancel = context.WithTimeout(ctx, timeout)
		defer cancel()
	}

	ln := v.(*Listener)
	dialerAddr := addr("flutter:" + strconv.FormatInt(channelId, 10))
	client, server := Pipe(&dialerAddr, ln.Addr())
	select {
	case ln.connCh <- server:
		return client, nil
	case <-ln.doneCh:
		return nil, errListenerClosed
	case <-ctx.Done():
		return nil, errDialTimeout
	}
}
