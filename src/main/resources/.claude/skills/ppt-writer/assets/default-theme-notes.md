# Default Theme Notes for PPT Writer

This file defines the default presentation style for generated PowerPoint decks.

Use this file when the user does not provide:
- a brand guide
- a visual theme
- a deck template
- a specific presentation-writing style

This file is a fallback style reference. Apply it quietly.

## Design intent
The default deck should feel:
- modern
- clean
- professional
- readable
- presentation-friendly

It should work well for:
- project updates
- technical reviews
- internal reports
- demos
- strategy overviews
- mixed business + technical audiences

## Language behavior
Default to the user's language for slide content and speaker notes unless the user explicitly requests another language.

If the user writes in Chinese:
- slides may be written in Chinese
- speaker notes may be written in Chinese
- use natural presentation language rather than literal translation style

If the user writes in English:
- produce slides and speaker notes in English

Keep terminology consistent throughout the deck.

## Default audience assumption
If the audience is not specified, assume:
- mixed audience
- moderately technical
- not deeply specialized
- needs clarity more than jargon

## Default tone
Use this tone unless the user specifies another one:
- professional
- confident
- concise
- practical
- clear
- moderately polished

Avoid:
- exaggerated marketing language
- academic over-explaining on slides
- overly casual wording
- vague claims without support

## Default slide count
If the user does not specify slide count:
- default to 8 slides

Use 6–10 slides as the normal range for a standard presentation.

## Default deck structure
When no better structure is provided, prefer this flow:

1. Title
2. Agenda or overview
3. Background / problem / context
4. Proposed approach / solution
5. Key details / design / evidence
6. Results / value / expected impact
7. Risks / limitations / next steps
8. Closing / summary / Q&A

Adapt this structure if the topic is clearly:
- technical
- academic
- executive
- sales-oriented

## Title writing rules
Slide titles should:
- be short
- be specific
- express the main point of the slide

Prefer:
- System Architecture
- Key Findings
- Deployment Plan
- Risks and Mitigations

Avoid:
- long generic headings
- vague titles like “Introduction” when a more specific title is possible

## Bullet writing rules
Bullets should be:
- concise
- parallel in style
- easy to scan
- easy to present verbally

Default guidance:
- prefer 3–5 bullets per slide
- avoid more than 6 bullets unless the user explicitly wants dense slides
- keep bullets short when possible
- use concrete wording
- include important numbers or metrics when available

Preferred examples:
- Reduced processing time by 35%
- Supports local and cloud deployment
- Main bottleneck: retrieval latency
- Next step: validate with production data

Avoid:
- long paragraph bullets
- repeating the title in bullet form
- filler language
- generic bullets like “Good performance” or “Important improvement”

## Information density
Optimize for speaking, not document dumping.

Default density:
- one main message per slide
- visible whitespace
- no wall of text
- split crowded content across multiple slides

If the user asks for an executive deck:
- make bullets shorter
- emphasize decisions, impact, priorities, and risks

If the user asks for a technical deck:
- allow somewhat denser bullets
- include architecture, assumptions, tradeoffs, and implementation details
- still keep the slide readable

If the user asks for a sales or business deck:
- emphasize value, differentiation, outcomes, adoption, and next steps

## Speaker notes rules
Every slide should include speaker notes.

Speaker notes should:
- help a presenter talk naturally
- expand on the slide without repeating it word-for-word
- explain why the slide matters
- add transitions or emphasis where useful

Preferred style:
- 2–5 compact speaking paragraphs or speaking points
- enough detail to guide the presenter
- more detailed than slide bullets
- still concise and practical

Speaker notes may include:
- examples
- interpretation
- tradeoffs
- short transitions to the next slide

Avoid:
- empty notes
- notes that only duplicate bullets
- essay-like notes unless the user explicitly asks for detailed scripts

## Visual direction
Use a restrained and professional visual style.

### Color direction
Prefer:
- light background
- dark text
- one restrained accent color
- one neutral supporting color

Suggested feel:
- white or near-white background
- dark gray text
- muted blue, slate, or teal accent

Avoid:
- neon accents
- too many colors
- heavy gradients
- low contrast combinations

### Typography direction
Prefer:
- clean sans-serif style
- obvious title hierarchy
- readable body text

Style intent:
- title: prominent and strong
- bullets: medium-sized and readable
- overall look: modern and uncluttered

Avoid:
- decorative fonts
- excessive capitalization
- overly dense text blocks

## Layout guidance
Prefer simple layouts unless the content clearly requires something else.

Default preference:
- single-column layout for most slides
- limited bullet count
- consistent title placement
- enough spacing between points

If a comparison is needed:
- use a two-column comparison style in content structure

If a process is needed:
- present it as sequential bullets or numbered stages

If a chart is implied but no data is provided:
- do not invent fake precise values
- summarize the takeaway in bullets
- mention in speaker notes what visual could be added later

## Consistency rules
Across the deck:
- keep title style consistent
- keep bullet grammar consistent
- keep level of detail reasonably consistent
- avoid repeating the same message across multiple slides
- maintain one coherent tone throughout

## Quality check
Before finalizing content, check:
- Does each slide have a single clear purpose?
- Is the flow logical from one slide to the next?
- Are bullets concise and not repetitive?
- Do speaker notes add useful speaking guidance?
- Would the deck feel usable in a real meeting or presentation?

## Priority order
When instructions conflict, use:
1. direct user request
2. `SKILL.md`
3. this file
4. best judgment