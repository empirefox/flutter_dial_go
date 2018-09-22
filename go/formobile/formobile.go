// formobile must be compiled with gomible. Do not use it in go.
package formobile

import (
	"math"
	"time"

	"github.com/empirefox/flutter_dial_go/go/internal/listener"
)

// for DataHandler/ClientRead and Write/ClientWrite, must sync with listener
const (
	ErrNone int8 = iota
	EOF
	ErrClosedPipe
	ErrTimeout
)

type DataHandler interface {
	OnData(b []byte, n int32, err int8)
}

type WriteReturn struct {
	N   int32
	Err int8
}

type Conn interface {
	// StartRead read buffered data to h.
	StartRead(h DataHandler)

	// Write writes data to the connection.
	// Write can be made to time out and return an Error with Timeout() == true
	// after a fixed time limit; see SetDeadline and SetWriteDeadline.
	Write(b []byte) *WriteReturn

	// Close closes the connection.
	// Any blocked Read or Write operations will be unblocked and return errors.
	Close()
}

type Dialer interface {
	Dial(port int32, channelId int64, timeoutnano int64) (Conn, error)
}

type conn struct {
	cc listener.ClientConn
}

func (c *conn) StartRead(h DataHandler) {
	go func() {
		onData := func(b []byte, n int32) { h.OnData(b, n, ErrNone) }
		err := ErrNone
		for {
			err = c.cc.ClientRead(onData)
			if err != ErrNone {
				h.OnData(nil, 0, err)
				return
			}
		}
	}()
}

func (c *conn) Write(b []byte) *WriteReturn {
	n, err := c.cc.ClientWrite(b)
	return &WriteReturn{int32(n), err}
}

func (c *conn) Close() { c.cc.Close() }

type dialer struct{}

func (d *dialer) Dial(port int32, channelId int64, timeoutnano int64) (Conn, error) {
	if port < 0 || port > math.MaxUint16 {
		return nil, listener.ErrInvalidPort
	}

	c, err := listener.Dial(uint16(port), channelId, time.Duration(timeoutnano))
	if err != nil {
		return nil, err
	}

	return &conn{c}, nil
}

var defaultDialer Dialer = new(dialer)

func GetDialer() Dialer { return defaultDialer }

