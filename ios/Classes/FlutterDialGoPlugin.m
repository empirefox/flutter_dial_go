#import "FlutterDialGoPlugin.h"
#import <flutter_dial_go/flutter_dial_go-Swift.h>

@implementation FlutterDialGoPlugin
+ (void)registerWithRegistrar:(NSObject<FlutterPluginRegistrar>*)registrar {
  [SwiftFlutterDialGoPlugin registerWithRegistrar:registrar];
}
@end
