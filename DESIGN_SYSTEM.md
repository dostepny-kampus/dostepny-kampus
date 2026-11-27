# Dostępny Kampus — design system

## 1. Project overview

Native Android application in Jetpack Compose. The Mobile / Native profile applies.
The canonical screen requirements live in [wymagania-ekranow.md](docs/docs/ux/wymagania-ekranow.md).

## 2. Brand direction

Preserve the established “Karta miejsca” design: readable instructions, explicit action labels,
and accessibility signs whose shape and text carry meaning alongside color.

## 3. Color system

Use the generated Material theme from [tokeny.yml](docs/docs/ux/tokeny.yml), the sole source
of color values for both themes. No new colors are introduced by responsive layouts.

## 4. Typography system

Atkinson Hyperlegible Next; bodyLarge is 18 sp, weight 400, line height 28 sp.
Button labels use labelLarge and the shared emphasis weight (700). Respect system font scaling.

## 5. Layout system

`Modifier.readableWidth()` in `core:designsystem` centers a column of at most 560 dp when
available width reaches 600 dp. Apply it before fillMaxWidth/fillMaxSize. Narrow windows retain
their width. Text, fields, and actions share constraints at 100% and 200% font size.
Standard content padding remains 16 dp. The app shell applies the column to non-map routes;
map lists, search, and sheets apply it separately. Maps and floor plans retain the viewport.

AdaptiveScreen measures the header and actions against 25% of the current window height.
When they exceed the remaining budget, or at large font sizes, they scroll with the content.
Help resets scroll position when its step changes.

## 6. Component system

Reuse `core:designsystem`: BackHeader, PinnedActionBar, PinnedButton, and the shared card actions.
Screen code owns navigation and state; shared components own presentation and layout.

## 7. Card and section style

Preserve InfoPanel, GroupFrame, and ListItemCard with their generated spacing, shapes, and colors.
Do not introduce another card style for wide windows.

## 8. Icon system

Reuse NavigationIcons and the shared accessibility and barrier signs. Decorative icons have no
spoken description; meaningful icons retain a textual label.

## 9. Image and asset rules

Preserve user photos and map assets. Descriptions and photo controls belong to the reading
column; map geometry remains independent of the text width.

## 10. Animation and interaction system

Retain existing navigation and map movement. Responsive width changes introduce no animation.

## 11. Accessibility rules

Follow NFR-16 through NFR-22 and NFR-51/52. Touch targets are at least 48 dp; text wraps at
large sizes. Reading columns target at most 75 characters per typical bodyLarge line.
Never restrict a map viewport to achieve text measure.

## 12. Review rules

Preserve the project's palette and component vocabulary. Review narrow, wide, and 200% layouts.
The width change's weakest point is the map/content boundary: review it explicitly to avoid
shrinking map geometry. Shared route constraints and separate map panels keep that boundary clear.

## 13. Future screen instructions

New non-map routes inherit the shell's reading column. New text panels over a map must apply
the shared modifier themselves. Reuse existing tokens and components.

## 14. Update policy

Update this guide when shared layout behavior changes. Edit visual values in tokeny.yml and
screen behavior in wymagania-ekranow.md; do not create a second independent token source.
