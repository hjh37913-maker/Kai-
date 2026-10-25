# KAI Launcher 2.0 — Design and Google Play roadmap

## Design direction
- Premium, restrained, dark navy/graphite base with cool blue and muted teal accents.
- Native app icons by default: no redundant circular badges around every installed app.
- Glass is reserved for a few meaningful surfaces (search and dock), not every control.
- Remove bright candy colors, heavy text shadows, oversized corner radii, and excessive outlines.
- Consistent spacing, readable labels, clear pressed/focused states, and accessible contrast.
- Keep customization optional: users who want glass/solid/monochrome can still select it.

## Implemented in this refresh
- Added a native icon presentation style and made it the default for new themes.
- Added native icon style to the Studio style selector and localized it in English, Ukrainian, and Russian.
- Reworked glass/solid icon treatments to rounded squares rather than circles.
- Reduced default icon size, glass opacity, border strength, and corner radius.
- Removed the heavy shadow from app labels and switched to a medium system typeface.
- Replaced candy-like accent swatches with a restrained neutral/blue/teal palette.
- Updated app display name to KAI Launcher.

## Still required before Google Play
1. Replace `<YOUR-EMAIL>` in `store/PRIVACY_POLICY.md` with a real monitored contact address.
2. Publish the privacy policy at a stable public HTTPS URL.
3. Confirm the actual data practices and complete the Play Console Data safety form accurately.
4. Build a signed release AAB with a private upload key; never commit that key or passwords.
5. Test first launch, default-home selection, app launch, app search, dock, all Studio sections, theme save/apply/undo, rotation, process death, Android 8–16, tablets, and accessibility font scaling.
6. Verify target SDK requirements and all Play Console declarations on the actual submission date.
7. Prepare truthful store screenshots from the running app, feature graphic, short/full descriptions, and support contact.
8. Run lint and inspect crash logs from closed testing; fix blockers before production rollout.

## Important limits
- The current environment had no configured Android SDK, so this refresh has not been verified with a full Gradle Android build or on-device runtime test.
- No design can guarantee a top Google Play ranking. Retention, reliability, reviews, accessibility, discoverability, and ongoing support matter as much as appearance.
