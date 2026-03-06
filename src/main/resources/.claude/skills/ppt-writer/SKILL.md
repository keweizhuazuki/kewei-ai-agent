---
name: ppt-writer
description: Use this skill when the user asks to create a PowerPoint, PPT, slide deck, presentation, or downloadable slides. Use AskUserQuestionTool to clarify missing requirements, then generate a structured .pptx file with clear slide titles, concise bullets, and useful speaker notes.
---

# PPT Writer Skill

## Purpose
Create presentation-ready PowerPoint (`.pptx`) files on local disk using the available PPT writing tool.

The deck should be:
- clear
- logically structured
- easy to present
- useful as a real downloadable file
- equipped with speaker notes for each slide

## When to use this skill
Use this skill when the user asks for any of the following:
- PowerPoint
- PPT
- slide deck
- slides
- presentation
- presentation file
- downloadable presentation
- report slides
- meeting deck
- pitch deck

Also use this skill when the user clearly wants a presentation artifact, even if they do not explicitly say “PPT”.

## Required information
Before generating the file, make sure the following information is available:

Required:
- presentation topic
- target audience
- tone or style
- output filename or output path
- approximate number of slides

If the topic itself is unclear, clarify it before doing anything else.

## Clarification behavior
If any required information is missing, use `AskUserQuestionTool` to ask the user for the missing details.

When using `AskUserQuestionTool`:
- ask one concise question that combines all missing important fields when possible
- prefer a single grouped clarification over multiple back-and-forth questions
- explicitly ask for:
    - slide count
    - audience
    - tone/style
    - output filename or output path
      when they are missing
- do not call `PptWriterTool` until the required information is sufficiently clear
- if the user answers only part of the question, ask again only for the remaining critical fields

## Default behavior
If some non-critical details are still not specified after clarification, use these defaults:
- target audience: mixed business + technical audience
- tone: professional and clear
- slide count: 8
- output path: `tmp/ppt/<generated-filename>.pptx`

If the user gives only a filename, place it under:
- `tmp/ppt/<filename>.pptx`

## Theme and style behavior
If the user does not provide an explicit visual theme, brand guide, or slide-writing style, read:

`assets/default-theme-notes.md`

Use that file to guide:
- title style
- bullet style
- information density
- speaker notes style
- default deck flow
- language behavior

If the user provides direct style instructions, follow the user's instructions over the default theme notes.

## Language behavior
Default to the user's language for:
- slide titles
- bullet points
- speaker notes
- deck summary

If the user explicitly requests another language, follow that request.

## Workflow
When using this skill, follow this sequence:

1. Understand the presentation goal.
2. Check whether required inputs are missing.
3. If important details are missing, use `AskUserQuestionTool`.
4. After the needed details are available, read `assets/default-theme-notes.md` if no explicit theme is provided.
5. Draft a slide plan.
6. Generate the PPT file using `PptWriterTool`.
7. Return the output file path.
8. Provide a short slide-by-slide summary.

## Tool usage
Use `PptWriterTool` to generate the `.pptx` file.

Provide the tool with:
- `title`
- `slidesMarkdown`
  Format rules:
  - each slide starts with `## 页面标题`
  - each bullet starts with `- `
  - optional speaker notes line starts with `Notes: `
  - separate slides with a blank line
- `outputPath`

Example:

```text
## 送礼物
- 选择她真正喜欢的礼物
- 仪式感比价格更重要
Notes: 强调用心和观察力。

## 制造惊喜
- 提前准备小而具体的惊喜
- 避开她工作最忙的时候
Notes: 惊喜不要变成压力。
```

## Content rules
When preparing slide content:
- keep each slide focused on one main idea
- prefer 3–5 bullets per slide
- keep bullets concise and presentation-friendly
- avoid long paragraphs on slides
- include speaker notes for every slide
- use speaker notes to add context, transitions, or explanation
- split overloaded content into multiple slides instead of making one crowded slide

## File output rules
Default output directory:
- `tmp/ppt`

If the user gives a specific file path, use it.

If needed, create parent directories before writing the file.

## Response format
After generating the file:
- return the generated file path
- provide a short slide-by-slide summary
- keep each slide summary to 1–2 sentences

## Quality bar
Before finalizing, check that:
- the deck has a clear flow
- slide titles are specific
- bullets are concise and non-redundant
- speaker notes add value
- the output looks ready for real presentation use with light editing

## Priority order
When instructions conflict, follow this order:
1. the user's direct request
2. this `SKILL.md`
3. `assets/default-theme-notes.md`
4. best judgment
