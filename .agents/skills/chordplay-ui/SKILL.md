---
name: chordplay-ui
description: Design and review ChordPlay Compose surfaces that fit the app's artwork palettes, sheets, controls, and motion. Use for player, queue, settings, and other ChordPlay UI changes.
---

# ChordPlay UI

1. Read [ChordPlay's design system](../../../docs/CHORDPLAY_DESIGN_SYSTEM.md) and find the closest shipping surface. Match the active style, typography, palette, and shapes through the existing theme and player locals.
2. For Now Playing sheets, start with the shared player's AudioOutputSheet and VideoQualitySheet. Reuse their sheet shape, haze treatment, artwork tint, motion settings, and haptic hooks. Keep action labels meaningful and localize them through the surface's existing resource system.
3. Make expressive motion communicate state. Follow the actual playback position for a preview playhead, use springs for sheet and control movement, and support a static presentation under reduced motion. Use measured audio data for any marks that imply editable timing.
4. Put the preview and primary action first. Reveal precise controls when requested, and explain unavailable actions next to the control that needs the explanation.

Before handing off, check bright and dark artwork, large text, 48 dp targets, TalkBack labels and action order, and reduced motion. Verify actions and feedback in the real player, including loading, fallback, cancellation, and return to playback.
