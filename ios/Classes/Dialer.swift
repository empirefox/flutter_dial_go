import Foundation
import Gomobile

class Dialer {
  typealias OnResult = (_ error: Error?) -> Void
  
  private let dialer:FormobileDialerProtocol = FormobileGetDialer()!
  let dialQueue = DispatchQueue(label: "go.dial")
  private var go:GomobileFromGoProtocol? = nil
  private var destroyRunner:GomobileConcurrentRunnerProtocol? = nil
  
  func doInit(_ onResult: @escaping OnResult) {
    if go == nil {
      go = GomobileNewFromGo()
    }
    
    let runner = go!.doInitOnce()!
    dialQueue.async {
      do {
        try runner.done()
        DispatchQueue.main.async { onResult(nil) }
      } catch {
        DispatchQueue.main.async { onResult(error) }
      }
    }
  }
  
  func doDestroy(_ onResult: @escaping OnResult) {
    guard let go = self.go else {
      onResult(nil)
      return
    }
    self.go = nil
    
    let runner = go.doDestroyOnce()!
    destroyRunner = runner
    dialQueue.async {
      do {
        try runner.done()
        DispatchQueue.main.async {
          onResult(nil)
          self.destroyRunner = nil
        }
      } catch {
        DispatchQueue.main.async {
          onResult(error)
          self.destroyRunner = nil
        }
      }
    }
  }
  
  func isNotInitialized() -> Bool {
    return go == nil
  }
  
  func dial(port: Int32, channelId: Int64, timeoutnano:Int64) throws -> FormobileConnProtocol {
    return try dialer.dial(port, channelId:channelId, timeoutnano: timeoutnano)
  }
}
