# License Compliance & Intellectual Property Audit

## 1. Reference Projects License Review

| Project | License | Compliance Decision & Policy |
|---|---|---|
| **Snaptube** | Proprietary Commercial | No code or assets reused. Functional ideas (discovery, format selector bottom sheet) studied at a UX behavioral level only. |
| **VRKA Android** | GNU General Public License v3.0 (GPL-3.0) | **Zero Code Reuse / Clean-Room Implementation.** To prevent licensing encumbrance and preserve distribution flexibility, no code, classes, or assets from VRKA Android are copied or included in Nuvio. Architectural concepts (such as separating stream extraction from post-processing remuxing) are implemented independently using documented public Android and networking APIs. |
| **Advanced Download Manager (ADM)** | Proprietary Commercial | No code or assets reused. Multi-connection segmented range downloading implemented independently via standard HTTP/1.1 RFC 7233 and RFC 9110 specifications. |
| **Google Android Apps** | Proprietary / Material 3 | Visual style inspired by Google's public Material Design 3 guidelines and principles. All branding, logos, graphics, and icons are original or sourced from Apache 2.0 Material Symbols. |

---

## 2. Dependencies & Third-Party Library Licenses

All chosen dependencies for Nuvio adhere to permissive open-source licenses (Apache 2.0, MIT, or BSD-3-Clause):

| Dependency | License | Permissible Use |
|---|---|---|
| **AndroidX (Compose, Media3, Room, DataStore, Lifecycle)** | Apache 2.0 | Fully permissive; allows commercial and open-source distribution without copyleft restrictions. |
| **Kotlin Coroutines & Serialization** | Apache 2.0 | Fully permissive. |
| **Square OkHttp** | Apache 2.0 | Fully permissive. |
| **Google Hilt / Dagger** | Apache 2.0 | Fully permissive. |
| **Google Material Design 3** | Apache 2.0 | Fully permissive. |
| **JUnit 4 / 5, MockK** | Eclipse Public License 2.0 / Apache 2.0 | Test scope only. |

---

## 3. Strict License Guardrails

1. **No Copyleft Taint:** Under no circumstances should copyleft (GPL / AGPL) source files be brought into the application repository.
2. **Third-Party Notices:** An in-app open-source attribution screen (`Settings -> About -> Open Source Licenses`) displays proper copyright notices for all bundled open-source components.
