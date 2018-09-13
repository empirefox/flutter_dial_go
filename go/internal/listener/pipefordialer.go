package listener

import (
	"net"
)

// must sync with formobile
const (
	ErrNone int8 = iota
	EOF
	ErrClosedPipe
	ErrTimeout
)

type ClientConn interface {
	net.Conn
	ClientRead(onData func(b []byte, n int32)) (err int8)
	ClientWrite(b []byte) (n int, err int8)
}

func (p *pipe) ClientRead(onData func(b []byte, n int32)) (err int8) {
	switch {
	case isClosedChan(p.localDone):
		return ErrClosedPipe
	case isClosedChan(p.remoteDone):
		return EOF
	case isClosedChan(p.readDeadline.wait()):
		return ErrTimeout
	}

	select {
	case bw := <-p.rdRx:
		n := len(bw)
		onData(bw, int32(n))
		p.rdTx <- n
		return ErrNone
	case <-p.localDone:
		return ErrClosedPipe
	case <-p.remoteDone:
		return EOF
	case <-p.readDeadline.wait():
		return ErrTimeout
	}
}

func (p *pipe) ClientWrite(b []byte) (n int, err int8) {
	switch {
	case isClosedChan(p.localDone):
		return 0, ErrClosedPipe
	case isClosedChan(p.remoteDone):
		return 0, ErrClosedPipe
	case isClosedChan(p.writeDeadline.wait()):
		return 0, ErrTimeout
	}

	p.wrMu.Lock() // Ensure entirety of b is written together
	defer p.wrMu.Unlock()
	for once := true; once || len(b) > 0; once = false {
		select {
		case p.wrTx <- b:
			nw := <-p.wrRx
			b = b[nw:]
			n += nw
		case <-p.localDone:
			return n, ErrClosedPipe
		case <-p.remoteDone:
			return n, ErrClosedPipe
		case <-p.writeDeadline.wait():
			return n, ErrTimeout
		}
	}
	return n, ErrNone
}
