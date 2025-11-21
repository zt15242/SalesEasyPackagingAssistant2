如果子项包含指定关键词的话需要创建scriptTrigger.xml 文件

~~~xml
<?xml version="1.0" encoding="utf-8"?>

<configs> 
  <config> 
	<!-- 根据子项文件名称来判断添加什么内容 -->
  </config> 
</configs>
~~~



文件名称包含 Event (不含 EventImpl） 需要让用户选择操作

- submit（提交审批）
- agree（通过）
- reject（拒绝）
- userTurn（转办）
- plussign（加签）
- withdraw（撤回）
- arrive（到达后-通过）
- rejectArrive（到达后-拒绝）
- complete（完成审批流程且最终结果为通过，仅用于流程级审批通过后事件中）
- return（完成审批流程且最终结果为拒绝，仅用于流程级审批拒绝后事件中）

这里每个文件可以进行多选。

页面显示：

文件名称

请输入对象名称

审批流配置

操作选项：提交审批  通过 ...

执行时机：validate   before   after

工作流配置

操作选项：submit  agree ...

执行时机：validate   	before  after

可视化流配置

....

config 补充内容

~~~xml
审批流配置 
<approvalevent> 
     <object>object</object>  
     <operate>operate</operate>  
     <position>position</position>  
     <class>class</class> 
</approvalevent> 
工作流配置
 <approvalevent> 
     <object>object</object>  
     <operate>operate</operate>  
     <position>position</position>  
     <class>class</class> 
</approvalevent>
可视化流配置
 <stageProcessEvent> 
     <object>object</object>  
     <operate>operate</operate>  
     <position>position</position>  
     <class>class</class> 
</stageProcessEvent> 
~~~

审批流配置

|   参数   |                             说明                             |
| :------: | :----------------------------------------------------------: |
|  object  |     业务逻辑代码操作对象的名称，需要与流程关联对象相同。     |
| operate  | 业务逻辑代码在进行哪类操作时执行，目前支持的操作包括：submit（提交审批）agree（通过）reject（拒绝）userTurn（转办）plussign（加签）withdraw（撤回）arrive（到达后-通过）rejectArrive（到达后-拒绝）complete（完成审批流程且最终结果为通过，仅用于流程级审批通过后事件中）return（完成审批流程且最终结果为拒绝，仅用于流程级审批拒绝后事件中）动态选人场景中不需要写此项。 |
| position | 业务逻辑代码执行的时机，目前支持的时机包括：validate（提交前校验、通过前校验、拒绝前校验）before（提交前、通过前、拒绝前、审批撤回前）after（提交后、通过后、拒绝后、转办后、加签后、审批通过后、审批拒绝后、审批撤回后）动态选人场景中不需要写此项。 |
|  class   |          业务逻辑代码的完整名称，格式为：包名.类名           |

工作流配置

|   参数   |                             说明                             |
| :------: | :----------------------------------------------------------: |
|  object  |     业务逻辑代码操作对象的名称，需要与流程关联对象相同。     |
| operate  | 业务逻辑代码在进行哪类操作时执行，目前支持的操作包括：submit（推进前校验、推进前、推进后、提交前校验、提交前、提交后、提交前校验（拒绝后是否重审））agree（流程通过后）reject（退回前校验、退回前）withdraw（退回后、流程退回后、流程撤回前、流程撤回后）arrive（到达后）自动任务节点事件和动态选人场景中不需要写此项。 |
| position | 业务逻辑代码执行的时机，目前支持的时机包括：validate（推进前校验、退回前校验、提交前校验、提交前校验（拒绝后是否重审））before（推进前、退回前、提交前、流程撤回前）after（推进后、退回后、到达后、提交后、流程通过后、流程撤回后）自动任务节点事件和动态选人场景中不需要写此项。 |
|  class   |          业务逻辑代码的完整名称，格式为：包名.类名           |

可视化流配置

|   参数   |                             说明                             |
| :------: | :----------------------------------------------------------: |
|  object  |     业务逻辑代码操作对象的名称，需要与流程关联对象相同。     |
| operate  | 业务逻辑代码在进行哪类操作时执行，目前支持的操作包括：arrive（阶段到达）advance（阶段推进）reactivate（阶段重新激活） |
| position | 业务逻辑代码执行的时机，目前支持的时机包括：validate（阶段到达前校验）after（阶段到达后、阶段推进后、阶段重新激活后） |
|  class   |          业务逻辑代码的完整名称，格式为：包名.类名           |

文件名包含trigger或Trigger时（注：也是需要选项的）

~~~xml
<trigger> 
    <object>object</object>  
    <operate>operate</operate>  
    <position>position</position>  
    <order>order</order>  
    <class>class</class> 
</trigger> 
~~~

|   参数   |                             说明                             |
| :------: | :----------------------------------------------------------: |
|  object  | 业务逻辑代码操作对象的 API Key，支持的对象及操作请参考[注意事项](https://doc.xiaoshouyi.com/developmentPlatform_businessLogicDevelopment_trigger_attentionPoints.html) 中的**Trigger 支持对象说明**部分，也可通过调用获取业务对象列表接口获取 |
| operate  | 业务逻辑代码在进行哪类操作时执行，目前支持的操作包括：add（创建）delete（删除）update（更新）transfer（转移）lock（锁定）unlock（解锁）recover（从数据回收站恢复数据） |
| position | 业务逻辑代码执行的时机，目前支持 before 和 after，before 为在操作前执行，after 为在操作后执行 |
|  order   | 业务逻辑代码的执行顺序。例如，在客户对象（account）创建数据（add）操作前有多个业务逻辑代码需要执行，则多个业务逻辑代码按照此处指定的顺序按升序依次执行，即序号小的优先执行 |
|  class   |          业务逻辑代码的完整名称，格式为：包名.类名           |

页面显示：

文件名称

触发器选项

请输入对象名称

操作选项：创建  删除 ...

执行时机： before  after  （可以全选，分别生成多条配置）





文件名有EventImpl 时，在文件下增加一个输入框 object 用户来输入对象名称：

~~~xml
自动流配置
<autoflowevent> 
     <object>object</object>  
     <class>class</class> 
</autoflowevent> 
触发规则配置
<ruleevent> 
    <object>object</object>  
    <class>class</class> 
</ruleevent> 
定时调度配置
<ruleevent> 
    <object>object</object>  
    <class>class</class> 
</ruleevent> 
~~~

页面显示：

文件名称

请输入对象名称

操作： 自动流配置   触发规则配置   定时调度配置



文件名有 ScheduleJob时

补充config

~~~xml
<schedule>
      <class>class</class>
</schedule>
~~~

