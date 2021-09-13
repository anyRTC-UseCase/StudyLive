# StudyLive
## 项目概述

StudyLive是anyRTC开发的示例项目，演示了如何通过anyRTC云服务，配合anyRTC RTC SDK、anyRTC RTM SDK、anyRTC播放插件，实现在线自习室的场景。

## 平台兼容

- iOS 9 及以上。
- Android 4.4 及以上

## 效果展示

![1](https://github.com/anyRTC-UseCase/StudyLive/blob/master/E322FC4A-3607-4DBB-81C4-8D9D6C1653F4.png)

### 主播端调用图

![host](https://github.com/anyRTC-UseCase/VideoLive/blob/main/host.png)

主持人的使用方法：

| 方法                               | 描述                                 |
| ---------------------------------- | ------------------------------------ |
| create                             | 创建RTC                              |
| setChannelProfile:LiveBroadcasting | 设置频道场景为直播模式               |
| setClientRole:Broadcaster          | 设置直播场景下的角色为主播           |
| setAudioProfile                    | 设置音频属性，建议码率不要超过48kbps |
| joinChannel                        | 加入频道                             |
| leaveChannel                       | 离开频道                             |
| destory                            | 释放引擎                             |

### 游客端调用图

![audience](https://github.com/anyRTC-UseCase/VideoLive/blob/main/audience.png)

游客RTC使用的方法：

| 方法                               | 描述                                 |
| ---------------------------------- | ------------------------------------ |
| create                             | 创建RTC                              |
| setChannelProfile:LiveBroadcasting | 设置频道场景为直播模式               |
| setClientRole:Broadcaster          | 设置直播场景下的角色为主播           |
| setAudioProfile                    | 设置音频属性，建议码率不要超过48kbps |
| joinChannel                        | 加入频道                             |
| leaveChanel                        | 离开频道                             |
| destory                            | 释放引擎                             |

## RESTful API 说明

该演示有配合RESTful API进行开发，主要模拟用户注册、登录、获取大厅列表、创建直播间、进入直播间、等一些信息。用户参考演示逻辑，配合服务端信息可根据自身业务逻辑推断。

除了调用完整的 API 入口，服务上还可以与 RTC 服务对接，可以实时跟踪查询信息，优化大厅列表。查看具体[服务端对接文档](https://docs.anyrtc.io/cn/Live/serverapi/ncs_eventtype#实时通信)

## 联系我们

联系电话：021-65650071

QQ咨询群：580477436

咨询邮箱：[hi@dync.cc](mailto:hi@dync.cc)

技术问题：[开发者论坛](https://bbs.anyrtc.io/)

加微信入技术群交流：

[![img](https://camo.githubusercontent.com/141871dd04eb0adc5fb006cbf53e2b952b50f2340ffb56350794cd90770f4fc7/68747470733a2f2f696d672d626c6f672e6373646e696d672e636e2f32303231303332343231353934313538382e706e67)](https://camo.githubusercontent.com/141871dd04eb0adc5fb006cbf53e2b952b50f2340ffb56350794cd90770f4fc7/68747470733a2f2f696d672d626c6f672e6373646e696d672e636e2f32303231303332343231353934313538382e706e67)

获取更多帮助：[www.anyrtc.io](http://www.anyrtc.io/)
