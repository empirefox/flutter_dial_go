// forgo only expose api for go. Do not compile directly with gomobile.
package forgo

import (
	"net"
	"sync"

	"github.com/empirefox/flutter_dial_go/go/internal/listener"
)

type ConcurrentRunner struct {
	once sync.Once
	err  error
	done chan struct{}
}

func NewConcurrentRunner() *ConcurrentRunner {
	return &ConcurrentRunner{
		done: make(chan struct{}),
	}
}

func (a *ConcurrentRunner) Once(fn func() error) {
	a.once.Do(func() {
		go func() {
			a.err = fn()
			close(a.done)
		}()
	})
}

func (a *ConcurrentRunner) Done() error {
	<-a.done
	return a.err
}

func Listen(port uint16) (net.Listener, error) {
	return listener.Listen(port)
}
