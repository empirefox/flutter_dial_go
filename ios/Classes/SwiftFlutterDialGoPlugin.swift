import Flutter
import UIKit
    
public class SwiftFlutterDialGoPlugin: NSObject, FlutterPlugin {
  private let registrar: FlutterPluginRegistrar
  private let controller: FlutterMethodChannel
  private let dialer = Dialer()
  private var pipes = [Int64: Pipe]()
  
  public static func register(with registrar: FlutterPluginRegistrar) {
    let channel = FlutterMethodChannel(name: "flutter_dial_go", binaryMessenger: registrar.messenger())
    let instance = SwiftFlutterDialGoPlugin(registrar: registrar, controller: channel)
    registrar.addMethodCallDelegate(instance, channel: channel)
  }
  
  init(registrar: FlutterPluginRegistrar, controller: FlutterMethodChannel) {
    self.registrar = registrar
    self.controller = controller
  }
  
  deinit {
    for pipe in pipes.values {
      pipe.stop()
    }
    pipes = [:]
  }

  public func handle(_ call: FlutterMethodCall, result: @escaping FlutterResult) {
    switch call.method {
    case "initGo":
      handleInitGo(call, result: result)
      return
    case "destroyGo":
      handleDestroyGo(call, result: result)
      return
    default:
      break
    }
    
    if dialer.isNotInitialized() {
      result(FlutterError.init(code: call.method, message: "go not initialized", details: nil))
      return
    }
    
    switch call.method {
    case "dial":
      handleDial(call, result: result)
      return
    case "close":
      handleClose(call, result: result)
      return
    default:
      break
    }
    
    result(FlutterMethodNotImplemented)
  }
  
  func handleInitGo(_ call: FlutterMethodCall, result: @escaping FlutterResult) {
    dialer.doInit {
      error in
      if let err = error {
        result(FlutterError.init(code: "initGo", message: err.localizedDescription, details: nil))
      } else {
        result(nil)
      }
    }
  }
  
  func handleDestroyGo(_ call: FlutterMethodCall, result: @escaping FlutterResult) {
    dialer.doDestroy {
      error in
      if let err = error {
        result(FlutterError(code: "destroyGo", message: err.localizedDescription, details: nil))
      } else {
        result(nil)
      }
    }
  }
  
  func handleDial(_ call: FlutterMethodCall, result: @escaping FlutterResult) {
    dialer.dialQueue.async {
      let args:NSArray = call.arguments as! NSArray
      let port:Int32 = args[0] as! Int32
      let id:Int64 = args[1] as! Int64
      let timeoutnano:Int64 = args[2] as! Int64
      
      do {
        let conn = try self.dialer.dial(port: port, channelId: id, timeoutnano: timeoutnano)
        let pipe = Pipe(messenger: self.registrar.messenger(), controller: self.controller, conn: conn, id: id)
        DispatchQueue.main.async {
          pipe.start()
          self.pipes[id] = pipe
          result(nil)
        }
      } catch {
        DispatchQueue.main.async {
          result(FlutterError(code: "dial", message: error.localizedDescription, details: nil))
        }
      }
    }
  }
  
  func handleClose(_ call: FlutterMethodCall, result: @escaping FlutterResult) {
    let id:Int64 = call.arguments as! Int64
    if let pipe = pipes[id] {
      pipe.stop()
      pipes[id] = nil
    }
    result(nil)
  }
}
