#import "AudioTrack.h"

@implementation AudioTrack

- (instancetype)initWithGroupId:(int)groupId
                        trackId:(int)trackId
                       language:(NSString *)language
                          label:(NSString *)label
                        isCurrent:(BOOL)isCurrent {
  self = [super init];
  if (self) {
    _groupId = groupId;
    _trackId = trackId;
    _language = [language copy];
    _label = [label copy];
    _isCurrent = isCurrent;
  }
  return self;
}

- (instancetype)initWithGroupId:(int)groupId trackId:(int)trackId {
  return [self initWithGroupId:groupId trackId:trackId language:nil label:nil isCurrent:NO];
}

- (NSString *)description {
  return [NSString stringWithFormat:@"<AudioTrack: groupId=%d, trackId=%d, language=%@, label=%@, isCurrent=%@>",
                                    self.groupId, self.trackId, self.language, self.label, self.isCurrent ? @"YES" : @"NO"];
}

- (NSDictionary<NSString *, id> *)asMap {
  return @{
    @"groupId" : @(self.groupId),
    @"trackId" : @(self.trackId),
    @"language" : self.language ?: [NSNull null],
    @"label" : self.label ?: [NSNull null],
    @"isCurrent" : [NSNumber numberWithBool:self.isCurrent]
  };
}

#pragma mark - NSSecureCoding (The equivalent of Java's Serializable)

+ (BOOL)supportsSecureCoding {
  return YES;
}

- (void)encodeWithCoder:(NSCoder *)coder {
  [coder encodeInt:self.groupId forKey:@"groupId"];
  [coder encodeInt:self.trackId forKey:@"trackId"];
  [coder encodeObject:self.language forKey:@"language"];
  [coder encodeObject:self.label forKey:@"label"];
  [coder encodeBool:self.isCurrent forKey:@"isCurrent"];
}

- (instancetype)initWithCoder:(NSCoder *)coder {
  int groupId = [coder decodeIntForKey:@"groupId"];
  int trackId = [coder decodeIntForKey:@"trackId"];
  NSString *language = [coder decodeObjectOfClass:[NSString class] forKey:@"language"];
  NSString *label = [coder decodeObjectOfClass:[NSString class] forKey:@"label"];
  BOOL isCurrent = [coder decodeBoolForKey:@"isCurrent"];

  return [self initWithGroupId:groupId trackId:trackId language:language label:label isCurrent:isCurrent];
}

@end