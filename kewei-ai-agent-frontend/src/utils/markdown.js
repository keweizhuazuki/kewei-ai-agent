import DOMPurify from 'dompurify'
import MarkdownIt from 'markdown-it'

const md = new MarkdownIt({
  html: false,
  linkify: true,
  breaks: true,
})

function preprocessManusText(input) {
  let text = typeof input === 'string' ? input : String(input || '')
  if (!text) return ''

  // 处理被转义的换行和引号，避免整段粘连成一行
  text = text
    .replace(/\\r\\n/g, '\n')
    .replace(/\\n/g, '\n')
    .replace(/\\t/g, '\t')
    .replace(/\\"/g, '"')

  // 一些工具结果会整体包一层引号，去掉外层双引号
  if (text.startsWith('"') && text.endsWith('"')) {
    text = text.slice(1, -1)
  }

  // Step 结构化展示
  text = text.replace(/Step\\s+(\\d+)\\s+result:/g, '\n\n### Step $1\n')

  // link 字段转为可点击 markdown 链接
  text = text.replace(/link:\s*(https?:\/\/\S+)/g, (m, url) => `link: [${url}](${url})`)

  return text
}

export function renderMarkdown(content) {
  const source = preprocessManusText(content)
  const rendered = md.render(source)
  return DOMPurify.sanitize(rendered)
}

export function renderAssistantContent(content) {
  const source = preprocessManusText(content)
  if (!/Step\s+\d+\s+result:/.test(source)) {
    return renderMarkdown(source)
  }

  const stepRegex = /Step\s+(\d+)\s+result:\s*([\s\S]*?)(?=Step\s+\d+\s+result:|$)/g
  let match
  const stepHtml = []
  const finalCandidates = []

  while ((match = stepRegex.exec(source)) !== null) {
    const stepNo = match[1]
    let body = (match[2] || '').trim()
    const finals = [...body.matchAll(/(?:Final\s*Answer|最终答案|最终结论|结论|总结)[:：]\s*([\s\S]*?)(?=(?:Final\s*Answer|最终答案|最终结论|结论|总结)[:：]|$)/gi)]
      .map((m) => (m[1] || '').trim())
      .filter(Boolean)
    if (finals.length) {
      finalCandidates.push(...finals)
    }

    // 过程区剔除 Final Answer 行，避免与正文混在一起
    body = body.replace(/(?:Final\s*Answer|最终答案|最终结论|结论|总结)[:：][\s\S]*$/gi, '').trim()
    if (!body) continue
    stepHtml.push(`<article class="trace-step"><h4>Step ${stepNo}</h4>${renderMarkdown(body)}</article>`)
  }

  const finalText = [...finalCandidates]
    .reverse()
    .find((text) => !/计划执行工具链/.test(text)) || ''

  const processHtml = stepHtml.length
    ? `<section class="manus-trace">${stepHtml.join('')}</section>`
    : ''

  const finalHtml = finalText
    ? `<section class="manus-final"><h4>最终回答</h4>${renderMarkdown(finalText)}</section>`
    : ''

  if (!processHtml && !finalHtml) {
    return renderMarkdown(source)
  }
  return DOMPurify.sanitize(`${processHtml}${finalHtml}`)
}
