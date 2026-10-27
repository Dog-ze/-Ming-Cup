/**
 * 从 china-cities-dataset 的中文 XML 生成前端用的“省份 → 城市”数据。
 *
 * 数据来源：https://github.com/pluwen/china-cities-dataset
 * 许可证：GPL-3.0
 *
 * 使用方法：node scripts/generate-china-regions.mjs
 */
import { mkdir, writeFile } from 'node:fs/promises'
import { dirname, resolve } from 'node:path'
import { fileURLToPath } from 'node:url'

const SOURCE_URL =
  'https://raw.githubusercontent.com/pluwen/china-cities-dataset/refs/heads/main/data/zh-cn.xml'

const directControlledRegions = new Set(['北京', '天津', '上海', '重庆', '香港', '澳门'])

function displayProvince(name) {
  if (directControlledRegions.has(name)) return name + '市'
  if (name === '广西') return '广西壮族自治区'
  if (name === '内蒙古') return '内蒙古自治区'
  if (name === '宁夏') return '宁夏回族自治区'
  if (name === '新疆') return '新疆维吾尔自治区'
  if (name === '西藏') return '西藏自治区'
  return name.endsWith('省') || name.endsWith('自治区') || name === '台湾' ? name : name + '省'
}

function displayCity(name) {
  if (name.endsWith('市') || name.endsWith('区') || name.endsWith('县')) return name
  return name + '市'
}

function parseRegions(xml) {
  const states = [...xml.matchAll(/<State Name="([^"]+)" Code="([^"]+)">([\s\S]*?)<\/State>/g)]

  return states.map(([, rawProvince, provinceCode, content]) => {
    const province = displayProvince(rawProvince)
    let cities

    if (directControlledRegions.has(rawProvince)) {
      cities = [{
        name: displayCity(rawProvince),
        code: provinceCode.padStart(2, '0') + '0100',
        longitude: null,
        latitude: null
      }]
    } else {
      cities = [...content.matchAll(/<City Name="([^"]+)" Code="([^"]+)"/g)].map(
        ([, rawCity, cityCode]) => ({
          name: displayCity(rawCity),
          code: provinceCode.padStart(2, '0') + cityCode.padStart(2, '0') + '00',
          longitude: null,
          latitude: null
        })
      )
    }

    return { province, code: provinceCode.padStart(2, '0'), cities }
  }).filter(region => region.cities.length > 0)
}

const response = await fetch(SOURCE_URL)
if (!response.ok) {
  throw new Error('省市数据下载失败，状态码：' + response.status)
}

const xml = await response.text()
const regions = parseRegions(xml)
const outputPath = resolve(
  dirname(fileURLToPath(import.meta.url)),
  '../src/data/chinaRegions.js'
)

await mkdir(dirname(outputPath), { recursive: true })
const generatedText =
  '// 本文件由 scripts/generate-china-regions.mjs 自动生成，请勿手动逐项编辑。\n' +
  '// 数据来源：pluwen/china-cities-dataset（GPL-3.0）。\n' +
  '// 坐标暂为空时，后端会按城市名称查询天气。\n\n' +
  'export const chinaRegions = ' + JSON.stringify(regions, null, 2) + '\n'
await writeFile(outputPath, generatedText, 'utf8')

console.log('已生成 ' + regions.length + ' 个省级地区的数据：' + outputPath)
