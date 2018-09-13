// gomobile must be created by developer. The package name must be gomobile.
// These apis must/only be here: ConcurrentRunner, FromGo, NewFromGo.
package gomobile

import (
	"github.com/empirefox/flutter_dial_go/go/example/internal/gomobile"
	"github.com/empirefox/flutter_dial_go/go/forgo"
)

type ConcurrentRunner interface {
	// Done will wait the runner to end.
	// Safe to be called multi times with multi threads.
	Done() error
}

type FromGo interface {
	// InitOnce do init things like listening. Do clean internally if err.
	// Safe to be called multi times with multi threads.
	InitOnce() ConcurrentRunner

	// DestroyOnce do clean things like freeing resources, closing listeners.
	// Safe to be called multi times with multi threads.
	DestroyOnce() ConcurrentRunner
}

func NewFromGo() FromGo {
	return &fromGo{
		init:    forgo.NewConcurrentRunner(),
		destroy: forgo.NewConcurrentRunner(),
	}
}

type fromGo struct {
	init    *forgo.ConcurrentRunner
	destroy *forgo.ConcurrentRunner
}

func (m *fromGo) InitOnce() ConcurrentRunner {
	m.init.Once(func() error {
		// TODO init code here
		gomobile.StartServer1()
		gomobile.StartServer2()
		return nil
	})
	return m.init
}

func (m *fromGo) DestroyOnce() ConcurrentRunner {
	m.destroy.Once(func() error {
		// TODO clean code here
		return nil
	})
	return m.destroy
}
