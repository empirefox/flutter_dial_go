// gomobile must be created by developer. The package name must be gomobile.
// These apis must/only be here: ConcurrentRunner, FromGo, NewFromGo.
// For ios, do not forget add "${PODS_ROOT}/../Frameworks" to Framework search
// path. Use `make ios` and  `make android`.
package gomobile

import (
	"io"

	"github.com/empirefox/flutter_dial_go/go/example/internal/gomobile"
	"github.com/empirefox/flutter_dial_go/go/forgo"
)

type ConcurrentRunner interface {
	// Done will wait the runner to end.
	// Safe to be called multi times with multi threads.
	Done() error
}

type FromGo interface {
	// DoInitOnce do init things like listening. Do clean internally if err.
	// Safe to be called multi times with multi threads.
	DoInitOnce() ConcurrentRunner

	// DoDestroyOnce do clean things like freeing resources, closing listeners.
	// Safe to be called multi times with multi threads.
	DoDestroyOnce() ConcurrentRunner
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

	s1 io.Closer
	s2 io.Closer
}

func (m *fromGo) DoInitOnce() ConcurrentRunner {
	m.init.Once(func() error {
		// TODO init code here
		m.s1 = gomobile.StartServer1()
		m.s2 = gomobile.StartServer2()
		return nil
	})
	return m.init
}

func (m *fromGo) DoDestroyOnce() ConcurrentRunner {
	m.destroy.Once(func() error {
		// TODO clean code here
		err := m.s1.Close()
		if e := m.s2.Close(); e != nil {
			err = e
		}
		return err
	})
	return m.destroy
}
