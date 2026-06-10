export const meta = {
  name: 'internationalize-all-files',
  description: '将所有硬编码中文字符串移到 strings.xml',
  phases: [
    { title: '分析', detail: '找出所有硬编码字符串' },
    { title: '更新', detail: '更新 strings.xml 和代码文件' }
  ]
}

const files = [
  'desktop/src/main/kotlin/com/zhang/adbhub/desktop/ui/SettingsDialog.kt',
  'desktop/src/main/kotlin/com/zhang/adbhub/desktop/ui/LogPanel.kt',
  'desktop/src/main/kotlin/com/zhang/adbhub/desktop/ui/OperationPanel.kt',
  'desktop/src/main/kotlin/com/zhang/adbhub/desktop/ui/MainScreen.kt',
  'desktop/src/main/kotlin/com/zhang/adbhub/desktop/ui/DeviceListPanel.kt',
  'desktop/src/main/kotlin/com/zhang/adbhub/desktop/ui/FileManagerPanel.kt',
  'desktop/src/main/kotlin/com/zhang/adbhub/desktop/viewmodel/MainViewModel.kt'
]

phase('分析')

const results = await pipeline(
  files,
  file => agent(`分析 ${file}，找出所有硬编码的中文字符串（包括按钮文本、提示信息、日志消息、错误消息等），返回字符串列表和建议的 key 名`, {
    label: `分析 ${file.split('/').pop()}`,
    phase: '分析',
    schema: {
      type: 'object',
      properties: {
        strings: {
          type: 'array',
          items: {
            type: 'object',
            properties: {
              text: { type: 'string' },
              key: { type: 'string' },
              hasParams: { type: 'boolean' }
            }
          }
        }
      }
    }
  }),
  (analysis, file) => agent(`更新 ${file}：
1. 添加 import com.zhang.adbhub.desktop.utils.StringResources
2. 将所有硬编码中文替换为 StringResources.get() 调用
3. 如果是 SettingsDialog.kt，同时修改弹窗高度为 .heightIn(min = 600.dp, max = 800.dp)

字符串映射：${JSON.stringify(analysis.strings)}`, {
    label: `更新 ${file.split('/').pop()}`,
    phase: '更新'
  })
)

phase('完成')
log(`已完成 ${files.length} 个文件的国际化`)

return { filesProcessed: files.length }
