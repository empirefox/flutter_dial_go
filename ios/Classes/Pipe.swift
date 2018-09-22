import Foundation
import Flutter
import Gomobile

class Pipe: NSObject {
  static let streamPrefix = "fdgs#"
  
  let messenger:FlutterBinaryMessenger
  let controller:FlutterMethodChannel
  let conn:FormobileConnProtocol
  let id:Int64
  let streamName:String
  
  var closed = false
  
  init(messenger:FlutterBinaryMessenger, controller:FlutterMethodChannel, conn:FormobileConnProtocol, id:Int64) {
    self.messenger = messenger
    self.controller = controller
    self.conn = conn
    self.id = id
    self.streamName = Pipe.streamPrefix + String(id)
  }
  
  func start() {
    messenger.setMessageHandlerOnChannel(streamName) {
      message, reply in
      let r = self.conn.write(message)!
      let err = r.err()
      if err != FormobileErrNone {
        self.closeWithErr(err)
      }
    }
    
    conn.startRead(self)
  }
  
  func stop() {
    closeWithErr(FormobileErrNone)
  }
  
  private func closeWithErr(_ err: Int8) {
    if closed {
      return
    }
    closed = true
    
    messenger.setMessageHandlerOnChannel(streamName, binaryMessageHandler: nil)
    let args:NSArray = [id, err]
    controller.invokeMethod("close", arguments: args)
    conn.close()
  }
}

extension Pipe: FormobileDataHandlerProtocol {
  func onData(_ b: Data!, n: Int32, err: Int8) {
    DispatchQueue.main.sync {
      if n > 0 {
        messenger.send(onChannel: streamName, message: b)
      }
      if err != FormobileErrNone {
        closeWithErr(err)
      }
    }
  }
}
