'use strict'

const { createApp } = require('./lib/app')
const { createStore } = require('./lib/store')
const { createAiService } = require('./lib/ai')
const { createCozeClient } = require('./lib/coze')
const { createExam } = require('./lib/exam')
const { createExamStore } = require('./lib/exam-store')
const { normalizePath, rawPath } = require('./lib/http')
const examSeed = require('./exam-seed.json')

const cozeToken = process.env.COZE_API_TOKEN
const cozeBotId = process.env.COZE_BOT_ID
const ai = cozeToken && cozeBotId
  ? createAiService({
      coze: createCozeClient({
        token: cozeToken,
        botId: cozeBotId,
        baseUrl: process.env.COZE_API_BASE || 'https://api.coze.cn',
      }),
    })
  : null

const urlPrefix = process.env.ORBIT_URL_PREFIX || '/orbit'

// 智解星考试后端与 Orbit 共用同一个 URL 化网关，通过 /exam 前缀分流。
const exam = createExam({ store: createExamStore(uniCloud, examSeed) })

// UNI_CLOUD_PROVIDER is only informational; the SDK picks the bound space.
const app = createApp({
  store: createStore(uniCloud),
  ai,

  async uploadAvatar({ key, extension, contentType, buffer }) {
    const result = await uniCloud.uploadFile({
      cloudPath: 'orbit/avatars/' + key + '.' + extension,
      fileContent: buffer,
      contentType,
    })
    if (!result || !result.fileID) {
      throw new Error('uniCloud.uploadFile returned no fileID')
    }
    return result.fileID
  },

  async deleteAvatar(fileID) {
    await uniCloud.deleteFile({ fileList: [fileID] })
  },

  async resolveAvatarUrl(fileID) {
    const result = await uniCloud.getTempFileURL({ fileList: [fileID] })
    const item = result && result.fileList && result.fileList[0]
    if (!item) return null
    if (item.status !== undefined && item.status !== 0) return null
    return item.tempFileURL || null
  },

  urlPrefix,
})

exports.main = async (event, context) => {
  const path = normalizePath(rawPath(event), urlPrefix)
  if (path === '/exam' || path.startsWith('/exam/')) {
    return exam(event, path.slice('/exam'.length) || '/')
  }
  return app(event, context)
}
