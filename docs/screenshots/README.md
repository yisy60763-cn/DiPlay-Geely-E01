# UI screenshots

The PNG files in this directory are legacy reference captures from the pre-wired-only interface. They are **not** screenshots of the current `wired-only-e01` build and must not be used to describe its current UI.

The `wired-only-e01` product profile is USB-wired CarPlay only:

- The home connection card offers USB only.
- The quick panel does not display wireless hotspot, same-Wi-Fi, or wireless Bluetooth-maintenance controls.
- The connection settings page omits wireless setup and shows wired connection settings.
- Bluetooth remains available only for the head unit's native phone-call audio path; it is not a wireless CarPlay transport.

The current implementation is guarded by `wiredOnlyProfile` in `DiPlayActivity`, with component-level regression tests in `HeroStageCardTest` and `QuickWidgetsViewTest`.

Do not replace these files with fabricated or emulator-generated captures. A new screenshot set should be captured from a real build on the target E01 head unit when that validation is available.
