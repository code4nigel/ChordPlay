# Rule: ChordPlay Design System & UI Architecture

When creating or modifying any UI in this repository, you MUST follow these guidelines:

## 1. Visual Aesthetics & Language (BitChord Heritage)
- **Frosted Glass Depth**: Use multi-layer translucent surfaces with `Haze` blur and subtle 1dp border strokes (`Color.White.copy(alpha = 0.08f)`). Avoid flat opaque backgrounds.
- **Dynamic Artwork Palette**: Tint background scrims and accents from the active song's artwork palette with strong contrast (WCAG AA).
- **Physics-Based Springs**: All motion and transitions MUST use spring physics (`Spring.DampingRatioMediumBouncy`, `Spring.StiffnessLow`), never linear animations.
- **Typography**: Adhere to the ChordPlay typography scale using **Poppins** for display/headings, **Parkinsans** for body/metadata, and support custom user fonts in the lyrics engine.

## 2. Settings Architecture & Customizability (Prism Heritage)
- **Modular Sub-pages**: Do NOT dump settings into a single 2,500-line monolithic sheet. Organize settings into clean, dedicated sub-screens:
  1. Appearance & Looks
  2. Player & Visuals
  3. Lyrics & Typography
  4. Audio & Playback
  5. Library & Downloads
  6. About & Community
- **Live Interactive Previews**: Any visual setting (Lyrics font/size/alignment, theme color, player layout) MUST feature a live preview card pinned at the top showing the change in real-time.
- **Component Editability**: Strive to give the user control over key UI elements (e.g., choice of player layouts, visualizers, and quick action buttons).

## 3. Specification Reference
For exact tokens, paddings, corner radii, and color values, always inspect:
- [docs/CHORDPLAY_DESIGN_SYSTEM.md](file:///d:/Projects/Chordplay/docs/CHORDPLAY_DESIGN_SYSTEM.md)
